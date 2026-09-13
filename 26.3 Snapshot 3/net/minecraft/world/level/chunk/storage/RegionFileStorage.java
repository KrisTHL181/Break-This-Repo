package net.minecraft.world.level.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public final class RegionFileStorage implements AutoCloseable {
   public static final String ANVIL_EXTENSION = ".mca";
   private static final int MAX_CACHE_SIZE = 256;
   private final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap();
   private final RegionStorageInfo info;
   private final Path folder;
   private final boolean sync;

   public RegionFileStorage(final RegionStorageInfo info, final Path folder, final boolean sync) {
      this.folder = folder;
      this.sync = sync;
      this.info = info;
   }

   private RegionFile getRegionFile(final ChunkPos pos) throws IOException {
      long key = ChunkPos.pack(pos.getRegionX(), pos.getRegionZ());
      RegionFile region = (RegionFile)this.regionCache.getAndMoveToFirst(key);
      if (region != null) {
         return region;
      }

      if (this.regionCache.size() >= 256) {
         ((RegionFile)this.regionCache.removeLast()).close();
      }

      FileUtil.createDirectoriesSafe(this.folder);
      Path file = this.folder.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
      RegionFile newRegion = new RegionFile(this.info, file, this.folder, this.sync);
      this.regionCache.putAndMoveToFirst(key, newRegion);
      return newRegion;
   }

   public @Nullable CompoundTag read(final ChunkPos pos) throws IOException {
      RegionFile region = this.getRegionFile(pos);

      try (DataInputStream regionChunkInputStream = region.getChunkDataInputStream(pos)) {
         return regionChunkInputStream == null ? null : NbtIo.read(regionChunkInputStream);
      }
   }

   public void scanChunk(final ChunkPos pos, final StreamTagVisitor scanner) throws IOException {
      RegionFile region = this.getRegionFile(pos);

      try (DataInputStream regionChunkInputStream = region.getChunkDataInputStream(pos)) {
         if (regionChunkInputStream != null) {
            NbtIo.parse(regionChunkInputStream, scanner, NbtAccounter.unlimitedHeap());
         }
      }
   }

   public void write(final ChunkPos pos, final @Nullable CompoundTag value) throws IOException {
      if (!SharedConstants.DEBUG_DONT_SAVE_WORLD) {
         RegionFile region = this.getRegionFile(pos);
         if (value == null) {
            region.clear(pos);
         } else {
            try (DataOutputStream output = region.getChunkDataOutputStream(pos)) {
               NbtIo.write(value, output);
            }
         }
      }
   }

   @Override
   public void close() throws IOException {
      ExceptionCollector<IOException> exception = new ExceptionCollector<>();
      ObjectIterator var2 = this.regionCache.values().iterator();

      while (var2.hasNext()) {
         RegionFile regionFile = (RegionFile)var2.next();

         try {
            regionFile.close();
         } catch (IOException e) {
            exception.add(e);
         }
      }

      exception.throwIfPresent();
   }

   public void flush() throws IOException {
      ObjectIterator var1 = this.regionCache.values().iterator();

      while (var1.hasNext()) {
         RegionFile regionFile = (RegionFile)var1.next();
         regionFile.flush();
      }
   }

   public RegionStorageInfo info() {
      return this.info;
   }
}
