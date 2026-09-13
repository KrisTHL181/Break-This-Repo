package net.minecraft.world.level.storage;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SavedDataStorage implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<SavedDataType<?>, Optional<SavedData>> cache = new HashMap<>();
   private final DataFixer fixerUpper;
   private final HolderLookup.Provider registries;
   private final Path dataFolder;
   private CompletableFuture<?> pendingWriteFuture = CompletableFuture.completedFuture(null);
   private boolean closed;

   public SavedDataStorage(final Path dataFolder, final DataFixer fixerUpper, final HolderLookup.Provider registries) {
      this.fixerUpper = fixerUpper;
      this.dataFolder = dataFolder;
      this.registries = registries;
   }

   private Path getDataFile(final Identifier id) {
      Path path = id.withSuffix(".dat").resolveAgainst(this.dataFolder);
      if (!path.toAbsolutePath().startsWith(this.dataFolder.toAbsolutePath())) {
         throw new IllegalArgumentException("SavedDataStorage attempted file access outside of data directory: {}" + path);
      } else {
         return path;
      }
   }

   public <T extends SavedData> T computeIfAbsent(final SavedDataType<T> type) {
      T data = this.get(type);
      if (data != null) {
         return data;
      }

      T newData = (T)type.constructor().get();
      this.set(type, newData);
      return newData;
   }

   public <T extends SavedData> @Nullable T get(final SavedDataType<T> type) {
      Optional<SavedData> data = this.cache.get(type);
      if (data == null) {
         data = Optional.ofNullable(this.readSavedData(type));
         this.cache.put(type, data);
      }

      return (T)data.orElse(null);
   }

   private <T extends SavedData> @Nullable T readSavedData(final SavedDataType<T> type) {
      try {
         Path file = this.getDataFile(type.id());
         if (Files.exists(file)) {
            CompoundTag tag = this.readTagFromDisk(file, type.dataFixType(), SharedConstants.getCurrentVersion().dataVersion().version());
            RegistryOps<Tag> ops = this.registries.createSerializationContext(NbtOps.INSTANCE);
            return (T)type.codec()
               .parse(ops, tag.get("data"))
               .resultOrPartial(error -> LOGGER.error("Failed to parse saved data for '{}': {}", type, error))
               .orElse(null);
         }
      } catch (Exception e) {
         LOGGER.error("Error loading saved data: {}", type, e);
      }

      return null;
   }

   public <T extends SavedData> void set(final SavedDataType<T> type, final T data) {
      this.cache.put(type, Optional.of(data));
      data.setDirty();
   }

   public CompoundTag readTagFromDisk(final Path dataFile, final DataFixTypes type, final int newVersion) throws IOException {
      try (
         InputStream in = Files.newInputStream(dataFile);
         PushbackInputStream inputStream = new PushbackInputStream(new FastBufferedInputStream(in), 2);
      ) {
         CompoundTag tag;
         if (this.isGzip(inputStream)) {
            tag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
         } else {
            try (DataInputStream dis = new DataInputStream(inputStream)) {
               tag = NbtIo.read(dis);
            }
         }

         int version = NbtUtils.getDataVersion(tag, 1343);
         return type.update(this.fixerUpper, tag, version, newVersion);
      }
   }

   private boolean isGzip(final PushbackInputStream inputStream) throws IOException {
      byte[] header = new byte[2];
      boolean gzip = false;
      int read = inputStream.read(header, 0, 2);
      if (read == 2) {
         int fullHeader = (header[1] & 255) << 8 | header[0] & 255;
         if (fullHeader == 35615) {
            gzip = true;
         }
      }

      if (read != 0) {
         inputStream.unread(header, 0, read);
      }

      return gzip;
   }

   public CompletableFuture<?> scheduleSave() {
      if (this.closed) {
         throw new IllegalStateException("Trying to schedule save when SavedDataStorage is already closed");
      }

      Map<SavedDataType<?>, CompoundTag> tagsToSave = this.collectDirtyTagsToSave();
      if (tagsToSave.isEmpty()) {
         return CompletableFuture.completedFuture(null);
      }

      int threads = Util.maxAllowedExecutorThreads();
      int taskCount = tagsToSave.size();
      if (taskCount > threads) {
         this.pendingWriteFuture = this.pendingWriteFuture.thenCompose(ignored -> {
            List<CompletableFuture<?>> tasks = new ArrayList<>(threads);
            int bucketSize = Mth.positiveCeilDiv(taskCount, threads);

            for (List<Entry<SavedDataType<?>, CompoundTag>> entries : Iterables.partition(tagsToSave.entrySet(), bucketSize)) {
               tasks.add(CompletableFuture.runAsync(() -> {
                  for (Entry<SavedDataType<?>, CompoundTag> entry : entries) {
                     this.tryWrite(entry.getKey(), entry.getValue());
                  }
               }, Util.ioPool()));
            }

            return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
         });
      } else {
         this.pendingWriteFuture = this.pendingWriteFuture
            .thenCompose(
               ignored -> CompletableFuture.allOf(
                  tagsToSave.entrySet()
                     .stream()
                     .map(entry -> CompletableFuture.runAsync(() -> this.tryWrite(entry.getKey(), entry.getValue()), Util.ioPool()))
                     .toArray(CompletableFuture[]::new)
               )
            );
      }

      return this.pendingWriteFuture;
   }

   private Map<SavedDataType<?>, CompoundTag> collectDirtyTagsToSave() {
      Map<SavedDataType<?>, CompoundTag> tagsToSave = new Object2ObjectArrayMap();
      RegistryOps<Tag> ops = this.registries.createSerializationContext(NbtOps.INSTANCE);
      this.cache.forEach((type, optional) -> optional.filter(SavedData::isDirty).ifPresent(data -> {
         tagsToSave.put(type, this.encodeUnchecked(type, data, ops));
         data.setDirty(false);
      }));
      return tagsToSave;
   }

   private <T extends SavedData> CompoundTag encodeUnchecked(final SavedDataType<T> type, final SavedData data, final RegistryOps<Tag> ops) {
      Codec<T> codec = type.codec();
      CompoundTag tag = new CompoundTag();
      tag.put("data", (Tag)codec.encodeStart(ops, data).getOrThrow());
      NbtUtils.addCurrentDataVersion(tag);
      return tag;
   }

   private void tryWrite(final SavedDataType<?> type, final CompoundTag tag) {
      Path path = this.getDataFile(type.id());

      try {
         FileUtil.createDirectoriesSafe(path.getParent());
         NbtIo.writeCompressed(tag, path);
      } catch (IOException e) {
         LOGGER.error("Could not save data to {}", path.getFileName(), e);
      }
   }

   public void saveAndJoin() {
      this.scheduleSave().join();
   }

   @Override
   public void close() {
      if (this.closed) {
         throw new IllegalStateException("Trying to close SavedDataStorage when it is already closed");
      }

      this.saveAndJoin();
      this.closed = true;
   }
}
