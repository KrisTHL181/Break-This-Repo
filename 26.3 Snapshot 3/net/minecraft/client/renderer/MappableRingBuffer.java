package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.device.GpuDevice;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class MappableRingBuffer implements AutoCloseable {
   private static final int BUFFER_COUNT = 3;
   private final GpuBuffer[] buffers = new GpuBuffer[3];
   private final @Nullable GpuFence[] fences = new GpuFence[3];
   private final int size;
   private int current = 0;

   public MappableRingBuffer(final Supplier<String> label, final @GpuBuffer.Usage int usage, final int size) {
      GpuDevice device = RenderSystem.getDevice();
      if ((usage & 1) == 0 && (usage & 2) == 0) {
         throw new IllegalArgumentException("MappableRingBuffer requires at least one of USAGE_MAP_READ or USAGE_MAP_WRITE");
      }

      for (int i = 0; i < 3; i++) {
         int finalI = i;
         this.buffers[i] = device.createBuffer(() -> label.get() + " #" + finalI, usage, size);
         this.fences[i] = null;
      }

      this.size = size;
   }

   public int size() {
      return this.size;
   }

   public GpuBuffer currentBuffer() {
      GpuFence fence = this.fences[this.current];
      if (fence != null) {
         fence.awaitCompletion(Long.MAX_VALUE);
         fence.close();
         this.fences[this.current] = null;
      }

      return this.buffers[this.current];
   }

   public void rotate() {
      if (this.fences[this.current] != null) {
         this.fences[this.current].close();
      }

      this.fences[this.current] = RenderSystem.getDevice().createCommandEncoder().createFence();
      this.current = (this.current + 1) % 3;
   }

   @Override
   public void close() {
      for (int i = 0; i < 3; i++) {
         this.buffers[i].close();
         if (this.fences[i] != null) {
            this.fences[i].close();
         }
      }
   }
}
