package com.mojang.blaze3d.vertex;

import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TlsfAllocator {
   private static final int SECOND_LEVEL_BIN_LOG2 = 3;
   private static final int SECOND_LEVEL_BIN_COUNT = 8;
   private static final int FIRST_LEVEL_INDEX_SHIFT = 8;
   private static final int FIRST_LEVEL_BIN_COUNT = 32;
   private static final int SMALL_BLOCK_SIZE = 256;
   private static final long MAX_ALLOCATION_SIZE = 549755813888L;
   private static final int ALIGN_SIZE = 32;
   private int firstLevelBitmap = 0;
   private final int[] secondLevelBitmap = new int[32];
   private final TlsfAllocator.@Nullable Block[] freeLists = new TlsfAllocator.Block[256];
   private final long totalMemorySize;
   private static final Logger LOGGER = LogUtils.getLogger();

   public TlsfAllocator(final TlsfAllocator.Heap heap) {
      long alignedHeapSize = alignDown(heap.size, 32L);
      TlsfAllocator.Block freeBlock = new TlsfAllocator.Block(alignedHeapSize, heap, 0L, null, null, null, null);
      freeBlock.setFree();
      this.insertFreeBlock(freeBlock);
      long remainingHeapSize = heap.size - alignedHeapSize;
      TlsfAllocator.Block sentinelBlock = new TlsfAllocator.Block(remainingHeapSize, heap, alignedHeapSize, null, null, null, freeBlock);
      sentinelBlock.setUsed();
      freeBlock.nextPhysicalBlock = sentinelBlock;
      this.totalMemorySize = alignedHeapSize;
   }

   private TlsfAllocator.@Nullable Block getBlockFromFreeList(final int firstLevelIndex, final int secondLevelIndex) {
      return this.freeLists[firstLevelIndex * 8 + secondLevelIndex];
   }

   private void setBlockFreeList(final int firstLevelIndex, final int secondLevelIndex, final TlsfAllocator.@Nullable Block block) {
      this.freeLists[firstLevelIndex * 8 + secondLevelIndex] = block;
   }

   private static long alignUp(final long x, final long align) {
      return x + (align - 1L) & ~(align - 1L);
   }

   private static long alignDown(final long x, final long align) {
      return x - (x & align - 1L);
   }

   private static int findLastSignificantBit(final long x) {
      return 63 - Long.numberOfLeadingZeros(x);
   }

   private static int findFirstSignificantBit(final int x) {
      return Integer.numberOfTrailingZeros(x);
   }

   private TlsfAllocator.IndexPair getLevelIndex(final long size) {
      if (Long.compareUnsigned(size, 256L) < 0) {
         int firstLevelIndex = 0;
         int secondLevelIndex = (int)Long.divideUnsigned(size, 32L);
         return new TlsfAllocator.IndexPair(0, secondLevelIndex);
      } else {
         int firstLevelIndex = findLastSignificantBit(size);
         int secondLevelIndex = (int)(size >>> firstLevelIndex - 3) ^ 8;
         firstLevelIndex -= 7;
         return new TlsfAllocator.IndexPair(firstLevelIndex, secondLevelIndex);
      }
   }

   private TlsfAllocator.IndexPair mappingSearch(final long size) {
      long roundedSize = size;
      if (Long.compareUnsigned(size, 256L) >= 0) {
         long round = (1L << findLastSignificantBit(size) - 3) - 1L;
         roundedSize += round;
      }

      return this.getLevelIndex(roundedSize);
   }

   private void insertFreeBlock(final TlsfAllocator.Block block) {
      TlsfAllocator.IndexPair levelIndex = this.getLevelIndex(block.getSize());
      TlsfAllocator.Block currentBlock = this.getBlockFromFreeList(levelIndex.firstLevelIndex, levelIndex.secondLevelIndex);
      if (currentBlock != null) {
         currentBlock.previousFreeBlock = block;
      }

      block.nextFreeBlock = currentBlock;
      this.firstLevelBitmap = this.firstLevelBitmap | 1 << levelIndex.firstLevelIndex;
      this.secondLevelBitmap[levelIndex.firstLevelIndex] = this.secondLevelBitmap[levelIndex.firstLevelIndex] | 1 << levelIndex.secondLevelIndex;
      this.setBlockFreeList(levelIndex.firstLevelIndex, levelIndex.secondLevelIndex, block);
   }

   private void removeFreeBlock(final TlsfAllocator.Block block, final int firstLevel, final int secondLevel) {
      TlsfAllocator.Block next = block.nextFreeBlock;
      TlsfAllocator.Block previous = block.previousFreeBlock;
      if (previous != null) {
         previous.nextFreeBlock = next;
      }

      if (next != null) {
         next.previousFreeBlock = previous;
      }

      if (this.getBlockFromFreeList(firstLevel, secondLevel) == block) {
         this.setBlockFreeList(firstLevel, secondLevel, next);
         if (next == null) {
            this.secondLevelBitmap[firstLevel] = this.secondLevelBitmap[firstLevel] & ~(1 << secondLevel);
            if (this.secondLevelBitmap[firstLevel] == 0) {
               this.firstLevelBitmap &= ~(1 << firstLevel);
            }
         }
      }
   }

   private void trimBlock(final TlsfAllocator.Block block, final long size) {
      if (Long.compareUnsigned(block.getSize(), size + 256L) > 0) {
         long remaining = block.getSize() - size;
         TlsfAllocator.Block remainingBlock = new TlsfAllocator.Block(
            remaining, block.heap, block.offsetFromHeap + size, null, null, block.nextPhysicalBlock, block
         );
         remainingBlock.setFree();
         TlsfAllocator.Block next = block.nextPhysicalBlock;
         if (next != null) {
            assert next.previousPhysicalBlock == block;
            next.previousPhysicalBlock = remainingBlock;
         }

         block.nextPhysicalBlock = remainingBlock;
         block.setSize(size);
         this.mergeBlockWithNext(remainingBlock);
         this.insertFreeBlock(remainingBlock);
      }
   }

   public TlsfAllocator.@Nullable Allocation allocate(final long size, final int align) {
      boolean isPowerOfTwo = Mth.isPowerOfTwo(align);
      int sizePadding = isPowerOfTwo && align <= 32 ? 0 : align;
      long alignedSize = alignUp(size + sizePadding, 32L);
      assert alignedSize <= 549755813888L;
      TlsfAllocator.IndexPair levelIndex = this.mappingSearch(alignedSize);
      int firstLevelIndex = levelIndex.firstLevelIndex;
      int secondLevelIndex = levelIndex.secondLevelIndex;
      long firstLevelSize = 1L << firstLevelIndex + 8 - 1;
      long secondLevelInterval = firstLevelSize / 8L;
      long slotSize = firstLevelSize + secondLevelIndex * secondLevelInterval;
      if (firstLevelIndex < 32) {
         int slBitmap = this.secondLevelBitmap[firstLevelIndex] & -1 << secondLevelIndex;
         if (slBitmap == 0) {
            int flBitmap = this.firstLevelBitmap & -1 << firstLevelIndex + 1;
            if (flBitmap == 0) {
               return null;
            }

            firstLevelIndex = findFirstSignificantBit(flBitmap);
            slBitmap = this.secondLevelBitmap[firstLevelIndex];
         }

         secondLevelIndex = findFirstSignificantBit(slBitmap);
         TlsfAllocator.Block block = this.getBlockFromFreeList(firstLevelIndex, secondLevelIndex);
         assert block != null && block.getSize() >= alignedSize;
         this.removeFreeBlock(block, firstLevelIndex, secondLevelIndex);
         this.trimBlock(block, slotSize);
         block.setUsed();
         long gap = Long.remainderUnsigned(block.offsetFromHeap, align);
         long alignmentOffset = gap == 0L ? 0L : align - gap;
         long allocationOffset = block.offsetFromHeap + alignmentOffset;
         return new TlsfAllocator.Allocation(block, allocationOffset);
      } else {
         return null;
      }
   }

   private void mergeBlockWithPrevious(final TlsfAllocator.Block block) {
      TlsfAllocator.Block previous = block.previousPhysicalBlock;
      if (previous != null && previous.isFree()) {
         assert previous.offsetFromHeap + previous.getSize() == block.offsetFromHeap;
         assert previous.nextPhysicalBlock == block;
         TlsfAllocator.IndexPair levelIndex = this.getLevelIndex(previous.getSize());
         this.removeFreeBlock(previous, levelIndex.firstLevelIndex, levelIndex.secondLevelIndex);
         TlsfAllocator.Block prevprev = previous.previousPhysicalBlock;
         if (prevprev != null) {
            assert prevprev.nextPhysicalBlock == previous;
            prevprev.nextPhysicalBlock = block;
         }

         block.previousPhysicalBlock = prevprev;
         block.setSize(block.getSize() + previous.getSize());
         block.offsetFromHeap = previous.offsetFromHeap;
         previous.previousPhysicalBlock = null;
         previous.nextPhysicalBlock = null;
         previous.nextFreeBlock = null;
         previous.previousFreeBlock = null;
      }
   }

   private void mergeBlockWithNext(final TlsfAllocator.Block block) {
      TlsfAllocator.Block next = block.nextPhysicalBlock;
      if (next != null && next.isFree()) {
         assert next.offsetFromHeap - block.getSize() == block.offsetFromHeap;
         assert next.previousPhysicalBlock == block;
         TlsfAllocator.IndexPair levelIndex = this.getLevelIndex(next.getSize());
         this.removeFreeBlock(next, levelIndex.firstLevelIndex, levelIndex.secondLevelIndex);
         TlsfAllocator.Block nextnext = next.nextPhysicalBlock;
         if (nextnext != null) {
            assert nextnext.previousPhysicalBlock == next;
            nextnext.previousPhysicalBlock = block;
         }

         block.nextPhysicalBlock = nextnext;
         block.setSize(block.getSize() + next.getSize());
         next.previousPhysicalBlock = null;
         next.nextPhysicalBlock = null;
         next.nextFreeBlock = null;
         next.previousFreeBlock = null;
      }
   }

   public void free(final TlsfAllocator.Allocation allocation) {
      if (!allocation.freed) {
         TlsfAllocator.Block block = allocation.block;
         block.setFree();
         this.mergeBlockWithPrevious(block);
         this.mergeBlockWithNext(block);
         this.insertFreeBlock(block);
         allocation.freed = true;
      }
   }

   public boolean isCompletelyFree() {
      if (Long.bitCount(this.firstLevelBitmap) == 1) {
         int firstLevelIndex = findFirstSignificantBit(this.firstLevelBitmap);
         int slBitmap = this.secondLevelBitmap[firstLevelIndex];
         if (Long.bitCount(slBitmap) == 1) {
            int secondLevelIndex = findFirstSignificantBit(slBitmap);
            TlsfAllocator.Block freeBlock = this.getBlockFromFreeList(firstLevelIndex, secondLevelIndex);
            return freeBlock != null && freeBlock.getSize() == this.totalMemorySize;
         }
      }

      return false;
   }

   @VisibleForDebug
   public void printAllocatorStatistics(final String name) {
      int freeBlockCount = 0;
      long freeMemorySize = 0L;
      int levelFreeBlockCount = 0;

      for (int i = 0; i < 32; i++) {
         for (int j = 0; j < 8; j++) {
            levelFreeBlockCount = 0;

            for (TlsfAllocator.Block block = this.getBlockFromFreeList(i, j); block != null; block = block.nextFreeBlock) {
               levelFreeBlockCount++;
               freeMemorySize += block.getSize();
            }

            freeBlockCount += levelFreeBlockCount;
         }
      }

      double unusedPercent = (double)freeMemorySize / this.totalMemorySize * 100.0;
      LOGGER.debug(
         "Uber buffer {}, size: {} -- free-listed memory: {} -- free-block count:{}", new Object[]{name, this.totalMemorySize, unusedPercent, freeBlockCount}
      );
   }

   public static class Allocation {
      private final TlsfAllocator.Block block;
      private final long offsetFromHeap;
      private boolean freed = false;

      private Allocation(final TlsfAllocator.Block block, final long offsetFromHeap) {
         this.block = block;
         this.offsetFromHeap = offsetFromHeap;
      }

      public long getSize() {
         return this.block.getSize();
      }

      public TlsfAllocator.Heap getHeap() {
         return this.block.heap;
      }

      public long getOffsetFromHeap() {
         return this.offsetFromHeap;
      }

      public boolean isFreed() {
         return this.freed;
      }
   }

   private static class Block {
      private long size = 0L;
      public final TlsfAllocator.Heap heap;
      public long offsetFromHeap;
      public TlsfAllocator.@Nullable Block nextFreeBlock;
      public TlsfAllocator.@Nullable Block previousFreeBlock;
      public TlsfAllocator.@Nullable Block nextPhysicalBlock;
      public TlsfAllocator.@Nullable Block previousPhysicalBlock;
      private static final int BLOCK_HEADER_FREE_BIT = 1;

      private Block(
         final long size,
         final TlsfAllocator.Heap heap,
         final long offsetFromHeap,
         final TlsfAllocator.@Nullable Block nextFreeBlock,
         final TlsfAllocator.@Nullable Block previousFreeBlock,
         final TlsfAllocator.@Nullable Block nextPhysicalBlock,
         final TlsfAllocator.@Nullable Block previousPhysicalBlock
      ) {
         this.heap = heap;
         this.offsetFromHeap = offsetFromHeap;
         this.nextFreeBlock = nextFreeBlock;
         this.previousFreeBlock = previousFreeBlock;
         this.nextPhysicalBlock = nextPhysicalBlock;
         this.previousPhysicalBlock = previousPhysicalBlock;
         this.setSize(size);
      }

      private boolean isFree() {
         return (this.size & 1L) == 1L;
      }

      private void setFree() {
         this.size |= 1L;
      }

      private void setUsed() {
         this.size &= -2L;
      }

      private long getSize() {
         return this.size & -2L;
      }

      private void setSize(final long size) {
         long oldSize = this.size;
         this.size = size | oldSize & 1L;
      }
   }

   public static class Heap {
      private final long size;

      public Heap(final long size) {
         this.size = size;
      }
   }

   private record IndexPair(int firstLevelIndex, int secondLevelIndex) {
   }
}
