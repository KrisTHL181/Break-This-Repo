package net.minecraft.world.level.chunk.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.thread.PriorityConsecutiveExecutor;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class IOWorker implements ChunkScanAccess, AutoCloseable {
   public static final Supplier<CompoundTag> STORE_EMPTY = () -> null;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AtomicBoolean shutdownRequested = new AtomicBoolean();
   private final PriorityConsecutiveExecutor consecutiveExecutor;
   private final RegionFileStorage storage;
   private final SequencedMap<ChunkPos, IOWorker.PendingStore> pendingWrites = new LinkedHashMap<>();
   private final Long2ObjectLinkedOpenHashMap<CompletableFuture<BitSet>> regionCacheForBlender = new Long2ObjectLinkedOpenHashMap();
   private static final int REGION_CACHE_SIZE = 1024;

   protected IOWorker(RegionStorageInfo p_335181_, Path p_196930_, boolean p_196931_) {
      this.storage = new RegionFileStorage(p_335181_, p_196930_, p_196931_);
      this.consecutiveExecutor = new PriorityConsecutiveExecutor(IOWorker.Priority.values().length, Util.ioPool(), "IOWorker-" + p_335181_.type());
   }

   public boolean isOldChunkAround(ChunkPos p_223472_, int p_223473_) {
      ChunkPos chunkpos = new ChunkPos(p_223472_.x - p_223473_, p_223472_.z - p_223473_);
      ChunkPos chunkpos1 = new ChunkPos(p_223472_.x + p_223473_, p_223472_.z + p_223473_);

      for (int i = chunkpos.getRegionX(); i <= chunkpos1.getRegionX(); i++) {
         for (int j = chunkpos.getRegionZ(); j <= chunkpos1.getRegionZ(); j++) {
            BitSet bitset = this.getOrCreateOldDataForRegion(i, j).join();
            if (!bitset.isEmpty()) {
               ChunkPos chunkpos2 = ChunkPos.minFromRegion(i, j);
               int k = Math.max(chunkpos.x - chunkpos2.x, 0);
               int l = Math.max(chunkpos.z - chunkpos2.z, 0);
               int i1 = Math.min(chunkpos1.x - chunkpos2.x, 31);
               int j1 = Math.min(chunkpos1.z - chunkpos2.z, 31);

               for (int k1 = k; k1 <= i1; k1++) {
                  for (int l1 = l; l1 <= j1; l1++) {
                     int i2 = l1 * 32 + k1;
                     if (bitset.get(i2)) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private CompletableFuture<BitSet> getOrCreateOldDataForRegion(int p_223464_, int p_223465_) {
      long i = ChunkPos.asLong(p_223464_, p_223465_);
      synchronized (this.regionCacheForBlender) {
         CompletableFuture<BitSet> completablefuture = (CompletableFuture<BitSet>)this.regionCacheForBlender.getAndMoveToFirst(i);
         if (completablefuture == null) {
            completablefuture = this.createOldDataForRegion(p_223464_, p_223465_);
            this.regionCacheForBlender.putAndMoveToFirst(i, completablefuture);
            if (this.regionCacheForBlender.size() > 1024) {
               this.regionCacheForBlender.removeLast();
            }
         }

         return completablefuture;
      }
   }

   private CompletableFuture<BitSet> createOldDataForRegion(int p_223490_, int p_223491_) {
      return CompletableFuture.supplyAsync(
         () -> {
            ChunkPos chunkpos = ChunkPos.minFromRegion(p_223490_, p_223491_);
            ChunkPos chunkpos1 = ChunkPos.maxFromRegion(p_223490_, p_223491_);
            BitSet bitset = new BitSet();
            ChunkPos.rangeClosed(chunkpos, chunkpos1)
               .forEach(
                  p_223480_ -> {
                     CollectFields collectfields = new CollectFields(
                        new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector(CompoundTag.TYPE, "blending_data")
                     );

                     try {
                        this.scanChunk(p_223480_, collectfields).join();
                     } catch (Exception exception) {
                        LOGGER.warn("Failed to scan chunk {}", p_223480_, exception);
                        return;
                     }

                     if (collectfields.getResult() instanceof CompoundTag compoundtag && this.isOldChunk(compoundtag)) {
                        int i = p_223480_.getRegionLocalZ() * 32 + p_223480_.getRegionLocalX();
                        bitset.set(i);
                     }
                  }
               );
            return bitset;
         },
         Util.backgroundExecutor()
      );
   }

   private boolean isOldChunk(CompoundTag p_223485_) {
      return p_223485_.getIntOr("DataVersion", 0) < 4295 ? true : p_223485_.getCompound("blending_data").isPresent();
   }

   public CompletableFuture<Void> store(ChunkPos p_63539_, CompoundTag p_63540_) {
      return this.store(p_63539_, () -> p_63540_);
   }

   public CompletableFuture<Void> store(ChunkPos p_363895_, Supplier<CompoundTag> p_367671_) {
      return this.<CompletableFuture<Void>>submitTask(() -> {
         CompoundTag compoundtag = p_367671_.get();
         IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.computeIfAbsent(p_363895_, p_223488_ -> new IOWorker.PendingStore(compoundtag));
         ioworker$pendingstore.data = compoundtag;
         return ioworker$pendingstore.result;
      }).thenCompose(Function.identity());
   }

   public CompletableFuture<Optional<CompoundTag>> loadAsync(ChunkPos p_156588_) {
      return this.submitThrowingTask(() -> {
         IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.get(p_156588_);
         if (ioworker$pendingstore != null) {
            return Optional.ofNullable(ioworker$pendingstore.copyData());
         }

         try {
            CompoundTag compoundtag = this.storage.read(p_156588_);
            return Optional.ofNullable(compoundtag);
         } catch (Exception exception) {
            LOGGER.warn("Failed to read chunk {}", p_156588_, exception);
            throw exception;
         }
      });
   }

   public CompletableFuture<Void> synchronize(boolean p_182499_) {
      CompletableFuture<Void> completablefuture = this.<CompletableFuture<Void>>submitTask(
            () -> CompletableFuture.allOf(this.pendingWrites.values().stream().map(p_223475_ -> p_223475_.result).toArray(CompletableFuture[]::new))
         )
         .thenCompose(Function.identity());
      return p_182499_ ? completablefuture.thenCompose(p_360563_ -> this.submitThrowingTask(() -> {
         try {
            this.storage.flush();
            return null;
         } catch (Exception exception) {
            LOGGER.warn("Failed to synchronize chunks", exception);
            throw exception;
         }
      })) : completablefuture.thenCompose(p_223477_ -> this.submitTask(() -> null));
   }

   @Override
   public CompletableFuture<Void> scanChunk(ChunkPos p_196939_, StreamTagVisitor p_196940_) {
      return this.submitThrowingTask(() -> {
         try {
            IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.get(p_196939_);
            if (ioworker$pendingstore != null) {
               if (ioworker$pendingstore.data != null) {
                  ioworker$pendingstore.data.acceptAsRoot(p_196940_);
               }
            } else {
               this.storage.scanChunk(p_196939_, p_196940_);
            }

            return null;
         } catch (Exception exception) {
            LOGGER.warn("Failed to bulk scan chunk {}", p_196939_, exception);
            throw exception;
         }
      });
   }

   private <T> CompletableFuture<T> submitThrowingTask(IOWorker.ThrowingSupplier<T> p_363182_) {
      return this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.FOREGROUND.ordinal(), p_360568_ -> {
         if (!this.shutdownRequested.get()) {
            try {
               p_360568_.complete(p_363182_.get());
            } catch (Exception exception) {
               p_360568_.completeExceptionally(exception);
            }
         }

         this.tellStorePending();
      });
   }

   private <T> CompletableFuture<T> submitTask(Supplier<T> p_63546_) {
      return this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.FOREGROUND.ordinal(), p_360561_ -> {
         if (!this.shutdownRequested.get()) {
            p_360561_.complete(p_63546_.get());
         }

         this.tellStorePending();
      });
   }

   private void storePendingChunk() {
      Entry<ChunkPos, IOWorker.PendingStore> entry = this.pendingWrites.pollFirstEntry();
      if (entry != null) {
         this.runStore(entry.getKey(), entry.getValue());
         this.tellStorePending();
      }
   }

   private void tellStorePending() {
      this.consecutiveExecutor.schedule(new StrictQueue.RunnableWithPriority(IOWorker.Priority.BACKGROUND.ordinal(), this::storePendingChunk));
   }

   private void runStore(ChunkPos p_63536_, IOWorker.PendingStore p_63537_) {
      try {
         this.storage.write(p_63536_, p_63537_.data);
         p_63537_.result.complete(null);
      } catch (Exception exception) {
         LOGGER.error("Failed to store chunk {}", p_63536_, exception);
         p_63537_.result.completeExceptionally(exception);
      }
   }

   @Override
   public void close() throws IOException {
      if (this.shutdownRequested.compareAndSet(false, true)) {
         this.waitForShutdown();
         this.consecutiveExecutor.close();

         try {
            this.storage.close();
         } catch (Exception exception) {
            LOGGER.error("Failed to close storage", exception);
         }
      }
   }

   private void waitForShutdown() {
      this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.SHUTDOWN.ordinal(), p_360562_ -> p_360562_.complete(Unit.INSTANCE)).join();
   }

   public RegionStorageInfo storageInfo() {
      return this.storage.info();
   }

   static class PendingStore {
      @Nullable CompoundTag data;
      final CompletableFuture<Void> result = new CompletableFuture<>();

      public PendingStore(@Nullable CompoundTag p_63568_) {
         this.data = p_63568_;
      }

      @Nullable CompoundTag copyData() {
         CompoundTag compoundtag = this.data;
         return compoundtag == null ? null : compoundtag.copy();
      }
   }

   enum Priority {
      FOREGROUND,
      BACKGROUND,
      SHUTDOWN;
   }

   @FunctionalInterface
   interface ThrowingSupplier<T> {
      @Nullable T get() throws Exception;
   }
}
