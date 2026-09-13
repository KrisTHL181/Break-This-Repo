package com.mojang.blaze3d.vertex;

import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryUtil.MemoryAllocator;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ByteBufferBuilder implements AutoCloseable {
   private static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool("ByteBufferBuilder");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
   private static final long DEFAULT_MAX_CAPACITY = 4294967295L;
   private static final int MAX_GROWTH_SIZE = 2097152;
   private static final int BUFFER_FREED_GENERATION = -1;
   long pointer;
   private long capacity;
   private final long maxCapacity;
   private long writeOffset;
   private long nextResultOffset;
   private int resultCount;
   private int generation;

   public ByteBufferBuilder(int p_408080_, long p_410447_) {
      this.capacity = p_408080_;
      this.maxCapacity = p_410447_;
      this.pointer = ALLOCATOR.malloc(p_408080_);
      MEMORY_POOL.malloc(this.pointer, p_408080_);
      if (this.pointer == 0L) {
         throw new OutOfMemoryError("Failed to allocate " + p_408080_ + " bytes");
      }
   }

   public ByteBufferBuilder(int p_344576_) {
      this(p_344576_, 4294967295L);
   }

   public static ByteBufferBuilder exactlySized(int p_406345_) {
      return new ByteBufferBuilder(p_406345_, p_406345_);
   }

   public long reserve(int p_342985_) {
      long i = this.writeOffset;
      long j = Math.addExact(i, p_342985_);
      this.ensureCapacity(j);
      this.writeOffset = j;
      return Math.addExact(this.pointer, i);
   }

   private void ensureCapacity(long p_408008_) {
      if (p_408008_ > this.capacity) {
         if (p_408008_ > this.maxCapacity) {
            throw new IllegalArgumentException("Maximum capacity of ByteBufferBuilder (" + this.maxCapacity + ") exceeded, required " + p_408008_);
         }

         long i = Math.min(this.capacity, 2097152L);
         long j = Mth.clamp(this.capacity + i, p_408008_, this.maxCapacity);
         this.resize(j);
      }
   }

   private void resize(long p_408815_) {
      MEMORY_POOL.free(this.pointer);
      this.pointer = ALLOCATOR.realloc(this.pointer, p_408815_);
      MEMORY_POOL.malloc(this.pointer, (int)Math.min(p_408815_, 2147483647L));
      LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.capacity, p_408815_);
      if (this.pointer == 0L) {
         throw new OutOfMemoryError("Failed to resize buffer from " + this.capacity + " bytes to " + p_408815_ + " bytes");
      }

      this.capacity = p_408815_;
   }

   public ByteBufferBuilder.@Nullable Result build() {
      this.checkOpen();
      long i = this.nextResultOffset;
      long j = this.writeOffset - i;
      if (j == 0L) {
         return null;
      }

      if (j > 2147483647L) {
         throw new IllegalStateException("Cannot build buffer larger than 2147483647 bytes (was " + j + ")");
      }

      this.nextResultOffset = this.writeOffset;
      this.resultCount++;
      return new ByteBufferBuilder.Result(i, (int)j, this.generation);
   }

   public void clear() {
      if (this.resultCount > 0) {
         LOGGER.warn("Clearing BufferBuilder with unused batches");
      }

      this.discard();
   }

   public void discard() {
      this.checkOpen();
      if (this.resultCount > 0) {
         this.discardResults();
         this.resultCount = 0;
      }
   }

   boolean isValid(int p_344177_) {
      return p_344177_ == this.generation;
   }

   void freeResult() {
      if (--this.resultCount <= 0) {
         this.discardResults();
      }
   }

   private void discardResults() {
      long i = this.writeOffset - this.nextResultOffset;
      if (i > 0L) {
         MemoryUtil.memCopy(this.pointer + this.nextResultOffset, this.pointer, i);
      }

      this.writeOffset = i;
      this.nextResultOffset = 0L;
      this.generation++;
   }

   @Override
   public void close() {
      if (this.pointer != 0L) {
         MEMORY_POOL.free(this.pointer);
         ALLOCATOR.free(this.pointer);
         this.pointer = 0L;
         this.generation = -1;
      }
   }

   private void checkOpen() {
      if (this.pointer == 0L) {
         throw new IllegalStateException("Buffer has been freed");
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class Result implements AutoCloseable {
      private final long offset;
      private final int capacity;
      private final int generation;
      private boolean closed;

      Result(final long p_409431_, final int p_343613_, final int p_344565_) {
         this.offset = p_409431_;
         this.capacity = p_343613_;
         this.generation = p_344565_;
      }

      public ByteBuffer byteBuffer() {
         if (!ByteBufferBuilder.this.isValid(this.generation)) {
            throw new IllegalStateException("Buffer is no longer valid");
         } else {
            return MemoryUtil.memByteBuffer(ByteBufferBuilder.this.pointer + this.offset, this.capacity);
         }
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            if (ByteBufferBuilder.this.isValid(this.generation)) {
               ByteBufferBuilder.this.freeResult();
            }
         }
      }
   }
}
