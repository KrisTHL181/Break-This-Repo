package com.mojang.blaze3d.resource;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import java.util.Objects;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

public record RenderTargetDescriptor(
   int width, int height, RenderTargetDescriptor.@Nullable TextureProperties color, RenderTargetDescriptor.@Nullable TextureProperties depth
) implements ResourceDescriptor<RenderTarget> {
   public RenderTarget allocate() {
      return new TextureTarget(null, this.width, this.height, this.color != null ? this.color.format : null, this.depth != null ? this.depth.format : null);
   }

   public void prepare(final RenderTarget resource) {
      if (this.color != null && this.depth != null) {
         RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(resource.getColorTexture(), this.color.clearColor, resource.getDepthTexture(), this.depth.clearColor.x());
      } else if (this.color != null) {
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(resource.getColorTexture(), this.color.clearColor);
      } else if (this.depth != null) {
         RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(resource.getDepthTexture(), this.depth.clearColor.x());
      }
   }

   public void free(final RenderTarget resource) {
      resource.destroyBuffers();
   }

   @Override
   public boolean canUsePhysicalResource(final ResourceDescriptor<?> other) {
      return !(other instanceof RenderTargetDescriptor descriptor)
         ? false
         : this.width == descriptor.width
            && this.height == descriptor.height
            && Objects.equals(this.color, descriptor.color)
            && Objects.equals(this.depth, descriptor.depth);
   }

   public record TextureProperties(Vector4fc clearColor, GpuFormat format) {
      public static final RenderTargetDescriptor.TextureProperties DEFAULT_DEPTH = new RenderTargetDescriptor.TextureProperties(
         new Vector4f(0.0F, 0.0F, 0.0F, 0.0F), GpuFormat.D32_FLOAT
      );
   }
}
