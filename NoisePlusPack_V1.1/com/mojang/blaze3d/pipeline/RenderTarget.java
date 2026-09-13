package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.OptionalInt;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class RenderTarget {
   private static int UNNAMED_RENDER_TARGETS = 0;
   public int width;
   public int height;
   protected final String label;
   public final boolean useDepth;
   protected @Nullable GpuTexture colorTexture;
   protected @Nullable GpuTextureView colorTextureView;
   protected @Nullable GpuTexture depthTexture;
   protected @Nullable GpuTextureView depthTextureView;

   public RenderTarget(@Nullable String p_392164_, boolean p_166199_) {
      this.label = p_392164_ == null ? "FBO " + UNNAMED_RENDER_TARGETS++ : p_392164_;
      this.useDepth = p_166199_;
   }

   public void resize(int p_83942_, int p_83943_) {
      RenderSystem.assertOnRenderThread();
      this.destroyBuffers();
      this.createBuffers(p_83942_, p_83943_);
   }

   public void destroyBuffers() {
      RenderSystem.assertOnRenderThread();
      if (this.depthTexture != null) {
         this.depthTexture.close();
         this.depthTexture = null;
      }

      if (this.depthTextureView != null) {
         this.depthTextureView.close();
         this.depthTextureView = null;
      }

      if (this.colorTexture != null) {
         this.colorTexture.close();
         this.colorTexture = null;
      }

      if (this.colorTextureView != null) {
         this.colorTextureView.close();
         this.colorTextureView = null;
      }
   }

   public void copyDepthFrom(RenderTarget p_83946_) {
      RenderSystem.assertOnRenderThread();
      if (this.depthTexture == null) {
         throw new IllegalStateException("Trying to copy depth texture to a RenderTarget without a depth texture");
      }

      if (p_83946_.depthTexture == null) {
         throw new IllegalStateException("Trying to copy depth texture from a RenderTarget without a depth texture");
      }

      RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(p_83946_.depthTexture, this.depthTexture, 0, 0, 0, 0, 0, this.width, this.height);
   }

   public void createBuffers(int p_83951_, int p_83952_) {
      RenderSystem.assertOnRenderThread();
      GpuDevice gpudevice = RenderSystem.getDevice();
      int i = gpudevice.getMaxTextureSize();
      if (p_83951_ > 0 && p_83951_ <= i && p_83952_ > 0 && p_83952_ <= i) {
         this.width = p_83951_;
         this.height = p_83952_;
         if (this.useDepth) {
            this.depthTexture = gpudevice.createTexture(() -> this.label + " / Depth", 15, TextureFormat.DEPTH32, p_83951_, p_83952_, 1, 1);
            this.depthTextureView = gpudevice.createTextureView(this.depthTexture);
         }

         this.colorTexture = gpudevice.createTexture(() -> this.label + " / Color", 15, TextureFormat.RGBA8, p_83951_, p_83952_, 1, 1);
         this.colorTextureView = gpudevice.createTextureView(this.colorTexture);
      } else {
         throw new IllegalArgumentException("Window " + p_83951_ + "x" + p_83952_ + " size out of bounds (max. size: " + i + ")");
      }
   }

   public void blitToScreen() {
      if (this.colorTexture == null) {
         throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");
      }

      RenderSystem.getDevice().createCommandEncoder().presentTexture(this.colorTextureView);
   }

   public void blitAndBlendToTexture(GpuTextureView p_409912_) {
      RenderSystem.assertOnRenderThread();

      try (RenderPass renderpass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Blit render target", p_409912_, OptionalInt.empty())) {
         renderpass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.bindTexture("InSampler", this.colorTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         renderpass.draw(0, 3);
      }
   }

   public @Nullable GpuTexture getColorTexture() {
      return this.colorTexture;
   }

   public @Nullable GpuTextureView getColorTextureView() {
      return this.colorTextureView;
   }

   public @Nullable GpuTexture getDepthTexture() {
      return this.depthTexture;
   }

   public @Nullable GpuTextureView getDepthTextureView() {
      return this.depthTextureView;
   }
}
