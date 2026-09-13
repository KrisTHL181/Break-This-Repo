package com.mojang.renderpearl.backend.util;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public class TransientBlockAllocator<T> implements AutoCloseable {
   private final long blockSize;
   private final long maxAlignment;
   private final TransientBlockAllocator.Allocator<T> allocator;
   private final Consumer<T> onBlockUse;
   private final ReferenceArrayList<T> specialBlocks = new ReferenceArrayList();
   private final ReferenceArrayList<T> freeBlocks = new ReferenceArrayList();
   private final ReferenceArrayList<T> usedBlocks = new ReferenceArrayList();
   private @Nullable T currentBlock;
   private long currentOffset = 0L;

   public TransientBlockAllocator(final long blockSize, final long maxAlignment, final TransientBlockAllocator.Allocator<T> allocator) {
      this(blockSize, maxAlignment, allocator, var0 -> {});
   }

   public TransientBlockAllocator(
      final long blockSize, final long maxAlignment, final TransientBlockAllocator.Allocator<T> allocator, final Consumer<T> onBlockUse
   ) {
      this.blockSize = blockSize;
      this.maxAlignment = maxAlignment;
      this.allocator = allocator;
      this.onBlockUse = onBlockUse;
   }

   @Override
   public void close() {
      this.rotate().run();
      this.rotate().run();
   }

   public long blockSize() {
      return this.blockSize;
   }

   public Runnable rotate() {
      this.currentBlock = null;
      this.currentOffset = this.blockSize;
      this.freeBlocks.forEach(this.allocator::free);
      this.freeBlocks.clear();
      if (this.usedBlocks.isEmpty() && this.specialBlocks.isEmpty()) {
         return () -> {};
      }

      ReferenceArrayList<T> blocksUsedThisRotation = this.usedBlocks.clone();
      this.usedBlocks.clear();
      ReferenceArrayList<T> specialBlocksUsedThisRotation = this.specialBlocks.clone();
      this.specialBlocks.clear();
      return () -> {
         if (!blocksUsedThisRotation.isEmpty()) {
            this.allocator.free((T)blocksUsedThisRotation.pop());
         }

         this.freeBlocks.addAll(blocksUsedThisRotation);
         specialBlocksUsedThisRotation.forEach(this.allocator::free);
      };
   }

   @Contract(pure = true)
   public boolean canAllocateInBlock(final long size, final long alignment) {
      return size <= this.blockSize && alignment <= this.maxAlignment;
   }

   @Contract(pure = true)
   public boolean canAllocateInCurrentBlock(final long size, final long alignment) {
      if (this.currentBlock == null && this.canAllocateInBlock(size, alignment)) {
         return true;
      }

      long alignedOffset = Mth.roundToward(this.currentOffset, alignment);
      return size <= this.blockSize - alignedOffset && alignment <= this.maxAlignment;
   }

   private T allocateBlock() {
      if (this.freeBlocks.isEmpty()) {
         this.freeBlocks.add(this.allocator.alloc(this.blockSize));
      }

      T block = (T)this.freeBlocks.pop();
      this.onBlockUse.accept(block);
      this.usedBlocks.add(block);
      return block;
   }

   public TransientBlockAllocator.Allocation<T> allocate(final long size, final long alignment, final long minimumAllocation, final long elementSize) {
      if (alignment > this.maxAlignment) {
         throw new IllegalArgumentException("Alignment requirement over maximum supported alignment");
      }

      if (size == this.blockSize) {
         return new TransientBlockAllocator.Allocation<>(this.allocateBlock(), 0L, this.blockSize);
      }

      if (!this.canAllocateInBlock(size, alignment)) {
         T specialBlock = this.allocator.alloc(size);
         this.onBlockUse.accept(specialBlock);
         this.specialBlocks.add(specialBlock);
         return new TransientBlockAllocator.Allocation<>(specialBlock, 0L, size);
      }

      if (this.currentBlock == null) {
         this.currentBlock = this.allocateBlock();
         this.currentOffset = 0L;
      }

      if (this.canAllocateInCurrentBlock(size, alignment)) {
         assert this.currentBlock != null;
         long alignedOffset = Mth.roundToward(this.currentOffset, alignment);
         this.currentOffset = alignedOffset + size;
         T block = this.currentBlock;
         return new TransientBlockAllocator.Allocation<>(block, alignedOffset, size);
      }

      if (this.canAllocateInCurrentBlock(minimumAllocation, alignment)) {
         assert this.currentBlock != null;
         long alignedOffset = Mth.roundToward(this.currentOffset, alignment);
         long allocatedSize = (this.blockSize - alignedOffset) / elementSize * elementSize;
         this.currentOffset = alignedOffset + allocatedSize;
         T block = this.currentBlock;
         return new TransientBlockAllocator.Allocation<>(block, alignedOffset, allocatedSize);
      }

      T newBlock = this.allocateBlock();
      if (this.currentOffset > size) {
         this.currentBlock = newBlock;
         this.currentOffset = size;
      }

      return new TransientBlockAllocator.Allocation<>(newBlock, 0L, size);
   }

   public record Allocation<T>(T block, long offset, long size) {
   }

   public interface Allocator<T> {
      T alloc(long size);

      void free(T t);

      static <T> TransientBlockAllocator.Allocator<T> create(final LongFunction<T> alloc, final Consumer<T> free) {
         return new TransientBlockAllocator.Allocator<T>() {
            @Override
            public T alloc(final long size) {
               return alloc.apply(size);
            }

            @Override
            public void free(final T t) {
               free.accept(t);
            }
         };
      }
   }
}
