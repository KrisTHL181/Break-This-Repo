package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SectionStorage<R, P> implements AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String SECTIONS_TAG = "Sections";
   private final SimpleRegionStorage simpleRegionStorage;
   private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectOpenHashMap();
   private final LongLinkedOpenHashSet dirtyChunks = new LongLinkedOpenHashSet();
   private final Codec<P> codec;
   private final Function<R, P> packer;
   private final BiFunction<P, Runnable, R> unpacker;
   private final Function<Runnable, R> factory;
   private final RegistryAccess registryAccess;
   private final ChunkIOErrorReporter errorReporter;
   protected final LevelHeightAccessor levelHeightAccessor;
   private final LongSet loadedChunks = new LongOpenHashSet();
   private final Long2ObjectMap<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>> pendingLoads = new Long2ObjectOpenHashMap();
   private final Object loadLock = new Object();

   public SectionStorage(
      SimpleRegionStorage p_335141_,
      Codec<P> p_368876_,
      Function<R, P> p_223510_,
      BiFunction<P, Runnable, R> p_365815_,
      Function<Runnable, R> p_223511_,
      RegistryAccess p_223515_,
      ChunkIOErrorReporter p_345160_,
      LevelHeightAccessor p_223516_
   ) {
      this.simpleRegionStorage = p_335141_;
      this.codec = p_368876_;
      this.packer = p_223510_;
      this.unpacker = p_365815_;
      this.factory = p_223511_;
      this.registryAccess = p_223515_;
      this.errorReporter = p_345160_;
      this.levelHeightAccessor = p_223516_;
   }

   protected void tick(BooleanSupplier p_63812_) {
      LongIterator longiterator = this.dirtyChunks.iterator();

      while (longiterator.hasNext() && p_63812_.getAsBoolean()) {
         ChunkPos chunkpos = new ChunkPos(longiterator.nextLong());
         longiterator.remove();
         this.writeChunk(chunkpos);
      }

      this.unpackPendingLoads();
   }

   private void unpackPendingLoads() {
      synchronized (this.loadLock) {
         Iterator<Entry<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>>> iterator = Long2ObjectMaps.fastIterator(this.pendingLoads);

         while (iterator.hasNext()) {
            Entry<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>> entry = iterator.next();
            Optional<SectionStorage.PackedChunk<P>> optional = (Optional<SectionStorage.PackedChunk<P>>)((CompletableFuture)entry.getValue()).getNow(null);
            if (optional != null) {
               long i = entry.getLongKey();
               this.unpackChunk(new ChunkPos(i), optional.orElse(null));
               iterator.remove();
               this.loadedChunks.add(i);
            }
         }
      }
   }

   public void flushAll() {
      if (!this.dirtyChunks.isEmpty()) {
         this.dirtyChunks.forEach(p_360574_ -> this.writeChunk(new ChunkPos(p_360574_)));
         this.dirtyChunks.clear();
      }
   }

   public boolean hasWork() {
      return !this.dirtyChunks.isEmpty();
   }

   protected @Nullable Optional<R> get(long p_63819_) {
      return (Optional<R>)this.storage.get(p_63819_);
   }

   protected Optional<R> getOrLoad(long p_63824_) {
      if (this.outsideStoredRange(p_63824_)) {
         return Optional.empty();
      } else {
         Optional<R> optional = this.get(p_63824_);
         if (optional != null) {
            return optional;
         } else {
            this.unpackChunk(SectionPos.of(p_63824_).chunk());
            optional = this.get(p_63824_);
            if (optional == null) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
            } else {
               return optional;
            }
         }
      }
   }

   protected boolean outsideStoredRange(long p_156631_) {
      int i = SectionPos.sectionToBlockCoord(SectionPos.y(p_156631_));
      return this.levelHeightAccessor.isOutsideBuildHeight(i);
   }

   protected R getOrCreate(long p_63828_) {
      if (this.outsideStoredRange(p_63828_)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("sectionPos out of bounds"));
      }

      Optional<R> optional = this.getOrLoad(p_63828_);
      if (optional.isPresent()) {
         return optional.get();
      }

      R r = this.factory.apply(() -> this.setDirty(p_63828_));
      this.storage.put(p_63828_, Optional.of(r));
      return r;
   }

   public CompletableFuture<?> prefetch(ChunkPos p_366341_) {
      synchronized (this.loadLock) {
         long i = p_366341_.toLong();
         return this.loadedChunks.contains(i)
            ? CompletableFuture.completedFuture(null)
            : (CompletableFuture)this.pendingLoads.computeIfAbsent(i, p_360582_ -> this.tryRead(p_366341_));
      }
   }

   private void unpackChunk(ChunkPos p_369465_) {
      long i = p_369465_.toLong();
      CompletableFuture<Optional<SectionStorage.PackedChunk<P>>> completablefuture;
      synchronized (this.loadLock) {
         if (!this.loadedChunks.add(i)) {
            return;
         }

         completablefuture = (CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>)this.pendingLoads
            .computeIfAbsent(i, p_360576_ -> this.tryRead(p_369465_));
      }

      this.unpackChunk(p_369465_, completablefuture.join().orElse(null));
      synchronized (this.loadLock) {
         this.pendingLoads.remove(i);
      }
   }

   private CompletableFuture<Optional<SectionStorage.PackedChunk<P>>> tryRead(ChunkPos p_223533_) {
      RegistryOps<Tag> registryops = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
      return this.simpleRegionStorage
         .read(p_223533_)
         .thenApplyAsync(
            p_360573_ -> p_360573_.map(
               p_360578_ -> SectionStorage.PackedChunk.parse(this.codec, registryops, p_360578_, this.simpleRegionStorage, this.levelHeightAccessor)
            ),
            Util.backgroundExecutor().forName("parseSection")
         )
         .exceptionally(p_375353_ -> {
            if (p_375353_ instanceof CompletionException) {
               p_375353_ = p_375353_.getCause();
            }

            if (p_375353_ instanceof IOException ioexception) {
               LOGGER.error("Error reading chunk {} data from disk", p_223533_, ioexception);
               this.errorReporter.reportChunkLoadFailure(ioexception, this.simpleRegionStorage.storageInfo(), p_223533_);
               return Optional.empty();
            } else {
               throw new CompletionException(p_375353_);
            }
         });
   }

   private void unpackChunk(ChunkPos p_362977_, SectionStorage.@Nullable PackedChunk<P> p_365518_) {
      if (p_365518_ == null) {
         for (int i = this.levelHeightAccessor.getMinSectionY(); i <= this.levelHeightAccessor.getMaxSectionY(); i++) {
            this.storage.put(getKey(p_362977_, i), Optional.empty());
         }
      } else {
         boolean flag = p_365518_.versionChanged();

         for (int j = this.levelHeightAccessor.getMinSectionY(); j <= this.levelHeightAccessor.getMaxSectionY(); j++) {
            long k = getKey(p_362977_, j);
            Optional<R> optional = Optional.ofNullable(p_365518_.sectionsByY.get(j))
               .map(p_360580_ -> this.unpacker.apply((P)p_360580_, () -> this.setDirty(k)));
            this.storage.put(k, optional);
            optional.ifPresent(p_223523_ -> {
               this.onSectionLoad(k);
               if (flag) {
                  this.setDirty(k);
               }
            });
         }
      }
   }

   private void writeChunk(ChunkPos p_364337_) {
      RegistryOps<Tag> registryops = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
      Dynamic<Tag> dynamic = this.writeChunk(p_364337_, registryops);
      Tag tag = (Tag)dynamic.getValue();
      if (tag instanceof CompoundTag compoundtag) {
         this.simpleRegionStorage.write(p_364337_, compoundtag).exceptionally(p_341891_ -> {
            this.errorReporter.reportChunkSaveFailure(p_341891_, this.simpleRegionStorage.storageInfo(), p_364337_);
            return null;
         });
      } else {
         LOGGER.error("Expected compound tag, got {}", tag);
      }
   }

   private <T> Dynamic<T> writeChunk(ChunkPos p_361397_, DynamicOps<T> p_361006_) {
      Map<T, T> map = Maps.newHashMap();

      for (int i = this.levelHeightAccessor.getMinSectionY(); i <= this.levelHeightAccessor.getMaxSectionY(); i++) {
         long j = getKey(p_361397_, i);
         Optional<R> optional = (Optional<R>)this.storage.get(j);
         if (optional != null && !optional.isEmpty()) {
            DataResult<T> dataresult = this.codec.encodeStart(p_361006_, this.packer.apply(optional.get()));
            String s = Integer.toString(i);
            dataresult.resultOrPartial(LOGGER::error).ifPresent(p_223531_ -> map.put((T)p_361006_.createString(s), (T)p_223531_));
         }
      }

      return new Dynamic(
         p_361006_,
         p_361006_.createMap(
            ImmutableMap.of(
               p_361006_.createString("Sections"),
               p_361006_.createMap(map),
               p_361006_.createString("DataVersion"),
               p_361006_.createInt(SharedConstants.getCurrentVersion().dataVersion().version())
            )
         )
      );
   }

   private static long getKey(ChunkPos p_156628_, int p_156629_) {
      return SectionPos.asLong(p_156628_.x, p_156629_, p_156628_.z);
   }

   protected void onSectionLoad(long p_63813_) {
   }

   protected void setDirty(long p_63788_) {
      Optional<R> optional = (Optional<R>)this.storage.get(p_63788_);
      if (optional != null && !optional.isEmpty()) {
         this.dirtyChunks.add(ChunkPos.asLong(SectionPos.x(p_63788_), SectionPos.z(p_63788_)));
      } else {
         LOGGER.warn("No data for position: {}", SectionPos.of(p_63788_));
      }
   }

   public void flush(ChunkPos p_63797_) {
      if (this.dirtyChunks.remove(p_63797_.toLong())) {
         this.writeChunk(p_63797_);
      }
   }

   @Override
   public void close() throws IOException {
      this.simpleRegionStorage.close();
   }

   record PackedChunk<T>(Int2ObjectMap<T> sectionsByY, boolean versionChanged) {
      public static <T> SectionStorage.PackedChunk<T> parse(
         Codec<T> p_363389_, DynamicOps<Tag> p_365665_, Tag p_366092_, SimpleRegionStorage p_362884_, LevelHeightAccessor p_366328_
      ) {
         Dynamic<Tag> dynamic = new Dynamic(p_365665_, p_366092_);
         Dynamic<Tag> dynamic1 = p_362884_.upgradeChunkTag(dynamic, 1945);
         boolean flag = dynamic != dynamic1;
         OptionalDynamic<Tag> optionaldynamic = dynamic1.get("Sections");
         Int2ObjectMap<T> int2objectmap = new Int2ObjectOpenHashMap();

         for (int i = p_366328_.getMinSectionY(); i <= p_366328_.getMaxSectionY(); i++) {
            Optional<T> optional = optionaldynamic.get(Integer.toString(i))
               .result()
               .flatMap(p_368164_ -> p_363389_.parse(p_368164_).resultOrPartial(SectionStorage.LOGGER::error));
            if (optional.isPresent()) {
               int2objectmap.put(i, optional.get());
            }
         }

         return new SectionStorage.PackedChunk<>(int2objectmap, flag);
      }
   }
}
