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
public class CachedPerspectiveProjectionMatrixBuffer implements AutoCloseable {
   private final GpuBuffer buffer;
   private final GpuBufferSlice bufferSlice;
   private final float zNear;
   private final float zFar;
   private int width;
   private int height;
   private float fov;

   public CachedPerspectiveProjectionMatrixBuffer(String p_406837_, float p_405955_, float p_406182_) {
      this.zNear = p_405955_;
      this.zFar = p_406182_;
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.buffer = gpudevice.createBuffer(() -> "Projection matrix UBO " + p_406837_, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
      this.bufferSlice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
   }

   public GpuBufferSlice getBuffer(int p_406375_, int p_407827_, float p_408594_) {
      if (this.width != p_406375_ || this.height != p_407827_ || this.fov != p_408594_) {
         Matrix4f matrix4f = this.createProjectionMatrix(p_406375_, p_407827_, p_408594_);
         MemoryStack memorystack = MemoryStack.stackPush();

         try {
            ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(matrix4f).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
         } catch (Throwable var9) {
            if (memorystack != null) {
               try {
                  memorystack.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (memorystack != null) {
            memorystack.close();
         }

         this.width = p_406375_;
         this.height = p_407827_;
         this.fov = p_408594_;
      }

      return this.bufferSlice;
   }

   private Matrix4f createProjectionMatrix(int p_406878_, int p_407251_, float p_408735_) {
      return new Matrix4f().perspective(p_408735_ * (float) (Math.PI / 180.0), (float)p_406878_ / p_407251_, this.zNear, this.zFar);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
