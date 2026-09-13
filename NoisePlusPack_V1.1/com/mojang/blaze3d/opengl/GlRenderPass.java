package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlRenderPass implements RenderPass {
   protected static final int MAX_VERTEX_BUFFERS = 1;
   public static final boolean VALIDATION = SharedConstants.IS_RUNNING_IN_IDE;
   private final GlCommandEncoder encoder;
   private final boolean hasDepthTexture;
   private boolean closed;
   protected @Nullable GlRenderPipeline pipeline;
   protected final @Nullable GpuBuffer[] vertexBuffers = new GpuBuffer[1];
   protected @Nullable GpuBuffer indexBuffer;
   protected VertexFormat.IndexType indexType = VertexFormat.IndexType.INT;
   private final ScissorState scissorState = new ScissorState();
   protected final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
   protected final HashMap<String, GlRenderPass.TextureViewAndSampler> samplers = new HashMap<>();
   protected final Set<String> dirtyUniforms = new HashSet<>();
   protected int pushedDebugGroups;

   public GlRenderPass(GlCommandEncoder p_394151_, boolean p_398021_) {
      this.encoder = p_394151_;
      this.hasDepthTexture = p_398021_;
   }

   public boolean hasDepthTexture() {
      return this.hasDepthTexture;
   }

   @Override
   public void pushDebugGroup(Supplier<String> p_409060_) {
      if (this.closed) {
         throw new IllegalStateException("Can't use a closed render pass");
      }

      this.pushedDebugGroups++;
      this.encoder.getDevice().debugLabels().pushDebugGroup(p_409060_);
   }

   @Override
   public void popDebugGroup() {
      if (this.closed) {
         throw new IllegalStateException("Can't use a closed render pass");
      }

      if (this.pushedDebugGroups == 0) {
         throw new IllegalStateException("Can't pop more debug groups than was pushed!");
      }

      this.pushedDebugGroups--;
      this.encoder.getDevice().debugLabels().popDebugGroup();
   }

   @Override
   public void setPipeline(RenderPipeline p_394211_) {
      if (this.pipeline == null || this.pipeline.info() != p_394211_) {
         this.dirtyUniforms.addAll(this.uniforms.keySet());
         this.dirtyUniforms.addAll(this.samplers.keySet());
      }

      this.pipeline = this.encoder.getDevice().getOrCompilePipeline(p_394211_);
   }

   @Override
   public void bindTexture(String p_455274_, @Nullable GpuTextureView p_459703_, @Nullable GpuSampler p_458642_) {
      if (p_458642_ == null) {
         this.samplers.remove(p_455274_);
      } else {
         this.samplers.put(p_455274_, new GlRenderPass.TextureViewAndSampler((GlTextureView)p_459703_, (GlSampler)p_458642_));
      }

      this.dirtyUniforms.add(p_455274_);
   }

   @Override
   public void setUniform(String p_394528_, GpuBuffer p_408743_) {
      this.uniforms.put(p_394528_, p_408743_.slice());
      this.dirtyUniforms.add(p_394528_);
   }

   @Override
   public void setUniform(String p_394503_, GpuBufferSlice p_406404_) {
      int i = this.encoder.getDevice().getUniformOffsetAlignment();
      if (p_406404_.offset() % i > 0L) {
         throw new IllegalArgumentException("Uniform buffer offset must be aligned to " + i);
      }

      this.uniforms.put(p_394503_, p_406404_);
      this.dirtyUniforms.add(p_394503_);
   }

   @Override
   public void enableScissor(int p_394105_, int p_397366_, int p_397303_, int p_391821_) {
      this.scissorState.enable(p_394105_, p_397366_, p_397303_, p_391821_);
   }

   @Override
   public void disableScissor() {
      this.scissorState.disable();
   }

   public boolean isScissorEnabled() {
      return this.scissorState.enabled();
   }

   public int getScissorX() {
      return this.scissorState.x();
   }

   public int getScissorY() {
      return this.scissorState.y();
   }

   public int getScissorWidth() {
      return this.scissorState.width();
   }

   public int getScissorHeight() {
      return this.scissorState.height();
   }

   @Override
   public void setVertexBuffer(int p_394641_, GpuBuffer p_397665_) {
      if (p_394641_ >= 0 && p_394641_ < 1) {
         this.vertexBuffers[p_394641_] = p_397665_;
      } else {
         throw new IllegalArgumentException("Vertex buffer slot is out of range: " + p_394641_);
      }
   }

   @Override
   public void setIndexBuffer(@Nullable GpuBuffer p_393276_, VertexFormat.IndexType p_392180_) {
      this.indexBuffer = p_393276_;
      this.indexType = p_392180_;
   }

   @Override
   public void drawIndexed(int p_393186_, int p_395612_, int p_406330_, int p_410739_) {
      if (this.closed) {
         throw new IllegalStateException("Can't use a closed render pass");
      }

      this.encoder.executeDraw(this, p_393186_, p_395612_, p_406330_, this.indexType, p_410739_);
   }

   @Override
   public <T> void drawMultipleIndexed(
      Collection<RenderPass.Draw<T>> p_394525_,
      @Nullable GpuBuffer p_397624_,
      VertexFormat.@Nullable IndexType p_391410_,
      Collection<String> p_408880_,
      T p_407505_
   ) {
      if (this.closed) {
         throw new IllegalStateException("Can't use a closed render pass");
      }

      this.encoder.executeDrawMultiple(this, p_394525_, p_397624_, p_391410_, p_408880_, p_407505_);
   }

   @Override
   public void draw(int p_392940_, int p_391785_) {
      if (this.closed) {
         throw new IllegalStateException("Can't use a closed render pass");
      }

      this.encoder.executeDraw(this, p_392940_, 0, p_391785_, null, 1);
   }

   @Override
   public void close() {
      if (!this.closed) {
         if (this.pushedDebugGroups > 0) {
            throw new IllegalStateException("Render pass had debug groups left open!");
         }

         this.closed = true;
         this.encoder.finishRenderPass();
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected record TextureViewAndSampler(GlTextureView view, GlSampler sampler) {
   }
}
