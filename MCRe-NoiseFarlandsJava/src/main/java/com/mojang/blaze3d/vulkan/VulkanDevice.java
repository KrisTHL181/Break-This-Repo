package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.DeviceFeatures;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.DeviceLimits;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.systems.HintsAndWorkarounds;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan11Properties;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class VulkanDevice implements GpuDeviceBackend {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ShaderSource defaultShaderSource;
    private final Map<RenderPipeline, VulkanRenderPipeline> pipelineCache = new IdentityHashMap<>();
    private final Map<VulkanDevice.ShaderCompilationKey, IntermediaryShaderModule> shaderCache = new HashMap<>();
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
        final ShaderSource defaultShaderSource,
        final VulkanInstance instance,
        final VulkanPhysicalDevice physicalDevice,
        final Set<String> enabledDeviceExtensions,
        final VkDevice vkDevice,
        final long vma,
        final CheckpointExtension checkpointExtension
    ) {
        this.defaultShaderSource = defaultShaderSource;
        this.instance = instance;
        this.vkDevice = vkDevice;
        this.vma = vma;
        this.checkpointExtension = checkpointExtension;
        Set<String> extensionNames = new HashSet<>();

        for (String name : instance.getEnabledExtensions()) {
            extensionNames.add(name + " (I)");
        }

        for (String name : enabledDeviceExtensions) {
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
            new DeviceFeatures(true, enabledDeviceExtensions.contains("VK_EXT_multi_draw"), false, true, true, true, true),
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
        this.clearPipelineCache();
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

    @Override
    public CompiledRenderPipeline precompilePipeline(final RenderPipeline pipeline, final @Nullable ShaderSource customShaderSource) {
        ShaderSource shaderSource = customShaderSource == null ? this.defaultShaderSource : customShaderSource;
        return this.pipelineCache.computeIfAbsent(pipeline, ignored -> this.compilePipeline(pipeline, shaderSource));
    }

    protected VulkanRenderPipeline getOrCompilePipeline(final RenderPipeline pipeline) {
        return this.pipelineCache.computeIfAbsent(pipeline, ignored -> this.compilePipeline(pipeline, this.defaultShaderSource));
    }

    protected IntermediaryShaderModule getOrCompileShader(
        final Identifier id, final ShaderType type, final ShaderDefines defines, final ShaderSource shaderSource
    ) {
        VulkanDevice.ShaderCompilationKey key = new VulkanDevice.ShaderCompilationKey(id, type, defines);
        return this.shaderCache.computeIfAbsent(key, ignored -> this.compileShader(key, shaderSource));
    }

    private IntermediaryShaderModule compileShader(final VulkanDevice.ShaderCompilationKey key, final ShaderSource shaderSource) {
        String source = shaderSource.get(key.id, key.type);
        if (source == null) {
            LOGGER.error("Couldn't find source for {} shader ({})", key.type, key.id);
            return IntermediaryShaderModule.INVALID;
        }

        String sourceWithDefines = GlslPreprocessor.injectDefines(source, key.defines);

        try {
            return this.glslCompiler.createIntermediary(key.id.toDebugFileName(), sourceWithDefines, key.type);
        } catch (ShaderCompileException e) {
            LOGGER.error("Couldn't compile {} shader {}: {}", key.type, key.id, e.getMessage());
            return IntermediaryShaderModule.INVALID;
        }
    }

    private VulkanRenderPipeline compilePipeline(final RenderPipeline pipeline, final ShaderSource shaderSource) {
        IntermediaryShaderModule vertexShader = this.getOrCompileShader(
            pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), shaderSource
        );
        IntermediaryShaderModule fragmentShader = this.getOrCompileShader(
            pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), shaderSource
        );
        if (vertexShader == IntermediaryShaderModule.INVALID) {
            LOGGER.error("Couldn't compile pipeline {}: vertex shader {} was invalid", pipeline.getLocation(), pipeline.getVertexShader());
            return new VulkanRenderPipeline(pipeline, this, 0L, 0L, 0L, VulkanBindGroupLayout.INVALID_LAYOUT, 0L, 0L);
        }

        if (fragmentShader == IntermediaryShaderModule.INVALID) {
            LOGGER.error("Couldn't compile pipeline {}: fragment shader {} was invalid", pipeline.getLocation(), pipeline.getFragmentShader());
            return new VulkanRenderPipeline(pipeline, this, 0L, 0L, 0L, VulkanBindGroupLayout.INVALID_LAYOUT, 0L, 0L);
        }

        try {
            GlslCompiler.CompiledModules modules = this.glslCompiler.compile(this, pipeline, vertexShader, fragmentShader);
            return VulkanRenderPipeline.compile(this, modules.layout(), pipeline, modules.vertex(), modules.fragment());
        } catch (ShaderCompileException e) {
            LOGGER.error("Couldn't compile pipeline {}: {}", pipeline.getLocation(), e.getMessage());
            return new VulkanRenderPipeline(pipeline, this, 0L, 0L, 0L, VulkanBindGroupLayout.INVALID_LAYOUT, 0L, 0L);
        }
    }

    @Override
    public void clearPipelineCache() {
        this.graphicsQueue.waitIdle();
        this.pipelineCache.values().forEach(VulkanRenderPipeline::destroy);
        this.pipelineCache.clear();
        this.shaderCache.values().forEach(IntermediaryShaderModule::close);
        this.shaderCache.clear();
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

    @OnlyIn(Dist.CLIENT)
    private record ShaderCompilationKey(Identifier id, ShaderType type, ShaderDefines defines) {
        @Override
        public String toString() {
            String string = this.id + " (" + this.type + ")";
            return !this.defines.isEmpty() ? string + " with " + this.defines : string;
        }
    }
}