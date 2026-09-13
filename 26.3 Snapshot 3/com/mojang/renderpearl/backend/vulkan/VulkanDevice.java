package com.mojang.renderpearl.backend.vulkan;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.device.DeviceFeatures;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.DeviceLimits;
import com.mojang.renderpearl.api.device.HintsAndWorkarounds;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;
import com.mojang.renderpearl.backend.vulkan.checkpoints.CheckpointExtension;
import com.mojang.renderpearl.backend.vulkan.glsl.GlslCompiler;
import com.mojang.renderpearl.backend.vulkan.glsl.IntermediaryShaderModule;
import com.mojang.renderpearl.backend.vulkan.glsl.ShaderCompileException;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan11Properties;
import org.slf4j.Logger;

public class VulkanDevice implements GpuDeviceBackend {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VulkanInstance instance;
   private final VkDevice vkDevice;
   private final long vma;
   private final GlslCompiler glslCompiler = new GlslCompiler();
   private final DeviceInfo deviceInfo;
   private final VulkanQueue graphicsQueue;
   private final VulkanQueue computeQueue;
   private final VulkanQueue transferQueue;
   private final boolean isIntegratedIntelMoltenVK;
   private final VulkanCommandEncoder commandEncoder;
   private final CheckpointExtension checkpointExtension;

   public VulkanDevice(
      final VulkanInstance instance,
      final VulkanPhysicalDevice physicalDevice,
      final FeatureSet enabledFeatureSet,
      final VkDevice vkDevice,
      final long vma,
      final CheckpointExtension checkpointExtension
   ) {
      this.instance = instance;
      this.vkDevice = vkDevice;
      this.vma = vma;
      this.checkpointExtension = checkpointExtension;
      Set<String> extensionNames = new HashSet<>();

      for (String name : instance.getEnabledExtensions()) {
         extensionNames.add(name + " (I)");
      }

      for (String name : enabledFeatureSet.extensions()) {
         extensionNames.add(name + " (D)");
      }

      VkPhysicalDeviceLimits limits = physicalDevice.vkPhysicalDeviceProperties().limits();
      VkPhysicalDeviceVulkan11Properties vk11Properties = physicalDevice.vkPhysicalDeviceVulkan11Properties();
      this.deviceInfo = new DeviceInfo(
         physicalDevice.deviceName(),
         physicalDevice.vendorName(),
         physicalDevice.driverInfo(),
         true,
         "Vulkan",
         limits.timestampPeriod(),
         new DeviceLimits(
            (int)limits.maxSamplerAnisotropy(),
            (int)limits.minUniformBufferOffsetAlignment(),
            limits.maxImageDimension2D(),
            vk11Properties.maxMemoryAllocationSize() < 0L ? Long.MAX_VALUE : vk11Properties.maxMemoryAllocationSize(),
            physicalDevice.vkPhysicalDeviceMultiDrawPropertiesEXT().maxMultiDrawCount() < 0
               ? Integer.MAX_VALUE
               : physicalDevice.vkPhysicalDeviceMultiDrawPropertiesEXT().maxMultiDrawCount(),
            limits.maxColorAttachments()
         ),
         new DeviceFeatures(true, enabledFeatureSet.contains(VulkanFeatureSets.MULTI_DRAW_FEATURESET), false, true, true, true, true),
         Collections.unmodifiableSet(extensionNames),
         new HintsAndWorkarounds(false, false),
         physicalDevice.deviceType()
      );
      IntIntPair graphicsQueueFamily = physicalDevice.graphicsQueueFamilyAndIndex();
      assert graphicsQueueFamily != null;
      IntIntPair computeQueueFamily = physicalDevice.computeQueueFamilyAndIndex();
      IntIntPair transferQueueFamily = physicalDevice.transferQueueFamilyAndIndex();
      this.graphicsQueue = new VulkanQueue(this, graphicsQueueFamily.leftInt(), graphicsQueueFamily.rightInt());
      if (computeQueueFamily != null) {
         this.computeQueue = new VulkanQueue(this, computeQueueFamily.leftInt(), computeQueueFamily.rightInt());
      } else {
         this.computeQueue = this.graphicsQueue;
      }

      if (transferQueueFamily != null) {
         this.transferQueue = new VulkanQueue(this, transferQueueFamily.leftInt(), transferQueueFamily.rightInt());
      } else {
         this.transferQueue = this.computeQueue;
      }

      this.isIntegratedIntelMoltenVK = physicalDevice.vkPhysicalDeviceProperties().deviceType() == 1
         && physicalDevice.vkPhysicalDeviceProperties().vendorID() == 32902
         && physicalDevice.vkPhysicalDeviceDriverProperties().driverID() == 14;
      physicalDevice.close();
      this.commandEncoder = new VulkanCommandEncoder(this);
   }

   @Override
   public void close() {
      this.checkpointExtension.close();
      this.commandEncoder.destroy();
      Vma.vmaDestroyAllocator(this.vma);
      VK12.vkDestroyDevice(this.vkDevice, null);
      this.instance.close();
      this.glslCompiler.close();
   }

   @Override
   public DeviceInfo getDeviceInfo() {
      return this.deviceInfo;
   }

   public VulkanInstance instance() {
      return this.instance;
   }

