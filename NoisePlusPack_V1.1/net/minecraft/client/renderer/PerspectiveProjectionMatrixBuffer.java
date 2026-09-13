package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class PerspectiveProjectionMatrixBuffer implements AutoCloseable {
   private final GpuBuffer buffer;
   private final GpuBufferSlice bufferSlice;

   public PerspectiveProjectionMatrixBuffer(String p_406651_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.buffer = gpudevice.createBuffer(() -> "Projection matrix UBO " + p_406651_, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
      this.bufferSlice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
   }

   public GpuBufferSlice getBuffer(Matrix4f p_408315_) {
      MemoryStack memorystack = MemoryStack.stackPush();

      try {
         ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(p_408315_).get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
      } catch (Throwable var6) {
         if (memorystack != null) {
            try {
               memorystack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (memorystack != null) {
         memorystack.close();
      }

      return this.bufferSlice;
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
