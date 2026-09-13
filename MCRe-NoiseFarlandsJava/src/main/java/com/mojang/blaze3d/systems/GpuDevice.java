package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GpuDevice {
    private final GpuDeviceBackend backend;
    private final Runnable criticalShaderLoader;
    private final @Nullable TracyGpuProfiler profiler;

    public GpuDevice(final GpuDeviceBackend backend, final Runnable criticalShaderLoader) {
        this.backend = backend;
        this.criticalShaderLoader = criticalShaderLoader;
        if (TracyClient.isAvailable()) {
            this.profiler = new TracyGpuProfiler(this);
        } else {
            this.profiler = null;
        }
    }

    public GpuSurface createSurface(final long windowHandle) {
        return new GpuSurface(this.backend.createSurface(windowHandle));
    }

    public CommandEncoder createCommandEncoder() {
        return new CommandEncoder(this.profiler, this.backend, this.backend.createCommandEncoder());
    }

    public GpuSampler createSampler(
        final AddressMode addressModeU,
        final AddressMode addressModeV,
        final FilterMode minFilter,
        final FilterMode magFilter,
        final int maxAnisotropy,
        final OptionalDouble maxLod
    ) {
        int maxSupportedAnisotropy = this.getDeviceInfo().limits().maxAnisotropy();
        if (maxAnisotropy >= 1 && maxAnisotropy <= maxSupportedAnisotropy) {
            return this.backend.createSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
        } else {
            throw new IllegalArgumentException("maxAnisotropy out of range; must be >= 1 and <= " + maxSupportedAnisotropy + ", but was " + maxAnisotropy);
        }
    }

    public GpuTexture createTexture(
        final @Nullable Supplier<String> label,
        final @GpuTexture.Usage int usage,
        final GpuFormat format,
        final int width,
        final int height,
        final int depthOrLayers,
        final int mipLevels
    ) {
        this.verifyTextureCreationArgs(usage, width, height, depthOrLayers, mipLevels);
        return this.backend.createTexture(label, usage, format, width, height, depthOrLayers, mipLevels);
    }

    public GpuTexture createTexture(
        final @Nullable String label,
        final @GpuTexture.Usage int usage,
        final GpuFormat format,
        final int width,
        final int height,
        final int depthOrLayers,
        final int mipLevels
    ) {
        this.verifyTextureCreationArgs(usage, width, height, depthOrLayers, mipLevels);
        return this.backend.createTexture(label, usage, format, width, height, depthOrLayers, mipLevels);
    }

    private void verifyTextureCreationArgs(final @GpuTexture.Usage int usage, final int width, final int height, final int depthOrLayers, final int mipLevels) {
        if (mipLevels < 1) {
            throw new IllegalArgumentException("mipLevels must be at least 1");
        }

        int maxDimension = Math.max(width, height);
        int maxMipSupported = Mth.log2(maxDimension) + 1;
        if (mipLevels > maxMipSupported) {
            throw new IllegalArgumentException(
                "mipLevels must be at most "
                    + maxMipSupported
                    + " for a texture of width "
                    + width
                    + " and height "
                    + height
                    + " (asked for "
                    + mipLevels
                    + " mipLevels)"
            );
        }

        if (depthOrLayers < 1) {
            throw new IllegalArgumentException("depthOrLayers must be at least 1");
        }

        boolean isCubemap = (usage & 16) != 0;
        if (isCubemap) {
            if (width != height) {
                throw new IllegalArgumentException("Cubemap compatible textures must be square, but size is " + width + "x" + height);
            }

            if (depthOrLayers % 6 != 0) {
                throw new IllegalArgumentException("Cubemap compatible textures must have a layer count with a multiple of 6, was " + depthOrLayers);
            }

            if (depthOrLayers > 6) {
                throw new UnsupportedOperationException("Array textures are not yet supported");
            }
        } else if (depthOrLayers > 1) {
            throw new UnsupportedOperationException("Array or 3D textures are not yet supported");
        }
    }

    public GpuTextureView createTextureView(final GpuTexture texture) {
        this.verifyTextureViewCreationArgs(texture, 0, texture.getMipLevels());
        return this.backend.createTextureView(texture, 0, texture.getMipLevels());
    }

    public GpuTextureView createTextureView(final GpuTexture texture, final int baseMipLevel, final int mipLevels) {
        this.verifyTextureViewCreationArgs(texture, baseMipLevel, mipLevels);
        return this.backend.createTextureView(texture, baseMipLevel, mipLevels);
    }

    private void verifyTextureViewCreationArgs(final GpuTexture texture, final int baseMipLevel, final int mipLevels) {
        if (texture.isClosed()) {
            throw new IllegalArgumentException("Can't create texture view with closed texture");
        } else if (baseMipLevel < 0 || baseMipLevel + mipLevels > texture.getMipLevels()) {
            throw new IllegalArgumentException(
                mipLevels
                    + " mip levels starting from "
                    + baseMipLevel
                    + " would be out of range for texture with only "
                    + texture.getMipLevels()
                    + " mip levels"
            );
        }
    }

    public GpuBuffer createBuffer(final @Nullable Supplier<String> label, final @GpuBuffer.Usage int usage, final long size) {
        if (size <= 0L) {
            throw new IllegalArgumentException("Buffer size must be greater than zero");
        } else {
            return this.backend.createBuffer(label, usage, size);
        }
    }

    public GpuBuffer createBuffer(final @Nullable Supplier<String> label, final @GpuBuffer.Usage int usage, final ByteBuffer data) {
        if (!data.hasRemaining()) {
            throw new IllegalArgumentException("Buffer source must not be empty");
        } else {
            return this.backend.createBuffer(label, usage, data);
        }
    }

    public List<String> getLastDebugMessages() {
        return this.backend.getLastDebugMessages();
    }

    public boolean isDebuggingEnabled() {
        return this.backend.isDebuggingEnabled();
    }

    public CompiledRenderPipeline precompilePipeline(final RenderPipeline pipeline) {
        return this.precompilePipeline(pipeline, null);
    }

    public CompiledRenderPipeline precompilePipeline(final RenderPipeline pipeline, final @Nullable ShaderSource shaderSource) {
        return this.backend.precompilePipeline(pipeline, shaderSource);
    }

    public void clearPipelineCache() {
        this.backend.clearPipelineCache();
    }

    public void loadCriticalShaders() {
        this.criticalShaderLoader.run();
    }

    public void close() {
        this.backend.close();
    }

    public GpuQueryPool createTimestampQueryPool(final int size) {
        return this.backend.createTimestampQueryPool(size);
    }

    protected long getTimestampNow() {
        return this.backend.getTimestampNow();
    }

    public DeviceInfo getDeviceInfo() {
        return this.backend.getDeviceInfo();
    }
}