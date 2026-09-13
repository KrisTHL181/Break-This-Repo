package com.mojang.renderpearl.backend.api;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public interface GpuDeviceBackend {
   GpuSurfaceBackend createSurface(long windowHandle);

   CommandEncoderBackend createCommandEncoder();

   GpuSampler createSampler(
      AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod
   );

   GpuTexture createTexture(
      @Nullable Supplier<String> label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels
   );

   GpuTexture createTexture(@Nullable String label, @GpuTexture.Usage int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels);

   GpuTextureView createTextureView(GpuTexture texture);

   GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels);

   GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size);

   GpuBuffer createBuffer(@Nullable Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data);

   List<String> getLastDebugMessages();

   boolean isDebuggingEnabled();

   @Nullable CompiledRenderPipeline compilePipeline(RenderPipeline pipeline, ShaderSource shaderSource);

   void close();

   GpuQueryPool createTimestampQueryPool(int size);

   long getTimestampNow();

   DeviceInfo getDeviceInfo();
}
