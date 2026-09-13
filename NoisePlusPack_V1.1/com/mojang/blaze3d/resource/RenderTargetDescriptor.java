package com.mojang.blaze3d.resource;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RenderTargetDescriptor(int width, int height, boolean useDepth, int clearColor) implements ResourceDescriptor<RenderTarget> {
   public RenderTarget allocate() {
      return new TextureTarget(null, this.width, this.height, this.useDepth);
   }

   public void prepare(RenderTarget p_393121_) {
      if (this.useDepth) {
         RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(p_393121_.getColorTexture(), this.clearColor, p_393121_.getDepthTexture(), 1.0);
      } else {
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(p_393121_.getColorTexture(), this.clearColor);
      }
   }

   public void free(RenderTarget p_362881_) {
      p_362881_.destroyBuffers();
   }

   @Override
   public boolean canUsePhysicalResource(ResourceDescriptor<?> p_392735_) {
      return p_392735_ instanceof RenderTargetDescriptor rendertargetdescriptor
         ? this.width == rendertargetdescriptor.width && this.height == rendertargetdescriptor.height && this.useDepth == rendertargetdescriptor.useDepth
         : false;
   }
}
