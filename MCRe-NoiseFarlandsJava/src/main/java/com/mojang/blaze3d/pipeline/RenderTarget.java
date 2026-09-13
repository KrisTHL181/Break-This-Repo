package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
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
    protected final GpuFormat format;
    protected @Nullable GpuTexture colorTexture;
    protected @Nullable GpuTextureView colorTextureView;
    protected @Nullable GpuTexture depthTexture;
    protected @Nullable GpuTextureView depthTextureView;

    public RenderTarget(final @Nullable String label, final boolean useDepth, final GpuFormat format) {
        this.label = label == null ? "FBO " + UNNAMED_RENDER_TARGETS++ : label;
        this.useDepth = useDepth;
        this.format = format;
    }

    public void resize(final int width, final int height) {
        RenderSystem.assertOnRenderThread();
        this.destroyBuffers();
        this.createBuffers(width, height);
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

    public void copyDepthFrom(final RenderTarget source) {
        RenderSystem.assertOnRenderThread();
        if (this.depthTexture == null) {
            throw new IllegalStateException("Trying to copy depth texture to a RenderTarget without a depth texture");
        }

        if (source.depthTexture == null) {
            throw new IllegalStateException("Trying to copy depth texture from a RenderTarget without a depth texture");
        }

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(source.depthTexture, this.depthTexture, 0, 0, 0, 0, 0, this.width, this.height);
    }

    public void createBuffers(final int width, final int height) {
        RenderSystem.assertOnRenderThread();
        GpuDevice device = RenderSystem.getDevice();
        int maxTextureSize = device.getDeviceInfo().limits().maxTextureSize();
        if (width > 0 && width <= maxTextureSize && height > 0 && height <= maxTextureSize) {
            this.width = width;
            this.height = height;
            if (this.useDepth) {
                this.depthTexture = device.createTexture(() -> this.label + " / Depth", 15, GpuFormat.D32_FLOAT, width, height, 1, 1);
                this.depthTextureView = device.createTextureView(this.depthTexture);
            }

            this.colorTexture = device.createTexture(() -> this.label + " / Color", 15, this.format, width, height, 1, 1);
            this.colorTextureView = device.createTextureView(this.colorTexture);
        } else {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + maxTextureSize + ")");
        }
    }

    public void blitAndBlendToTexture(final GpuTextureView output, final GpuTextureView outputDepth) {
        RenderSystem.assertOnRenderThread();

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Blit render target", output, Optional.empty(), outputDepth, OptionalDouble.empty())) {
            renderPass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("InSampler", this.colorTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass.draw(3, 1, 0, 0);
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