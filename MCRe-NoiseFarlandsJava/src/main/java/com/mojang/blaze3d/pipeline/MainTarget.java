package com.mojang.blaze3d.pipeline;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MainTarget extends RenderTarget {
    public static final int DEFAULT_WIDTH = 854;
    public static final int DEFAULT_HEIGHT = 480;
    private static final MainTarget.Dimension DEFAULT_DIMENSIONS = new MainTarget.Dimension(854, 480);

    public MainTarget(final int desiredWidth, final int desiredHeight) {
        super("Main", true, GpuFormat.RGBA8_UNORM);
        this.createFrameBuffer(desiredWidth, desiredHeight);
    }

    private void createFrameBuffer(final int desiredWidth, final int desiredHeight) {
        MainTarget.Dimension allocatedDimensions = this.allocateAttachments(desiredWidth, desiredHeight);
        if (this.colorTexture != null && this.depthTexture != null) {
            this.width = allocatedDimensions.width;
            this.height = allocatedDimensions.height;
        } else {
            throw new IllegalStateException("Missing color and/or depth textures");
        }
    }

    private MainTarget.Dimension allocateAttachments(final int width, final int height) {
        RenderSystem.assertOnRenderThread();

        for (MainTarget.Dimension dimension : MainTarget.Dimension.listWithFallback(width, height)) {
            if (this.colorTexture != null) {
                this.colorTexture.close();
                this.colorTexture = null;
            }

            if (this.colorTextureView != null) {
                this.colorTextureView.close();
                this.colorTextureView = null;
            }

            if (this.depthTexture != null) {
                this.depthTexture.close();
                this.depthTexture = null;
            }

            if (this.depthTextureView != null) {
                this.depthTextureView.close();
                this.depthTextureView = null;
            }

            this.colorTexture = this.allocateColorAttachment(dimension);
            this.depthTexture = this.allocateDepthAttachment(dimension);
            if (this.colorTexture != null && this.depthTexture != null) {
                this.colorTextureView = RenderSystem.getDevice().createTextureView(this.colorTexture);
                this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
                return dimension;
            }
        }

        throw new RuntimeException(
            "Unrecoverable GL_OUT_OF_MEMORY ("
                + (this.colorTexture == null ? "missing color" : "have color")
                + ", "
                + (this.depthTexture == null ? "missing depth" : "have depth")
                + ")"
        );
    }

    private @Nullable GpuTexture allocateColorAttachment(final MainTarget.Dimension dimension) {
        try {
            return RenderSystem.getDevice().createTexture(() -> this.label + " / Color", 15, this.format, dimension.width, dimension.height, 1, 1);
        } catch (GpuOutOfMemoryException ignored) {
            return null;
        }
    }

    private @Nullable GpuTexture allocateDepthAttachment(final MainTarget.Dimension dimension) {
        try {
            return RenderSystem.getDevice().createTexture(() -> this.label + " / Depth", 15, GpuFormat.D32_FLOAT, dimension.width, dimension.height, 1, 1);
        } catch (GpuOutOfMemoryException ignored) {
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class Dimension {
        public final int width;
        public final int height;

        private Dimension(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        private static List<MainTarget.Dimension> listWithFallback(final int width, final int height) {
            RenderSystem.assertOnRenderThread();
            int maxTextureSize = RenderSystem.getDevice().getDeviceInfo().limits().maxTextureSize();
            return width > 0 && width <= maxTextureSize && height > 0 && height <= maxTextureSize
                ? ImmutableList.of(new MainTarget.Dimension(width, height), MainTarget.DEFAULT_DIMENSIONS)
                : ImmutableList.of(MainTarget.DEFAULT_DIMENSIONS);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            } else if (other != null && this.getClass() == other.getClass()) {
                MainTarget.Dimension that = (MainTarget.Dimension)other;
                return this.width == that.width && this.height == that.height;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.width, this.height);
        }

        @Override
        public String toString() {
            return this.width + "x" + this.height;
        }
    }
}