package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface RenderPass extends AutoCloseable {
   void pushDebugGroup(Supplier<String> var1);

   void popDebugGroup();

   void setPipeline(RenderPipeline var1);

   void bindTexture(String var1, @Nullable GpuTextureView var2, @Nullable GpuSampler var3);

   void setUniform(String var1, GpuBuffer var2);

   void setUniform(String var1, GpuBufferSlice var2);

   void enableScissor(int var1, int var2, int var3, int var4);

   void disableScissor();

   void setVertexBuffer(int var1, GpuBuffer var2);

   void setIndexBuffer(GpuBuffer var1, VertexFormat.IndexType var2);

   void drawIndexed(int var1, int var2, int var3, int var4);

   <T> void drawMultipleIndexed(
      Collection<RenderPass.Draw<T>> var1, @Nullable GpuBuffer var2, VertexFormat.@Nullable IndexType var3, Collection<String> var4, T var5
   );

   void draw(int var1, int var2);

   @Override
   void close();

   @OnlyIn(Dist.CLIENT)
   record Draw<T>(
      int slot,
      GpuBuffer vertexBuffer,
      @Nullable GpuBuffer indexBuffer,
      VertexFormat.@Nullable IndexType indexType,
      int firstIndex,
      int indexCount,
      @Nullable BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer
   ) {
      public Draw(int p_394209_, GpuBuffer p_394761_, GpuBuffer p_393439_, VertexFormat.IndexType p_393418_, int p_392985_, int p_394886_) {
         this(p_394209_, p_394761_, p_393439_, p_393418_, p_392985_, p_394886_, null);
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface UniformUploader {
      void upload(String var1, GpuBufferSlice var2);
   }
}
