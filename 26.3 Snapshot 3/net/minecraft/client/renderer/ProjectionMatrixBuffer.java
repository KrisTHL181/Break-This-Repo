package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.device.GpuDevice;
import java.nio.ByteBuffer;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

public class ProjectionMatrixBuffer implements AutoCloseable {
   private final GpuBuffer buffer;
   private final GpuBufferSlice bufferSlice;
   private @Nullable Projection lastUploadedProjection;
   private long projectionMatrixVersion;
   private final Matrix4f tempMatrix = new Matrix4f();

   public ProjectionMatrixBuffer(final String name) {
      this.lastUploadedProjection = null;
      this.projectionMatrixVersion = -1L;
      GpuDevice device = RenderSystem.getDevice();
      this.buffer = device.createBuffer(() -> "Camera projection matrix UBO " + name, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
      this.bufferSlice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
   }

   public GpuBufferSlice getBuffer(final Projection projection) {
      assert projection.getMatrixVersion() != -1L;
      if (this.lastUploadedProjection == projection && projection.getMatrixVersion() == this.projectionMatrixVersion) {
         return this.bufferSlice;
      }

      this.lastUploadedProjection = projection;
      this.projectionMatrixVersion = projection.getMatrixVersion();
      return this.writeBuffer(projection.getMatrix(this.tempMatrix));
   }

   public GpuBufferSlice getBuffer(final Matrix4f projectionMatrix) {
      this.lastUploadedProjection = null;
      this.projectionMatrixVersion = -1L;
      return this.writeBuffer(projectionMatrix);
   }

   private GpuBufferSlice writeBuffer(final Matrix4f projectionMatrix) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer byteBuffer = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(projectionMatrix).get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), byteBuffer);
      } catch (Throwable var6) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stack != null) {
         stack.close();
      }

      return this.bufferSlice;
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
