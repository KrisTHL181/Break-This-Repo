package net.minecraft.client.color.block;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class BlockTintCache {
   private static final int MAX_CACHE_ENTRIES = 256;
   private final ThreadLocal<BlockTintCache.LatestCacheInfo> latestChunkOnThread = ThreadLocal.withInitial(BlockTintCache.LatestCacheInfo::new);
   private final Long2ObjectLinkedOpenHashMap<BlockTintCache.CacheData> cache = new Long2ObjectLinkedOpenHashMap(256, 0.25F);
   private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
   private final ToIntFunction<BlockPos> source;

   public BlockTintCache(final ToIntFunction<BlockPos> source) {
      this.source = source;
   }

   public int getColor(final BlockPos pos) {
      int chunkX = SectionPos.blockToSectionCoord(pos.getX());
      int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
      BlockTintCache.LatestCacheInfo chunkInfo = this.latestChunkOnThread.get();
      if (chunkInfo.x != chunkX || chunkInfo.z != chunkZ || chunkInfo.cache == null || chunkInfo.cache.isInvalidated()) {
         chunkInfo.x = chunkX;
         chunkInfo.z = chunkZ;
         chunkInfo.cache = this.findOrCreateChunkCache(chunkX, chunkZ);
      }

      int[] layer = chunkInfo.cache.getLayer(pos.getY());
      int x = pos.getX() & 15;
      int z = pos.getZ() & 15;
      int index = z << 4 | x;
      int cached = layer[index];
      if (cached != -1) {
         return cached;
      }

      int calculated = this.source.applyAsInt(pos);
      layer[index] = calculated;
      return calculated;
   }

   public void invalidateForChunk(final int chunkX, final int chunkZ) {
      try {
         this.lock.writeLock().lock();

         for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
               long key = ChunkPos.pack(chunkX + offsetX, chunkZ + offsetZ);
               BlockTintCache.CacheData removed = (BlockTintCache.CacheData)this.cache.remove(key);
               if (removed != null) {
                  removed.invalidate();
               }
            }
         }
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   public void invalidateAll() {
      try {
         this.lock.writeLock().lock();
         this.cache.values().forEach(BlockTintCache.CacheData::invalidate);
         this.cache.clear();
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   private BlockTintCache.CacheData findOrCreateChunkCache(final int x, final int z) {
      long key = ChunkPos.pack(x, z);
      this.lock.readLock().lock();

      try {
         BlockTintCache.CacheData existing = (BlockTintCache.CacheData)this.cache.get(key);
         if (existing != null) {
            return existing;
         }
      } finally {
         this.lock.readLock().unlock();
      }

      this.lock.writeLock().lock();

      try {
         BlockTintCache.CacheData existingNow = (BlockTintCache.CacheData)this.cache.get(key);
         if (existingNow != null) {
            return existingNow;
         }

         BlockTintCache.CacheData newCache = new BlockTintCache.CacheData();
         if (this.cache.size() >= 256) {
            BlockTintCache.CacheData cacheData = (BlockTintCache.CacheData)this.cache.removeFirst();
            if (cacheData != null) {
               cacheData.invalidate();
            }
         }

         this.cache.put(key, newCache);
         return newCache;
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   private static class CacheData {
      private final Int2ObjectArrayMap<int[]> cache = new Int2ObjectArrayMap(16);
      private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
      private static final int BLOCKS_PER_LAYER = Mth.square(16);
      private volatile boolean invalidated;

      public int[] getLayer(final int y) {
         this.lock.readLock().lock();

         try {
            int[] existing = (int[])this.cache.get(y);
            if (existing != null) {
               return existing;
            }
         } finally {
            this.lock.readLock().unlock();
         }

         this.lock.writeLock().lock();

         try {
            return (int[])this.cache.computeIfAbsent(y, n -> this.allocateLayer());
         } finally {
            this.lock.writeLock().unlock();
         }
      }

      private int[] allocateLayer() {
         int[] newCache = new int[BLOCKS_PER_LAYER];
         Arrays.fill(newCache, -1);
         return newCache;
      }

      public boolean isInvalidated() {
         return this.invalidated;
      }

      public void invalidate() {
         this.invalidated = true;
      }
   }

   private static class LatestCacheInfo {
      private int x = Integer.MIN_VALUE;
      private int z = Integer.MIN_VALUE;
      private BlockTintCache.@Nullable CacheData cache;
   }
}