   public VkDevice vkDevice() {
      return this.vkDevice;
   }

   public VulkanQueue graphicsQueue() {
      return this.graphicsQueue;
   }

   public VulkanQueue computeQueue() {
      return this.computeQueue;
   }

   public VulkanQueue transferQueue() {
      return this.transferQueue;
   }

   public long vma() {
      return this.vma;
   }

   @Override
   public GpuSurfaceBackend createSurface(final long windowHandle) {
      return new VulkanGpuSurface(this, windowHandle);
   }

   public VulkanCommandEncoder createCommandEncoder() {
      return this.commandEncoder;
   }

   @Override
   public GpuSampler createSampler(
      final AddressMode addressModeU,
      final AddressMode addressModeV,
      final FilterMode minFilter,
      final FilterMode magFilter,
      final int maxAnisotropy,
      final OptionalDouble maxLod
   ) {
      return new VulkanGpuSampler(this, addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
   }

   @Override
   public GpuTexture createTexture(
      final @Nullable Supplier<String> label,
      final @GpuTexture.Usage int usage,
      final GpuFormat format,
      final int width,
      final int height,
      final int depthOrLayers,
      final int mipLevels
   ) {
      return new VulkanGpuTexture(this, usage, this.isDebuggingEnabled() && label != null ? label.get() : "", format, width, height, depthOrLayers, mipLevels);
   }

   @Override
   public GpuTexture createTexture(
      final @Nullable String label,
      final @GpuTexture.Usage int usage,
      final GpuFormat format,
      final int width,
      final int height,
      final int depthOrLayers,
      final int mipLevels
   ) {
      return new VulkanGpuTexture(this, usage, this.isDebuggingEnabled() && label != null ? label : "", format, width, height, depthOrLayers, mipLevels);
   }

   @Override
   public GpuTextureView createTextureView(final GpuTexture texture) {
      return this.createTextureView(texture, 0, texture.getMipLevels());
   }

   @Override
   public GpuTextureView createTextureView(final GpuTexture texture, final int baseMipLevel, final int mipLevels) {
      return new VulkanGpuTextureView(this, (VulkanGpuTexture)texture, baseMipLevel, mipLevels);
   }

   public VulkanGpuBuffer createBuffer(final @Nullable Supplier<String> label, final @GpuBuffer.Usage int usage, final long size) {
      return new VulkanGpuBuffer.Direct(this, label, usage, size, this.isIntegratedIntelMoltenVK);
   }

   @Override
   public GpuBuffer createBuffer(final @Nullable Supplier<String> label, final @GpuBuffer.Usage int usage, final ByteBuffer data) {
      GpuBuffer buffer = this.createBuffer(label, usage | 8, data.remaining());
      this.createCommandEncoder().writeToBuffer(buffer.slice(), data);
      return buffer;
   }

   @Override
   public List<String> getLastDebugMessages() {
      return List.of();
   }

   @Override
   public boolean isDebuggingEnabled() {
      return this.instance.debug().enabled();
   }

   protected @Nullable IntermediaryShaderModule compileShader(
      final Identifier id, final ShaderType type, final ShaderDefines defines, final ShaderSource shaderSource
   ) {
      String source = shaderSource.get(id, type);
      if (source == null) {
         LOGGER.error("Couldn't find source for {} shader ({})", type, id);
         return null;
      }

      String sourceWithDefines = GlslPreprocessor.injectDefines(source, defines);

      try {
         return this.glslCompiler.createIntermediary(id.toDebugFileName(), sourceWithDefines, type);
      } catch (ShaderCompileException e) {
         LOGGER.error("Couldn't compile {} shader {}", new Object[]{type, id, e});
         return null;
      }
   }

   @Override
   public @Nullable CompiledRenderPipeline compilePipeline(final RenderPipeline pipeline, final ShaderSource shaderSource) {
      IntermediaryShaderModule vertexShader = this.compileShader(pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), shaderSource);
      if (vertexShader == null) {
         LOGGER.error("Couldn't compile pipeline {}: vertex shader {} was invalid", pipeline.getLocation(), pipeline.getVertexShader());
         return null;
      }

      IntermediaryShaderModule fragmentShader = this.compileShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), shaderSource);
      if (fragmentShader == null) {
         LOGGER.error("Couldn't compile pipeline {}: fragment shader {} was invalid", pipeline.getLocation(), pipeline.getFragmentShader());
         vertexShader.close();
         return null;
      }

      try {
         GlslCompiler.CompiledModules modules = this.glslCompiler.compile(this, pipeline, vertexShader, fragmentShader);
         return VulkanRenderPipeline.compile(this, modules.layout(), pipeline, modules.vertex(), modules.fragment());
      } catch (ShaderCompileException e) {
         LOGGER.error("Couldn't compile pipeline {}", pipeline.getLocation(), e);
         vertexShader.close();
         fragmentShader.close();
         return null;
      }
   }

   @Override
   public GpuQueryPool createTimestampQueryPool(final int size) {
      return new VulkanQueryPool(this, size);
   }

   @Override
   public long getTimestampNow() {
      return this.commandEncoder.getTimestampNow();
   }

   public CheckpointExtension checkpointExtension() {
      return this.checkpointExtension;
   }
}
