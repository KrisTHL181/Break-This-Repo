package com.mojang.blaze3d.resource;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector4fc;

@OnlyIn(Dist.CLIENT)
public record RenderTargetDescriptor(int width, int height, boolean useDepth, Vector4fc clearColor, GpuFormat format)
    implements ResourceDescriptor<RenderTarget> {
    public RenderTarget allocate() {
        return new TextureTarget(null, this.width, this.height, this.useDepth, this.format);
    }

    public void prepare(final RenderTarget resource) {
        if (this.useDepth) {
            RenderSystem.getDevice()
                .createCommandEncoder()
                .clearColorAndDepthTextures(resource.getColorTexture(), this.clearColor, resource.getDepthTexture(), 0.0);
        } else {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(resource.getColorTexture(), this.clearColor);
        }
    }

    public void free(final RenderTarget resource) {
        resource.destroyBuffers();
    }

    @Override
    public boolean canUsePhysicalResource(final ResourceDescriptor<?> other) {
        return !(other instanceof RenderTargetDescriptor descriptor)
            ? false
            : this.width == descriptor.width && this.height == descriptor.height && this.useDepth == descriptor.useDepth && this.format == descriptor.format;
    }
}