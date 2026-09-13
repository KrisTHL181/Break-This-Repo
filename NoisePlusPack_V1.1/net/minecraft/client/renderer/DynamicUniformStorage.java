package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final List<MappableRingBuffer> oldBuffers = new ArrayList<>();
   private final int blockSize;
   private MappableRingBuffer ringBuffer;
   private int nextBlock;
   private int capacity;
   private @Nullable T lastUniform;
   private final String label;

   public DynamicUniformStorage(String p_406108_, int p_409753_, int p_407366_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.blockSize = Mth.roundToward(p_409753_, gpudevice.getUniformOffsetAlignment());
      this.capacity = Mth.smallestEncompassingPowerOfTwo(p_407366_);
      this.nextBlock = 0;
      this.ringBuffer = new MappableRingBuffer(() -> p_406108_ + " x" + this.blockSize, 130, this.blockSize * this.capacity);
      this.label = p_406108_;
   }

   public void endFrame() {
      this.nextBlock = 0;
      this.lastUniform = null;
      this.ringBuffer.rotate();
      if (!this.oldBuffers.isEmpty()) {
         for (MappableRingBuffer mappableringbuffer : this.oldBuffers) {
            mappableringbuffer.close();
         }

         this.oldBuffers.clear();
      }
   }

   private void resizeBuffers(int p_410628_) {
      this.capacity = p_410628_;
      this.nextBlock = 0;
      this.lastUniform = null;
      this.oldBuffers.add(this.ringBuffer);
      this.ringBuffer = new MappableRingBuffer(() -> this.label + " x" + this.blockSize, 130, this.blockSize * this.capacity);
   }

   public GpuBufferSlice writeUniform(T p_407834_) {
      if (this.lastUniform != null && this.lastUniform.equals(p_407834_)) {
         return this.ringBuffer.currentBuffer().slice((this.nextBlock - 1) * this.blockSize, this.blockSize);
      }

      if (this.nextBlock >= this.capacity) {
         int i = this.capacity * 2;
         LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", new Object[]{this.label, this.capacity, i});
         this.resizeBuffers(i);
      }

      int j = this.nextBlock * this.blockSize;

      try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
            .createCommandEncoder()
            .mapBuffer(this.ringBuffer.currentBuffer().slice(j, this.blockSize), false, true)) {
         p_407834_.write(gpubuffer$mappedview.data());
      }

      this.nextBlock++;
      this.lastUniform = p_407834_;
      return this.ringBuffer.currentBuffer().slice(j, this.blockSize);
   }

   public GpuBufferSlice[] writeUniforms(T[] p_409933_) {
      if (p_409933_.length == 0) {
         return new GpuBufferSlice[0];
      }

      if (this.nextBlock + p_409933_.length > this.capacity) {
         int i = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, p_409933_.length));
         LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", new Object[]{this.label, this.capacity, i});
         this.resizeBuffers(i);
      }

      int k = this.nextBlock * this.blockSize;
      GpuBufferSlice[] agpubufferslice = new GpuBufferSlice[p_409933_.length];

      try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
            .createCommandEncoder()
            .mapBuffer(this.ringBuffer.currentBuffer().slice(k, p_409933_.length * this.blockSize), false, true)) {
         ByteBuffer bytebuffer = gpubuffer$mappedview.data();

         for (int j = 0; j < p_409933_.length; j++) {
            T t = p_409933_[j];
            agpubufferslice[j] = this.ringBuffer.currentBuffer().slice(k + j * this.blockSize, this.blockSize);
            bytebuffer.position(j * this.blockSize);
            t.write(bytebuffer);
         }
      }

      this.nextBlock += p_409933_.length;
      this.lastUniform = p_409933_[p_409933_.length - 1];
      return agpubufferslice;
   }

   @Override
   public void close() {
      for (MappableRingBuffer mappableringbuffer : this.oldBuffers) {
         mappableringbuffer.close();
      }

      this.ringBuffer.close();
   }

   @OnlyIn(Dist.CLIENT)
   public interface DynamicUniform {
      void write(ByteBuffer var1);
   }
}
