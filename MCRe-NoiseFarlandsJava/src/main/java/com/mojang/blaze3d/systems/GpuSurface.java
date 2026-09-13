package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Collection;
import java.util.Optional;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GpuSurface implements AutoCloseable {
    private final GpuSurfaceBackend backend;
    private boolean hasImageAcquired = false;
    private boolean hasBlittedTexture = false;
    private Optional<GpuSurface.Configuration> currentConfiguration = Optional.empty();

    public GpuSurface(final GpuSurfaceBackend backend) {
        this.backend = backend;
    }

    @Override
    public void close() {
        if (this.hasImageAcquired) {
            throw new IllegalStateException("Cannot close a surface while it is acquired");
        }

        this.backend.close();
    }

    public void configure(final GpuSurface.Configuration config) throws SurfaceException {
        if (this.hasImageAcquired) {
            throw new IllegalStateException("Cannot configure a surface while it is acquired");
        }

        if (!this.supportedPresentModes().contains(config.presentMode())) {
            throw new SurfaceException("Surface does not support present mode " + config.presentMode() + " (supported: " + this.supportedPresentModes() + ")");
        }

        this.backend.configure(config);
        this.currentConfiguration = Optional.of(config);
    }

    public Optional<GpuSurface.Configuration> currentConfiguration() {
        return this.currentConfiguration;
    }

    public Collection<GpuSurface.PresentMode> supportedPresentModes() {
        return this.backend.supportedPresentModes();
    }

    public boolean isSuboptimal() {
        return this.backend.isSuboptimal();
    }

    public boolean isAcquired() {
        return this.hasImageAcquired;
    }

    public void acquireNextTexture() throws SurfaceException {
        if (this.hasImageAcquired) {
            throw new IllegalStateException("Cannot acquire a surface while it is already acquired");
        }

        if (this.currentConfiguration.isEmpty()) {
            throw new IllegalStateException("Cannot acquire an unconfigured surface");
        }

        this.backend.acquireNextTexture();
        this.hasImageAcquired = true;
        this.hasBlittedTexture = false;
    }

    public void blitFromTexture(final CommandEncoder commandEncoder, final GpuTextureView textureView) {
        if (commandEncoder.isInRenderPass()) {
            throw new IllegalStateException("Close the existing render pass before presenting with a command encoder");
        }

        if (!textureView.texture().getFormat().hasColorAspect()) {
            throw new IllegalStateException("Cannot present a non-color texture!");
        }

        if ((textureView.texture().usage() & 2) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_SRC to presented to the screen");
        }

        if (textureView.texture().getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for presentation");
        }

        if (!this.hasImageAcquired) {
            throw new IllegalStateException("Cannot present to an unacquired surface");
        }

        if (this.hasBlittedTexture) {
            throw new IllegalStateException("Already blitted to this frame!");
        }

        this.backend.blitFromTexture(commandEncoder.backend(), textureView);
        this.hasBlittedTexture = true;
    }

    public void present() {
        if (!this.hasImageAcquired) {
            throw new IllegalStateException("Cannot present to a surface if it isn't acquired");
        }

        if (!this.hasBlittedTexture) {
            throw new IllegalStateException("Must blit to surface before presenting!");
        }

        this.backend.present();
        this.hasImageAcquired = false;
    }

    @OnlyIn(Dist.CLIENT)
    public record Configuration(int width, int height, GpuSurface.PresentMode presentMode) {
    }

    @OnlyIn(Dist.CLIENT)
    public enum PresentMode {
        IMMEDIATE,
        MAILBOX,
        FIFO,
        FIFO_RELAXED;

        private static final GpuSurface.PresentMode[] PRESENT_MODES_VSYNC = new GpuSurface.PresentMode[]{FIFO_RELAXED, FIFO};
        private static final GpuSurface.PresentMode[] PRESENT_MODES_NO_VSYNC = new GpuSurface.PresentMode[]{IMMEDIATE, MAILBOX, FIFO};

        public static GpuSurface.PresentMode getSupportedVsyncMode(final Collection<GpuSurface.PresentMode> supportedModes, final boolean vsync) {
            GpuSurface.PresentMode[] preferred = vsync ? PRESENT_MODES_VSYNC : PRESENT_MODES_NO_VSYNC;

            for (GpuSurface.PresentMode mode : preferred) {
                if (supportedModes.contains(mode)) {
                    return mode;
                }
            }

            throw new IllegalStateException("No supported presentation mode was found");
        }
    }
}