package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.platform.NativeLibrariesBootstrap;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.vulkan.checkpoints.AmdCheckpointExtension;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import com.mojang.blaze3d.vulkan.checkpoints.NoopCheckpointExtension;
import com.mojang.blaze3d.vulkan.checkpoints.NvidiaCheckpointExtension;
import com.mojang.blaze3d.vulkan.init.VulkanFeature;
import com.mojang.blaze3d.vulkan.init.VulkanPNextStruct;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceDynamicRenderingFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceMultiDrawFeaturesEXT;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties2;
import org.lwjgl.vulkan.VkPhysicalDeviceSynchronization2Features;
import org.lwjgl.vulkan.VkPhysicalDeviceVertexAttributeDivisorFeaturesEXT;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan11Features;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan12Features;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo.Buffer;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class VulkanBackend implements GpuBackend {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Set<String> REQUIRED_DEVICE_EXTENSIONS = Set.of(
        "VK_KHR_dynamic_rendering", "VK_KHR_push_descriptor", "VK_KHR_synchronization2", "VK_EXT_vertex_attribute_divisor", "VK_KHR_swapchain"
    );
    public static final VulkanPNextStruct VK10_FEATURES_STRUCT = new VulkanPNextStruct(1000059000, VkPhysicalDeviceProperties2.SIZEOF);
    public static final VulkanPNextStruct VK11_FEATURES_STRUCT = new VulkanPNextStruct(49, VkPhysicalDeviceVulkan11Features.SIZEOF);
    public static final VulkanPNextStruct VK12_FEATURES_STRUCT = new VulkanPNextStruct(51, VkPhysicalDeviceVulkan12Features.SIZEOF);
    public static final VulkanPNextStruct SYNC2_FEATURES_STRUCT = new VulkanPNextStruct(1000314007, VkPhysicalDeviceSynchronization2Features.SIZEOF);
    public static final VulkanPNextStruct DYNAMIC_RENDERING_FEATURES_STRUCT = new VulkanPNextStruct(1000044003, VkPhysicalDeviceDynamicRenderingFeatures.SIZEOF);
    public static final VulkanPNextStruct VERTEX_ATTRIB_DIVISOR_FEATURES_STRUCT = new VulkanPNextStruct(
        1000190002, VkPhysicalDeviceVertexAttributeDivisorFeaturesEXT.SIZEOF
    );
    public static final VulkanPNextStruct MULTI_DRAW_FEATURES_STRUCT = new VulkanPNextStruct(1000392000, VkPhysicalDeviceMultiDrawFeaturesEXT.SIZEOF);
    public static final Set<VulkanFeature> REQUIRED_DEVICE_FEATURES = Set.of(
        new VulkanFeature(VK10_FEATURES_STRUCT, "multiDrawIndirect", VkPhysicalDeviceFeatures.MULTIDRAWINDIRECT),
        new VulkanFeature(VK10_FEATURES_STRUCT, "fillModeNonSolid", VkPhysicalDeviceFeatures.FILLMODENONSOLID),
        new VulkanFeature(VK10_FEATURES_STRUCT, "samplerAnisotropy", VkPhysicalDeviceFeatures.SAMPLERANISOTROPY),
        new VulkanFeature(VK11_FEATURES_STRUCT, "shaderDrawParameters", VkPhysicalDeviceVulkan11Features.SHADERDRAWPARAMETERS),
        new VulkanFeature(VK12_FEATURES_STRUCT, "timelineSemaphore", VkPhysicalDeviceVulkan12Features.TIMELINESEMAPHORE),
        new VulkanFeature(VK12_FEATURES_STRUCT, "hostQueryReset", VkPhysicalDeviceVulkan12Features.HOSTQUERYRESET),
        new VulkanFeature(SYNC2_FEATURES_STRUCT, "synchronization2", VkPhysicalDeviceSynchronization2Features.SYNCHRONIZATION2),
        new VulkanFeature(DYNAMIC_RENDERING_FEATURES_STRUCT, "dynamicRendering", VkPhysicalDeviceDynamicRenderingFeatures.DYNAMICRENDERING),
        new VulkanFeature(
            VERTEX_ATTRIB_DIVISOR_FEATURES_STRUCT,
            "vertexAttributeInstanceRateDivisor",
            VkPhysicalDeviceVertexAttributeDivisorFeaturesEXT.VERTEXATTRIBUTEINSTANCERATEDIVISOR
        )
    );
    private static final VulkanFeature MULTI_DRAW_FEATURE = new VulkanFeature(
        MULTI_DRAW_FEATURES_STRUCT, "multiDraw", VkPhysicalDeviceMultiDrawFeaturesEXT.MULTIDRAW
    );

    @Override
    public String getName() {
        return "Vulkan";
    }

    @Override
    public void setWindowHints() {
        GLFW.glfwWindowHint(139265, 0);
    }

    public static @Nullable BackendCreationException checkBackendAvailable() {
        if (!NativeLibrariesBootstrap.isVulkanLoaderAvailable()) {
            return new BackendCreationException("Vulkan loader library is missing", BackendCreationException.Reason.VULKAN_LOADER_MISSING);
        }

        if (!GLFWVulkan.glfwVulkanSupported()) {
            return new BackendCreationException("Vulkan is not supported", BackendCreationException.Reason.GLFW_ERROR);
        }

        try (
            VulkanInstance instance = new VulkanInstance(0, false, false);
            VulkanPhysicalDevice physicalDevice = findPhysicalDevice(instance);
        ) {
            return null;
        } catch (BackendCreationException e) {
            return e;
        }
    }

    @Override
    public void handleWindowCreationErrors(final GLFWErrorCapture.@Nullable Error error) throws BackendCreationException {
        if (error != null) {
            throw new BackendCreationException(String.format(Locale.ROOT, "GLFW_ERROR: 0x%X", error.error()), BackendCreationException.Reason.GLFW_ERROR);
        } else {
            throw new BackendCreationException("Failed to create window for Vulkan", BackendCreationException.Reason.GLFW_ERROR);
        }
    }

    @Override
    public GpuDevice createDevice(
        final long window, final ShaderSource defaultShaderSource, final GpuDebugOptions debugOptions, final Runnable criticalShaderLoader
    ) throws BackendCreationException {
        if (!NativeLibrariesBootstrap.isVulkanLoaderAvailable()) {
            throw new BackendCreationException("Vulkan loader library is missing", BackendCreationException.Reason.VULKAN_LOADER_MISSING);
        }

        if (!GLFWVulkan.glfwVulkanSupported()) {
            throw new BackendCreationException("Vulkan is not supported", BackendCreationException.Reason.GLFW_ERROR);
        }

        Set<String> deviceExtensions = new HashSet<>(REQUIRED_DEVICE_EXTENSIONS);
        VulkanInstance instance = null;
        VulkanPhysicalDevice physicalDevice = null;
        VkDevice device = null;
        long vma = 0L;
        CheckpointExtension checkpointExtension = NoopCheckpointExtension.INSTANCE;

        try {
            boolean renderdocAttached = "1".equals(System.getenv("ENABLE_VULKAN_RENDERDOC_CAPTURE"));
            instance = new VulkanInstance(debugOptions.logLevel(), debugOptions.useLabels() || renderdocAttached, debugOptions.useValidationLayers());
            physicalDevice = findPhysicalDevice(instance);
            Set<VulkanFeature> enabledFeatures = new ObjectOpenHashSet<>(REQUIRED_DEVICE_FEATURES);
            if (physicalDevice.hasDeviceExtension("VK_KHR_portability_subset")) {
                deviceExtensions.add("VK_KHR_portability_subset");
            }

            if (physicalDevice.hasDeviceExtension("VK_AMD_buffer_marker")) {
                deviceExtensions.add("VK_AMD_buffer_marker");
                checkpointExtension = new AmdCheckpointExtension();
            } else if (physicalDevice.hasDeviceExtension("VK_NV_device_diagnostic_checkpoints")) {
                deviceExtensions.add("VK_NV_device_diagnostic_checkpoints");
                checkpointExtension = new NvidiaCheckpointExtension();
            }

            if (physicalDevice.hasDeviceExtension("VK_EXT_multi_draw") && isFeatureSupported(physicalDevice.vkPhysicalDevice(), MULTI_DRAW_FEATURE)) {
                deviceExtensions.add("VK_EXT_multi_draw");
                enabledFeatures.add(MULTI_DRAW_FEATURE);
            }

            device = createDevice(deviceExtensions, physicalDevice, enabledFeatures);
            vma = createVma(device);
        } catch (BackendCreationException e) {
            if (vma != 0L) {
                Vma.vmaDestroyAllocator(vma);
            }

            if (device != null) {
                VK12.vkDestroyDevice(device, null);
            }

            if (physicalDevice != null) {
                physicalDevice.close();
            }

            if (instance != null) {
                instance.close();
            }

            throw e;
        }

        return new GpuDevice(
            new VulkanDevice(defaultShaderSource, instance, physicalDevice, deviceExtensions, device, vma, checkpointExtension), criticalShaderLoader
        );
    }

    private static long createVma(final VkDevice vkDevice) throws BackendCreationException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VmaVulkanFunctions vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack).set(vkDevice.getPhysicalDevice().getInstance(), vkDevice);
            VmaAllocatorCreateInfo createInfo = VmaAllocatorCreateInfo.calloc(stack)
                .instance(vkDevice.getPhysicalDevice().getInstance())
                .vulkanApiVersion(VK12.VK_API_VERSION_1_2)
                .device(vkDevice)
                .physicalDevice(vkDevice.getPhysicalDevice())
                .pVulkanFunctions(vmaVulkanFunctions);
            PointerBuffer pointer = stack.callocPointer(1);
            VulkanUtils.throwIfFailure(Vma.vmaCreateAllocator(createInfo, pointer), "Failed to create VMA allocator", BackendCreationException.Reason.OTHER);
            return pointer.get(0);
        }
    }

    private static VulkanPhysicalDevice findPhysicalDevice(final VulkanInstance instance) throws BackendCreationException {
        VkPhysicalDevice firstDevice = null;
        VkPhysicalDevice selectedDevice = null;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer intBuffer = stack.callocInt(1);
            VulkanUtils.throwIfFailure(
                VK12.vkEnumeratePhysicalDevices(instance.vkInstance(), intBuffer, null),
                "Failed to get number of physical devices",
                BackendCreationException.Reason.VULKAN_NO_DEVICE
            );
            if (intBuffer.get(0) == 0) {
                throw new BackendCreationException("No Vulkan capable devices", BackendCreationException.Reason.VULKAN_NO_DEVICE);
            }

            PointerBuffer pPhysicalDevices = stack.callocPointer(intBuffer.get(0));
            VulkanUtils.throwIfFailure(
                VK12.vkEnumeratePhysicalDevices(instance.vkInstance(), intBuffer, pPhysicalDevices),
                "Failed to get physical devices",
                BackendCreationException.Reason.VULKAN_NO_DEVICE
            );
            int numDevices = intBuffer.get(0);
            if (numDevices == 0) {
                throw new BackendCreationException("No Vulkan capable devices", BackendCreationException.Reason.VULKAN_NO_DEVICE);
            }

            for (int i = 0; i < numDevices; i++) {
                if (pPhysicalDevices.get(i) != 0L) {
                    VkPhysicalDevice currentDevice = new VkPhysicalDevice(pPhysicalDevices.get(i), instance.vkInstance());
                    if (firstDevice == null) {
                        firstDevice = currentDevice;
                    }

                    if (deviceMeetsFeatureQueryRequirements(currentDevice) && isDeviceSuitable(currentDevice)) {
                        if (selectedDevice == null) {
                            selectedDevice = currentDevice;
                        } else if (isDeviceDiscrete(currentDevice) && !isDeviceDiscrete(selectedDevice)) {
                            LOGGER.info("Preferring discrete GPU: {}", getDeviceName(currentDevice));
                            selectedDevice = currentDevice;
                            break;
                        }
                    }
                }
            }
        }

        if (firstDevice == null) {
            throw new BackendCreationException("No Vulkan capable devices", BackendCreationException.Reason.VULKAN_NO_DEVICE);
        }

        if (selectedDevice == null) {
            throwForMissingRequrements(firstDevice);
            assert false;
        }

        return new VulkanPhysicalDevice(selectedDevice);
    }

    private static boolean deviceMeetsFeatureQueryRequirements(final VkPhysicalDevice vkPhysicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);
            VK12.vkGetPhysicalDeviceProperties(vkPhysicalDevice, properties);
            return properties.apiVersion() >= VK12.VK_API_VERSION_1_1;
        }
    }

    private static boolean isDeviceSuitable(final VkPhysicalDevice vkPhysicalDevice) throws BackendCreationException {
        try (
            VulkanPhysicalDevice physicalDevice = new VulkanPhysicalDevice(vkPhysicalDevice);
            MemoryStack stack = MemoryStack.stackPush();
        ) {
            String deviceName = physicalDevice.deviceName();
            VulkanUtils.DeviceUUID deviceUUID = new VulkanUtils.DeviceUUID(
                physicalDevice.vkPhysicalDeviceDriverProperties().driverID(),
                physicalDevice.vkPhysicalDeviceProperties().vendorID(),
                physicalDevice.vkPhysicalDeviceProperties().deviceID()
            );
            if (!VulkanUtils.KNOWN_PROBLEMATIC_DEVICES.contains(deviceUUID)) {
                Set<String> missingExtensions = physicalDevice.getMissingExtensions(REQUIRED_DEVICE_EXTENSIONS);
                VkPhysicalDeviceFeatures2 deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();

                for (VulkanFeature requiredDeviceFeature : REQUIRED_DEVICE_FEATURES) {
                    requiredDeviceFeature.struct().findOrCreateStructInPNextChain(deviceFeatures, stack);
                }

                VK12.vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, deviceFeatures);
                boolean isSuitableDevice = true;
                if (physicalDevice.vkPhysicalDeviceProperties().apiVersion() < VK12.VK_API_VERSION_1_2) {
                    LOGGER.warn("Device [{}] does not support Vulkan 1.2", deviceName);
                    isSuitableDevice = false;
                }

                if (physicalDevice.graphicsQueueFamilyAndIndex() == null) {
                    LOGGER.warn("Device [{}] does not have a graphics queue", deviceName);
                    isSuitableDevice = false;
                }

                if (!missingExtensions.isEmpty()) {
                    LOGGER.warn("Device [{}] does not support required extensions, missing: {}", deviceName, missingExtensions);
                    isSuitableDevice = false;
                }

                for (VulkanFeature requiredDeviceFeature : REQUIRED_DEVICE_FEATURES) {
                    if (!requiredDeviceFeature.get(deviceFeatures)) {
                        LOGGER.warn("Device [{}] does not have required feature [{}]", deviceName, requiredDeviceFeature.name());
                        isSuitableDevice = false;
                    }
                }

                if (isSuitableDevice) {
                    LOGGER.debug("Device [{}] is suitable", deviceName);
                }

                return isSuitableDevice;
            } else {
                LOGGER.warn("Device [{}] is known to be problematic, skipping", deviceName);
                return false;
            }
        }
    }

    private static boolean isDeviceDiscrete(final VkPhysicalDevice vkPhysicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties2 deviceProperties = VkPhysicalDeviceProperties2.calloc(stack).sType$Default();
            VK12.vkGetPhysicalDeviceProperties2(vkPhysicalDevice, deviceProperties);
            return deviceProperties.properties().deviceType() == 2;
        }
    }

    private static String getDeviceName(final VkPhysicalDevice vkPhysicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties2 deviceProperties = VkPhysicalDeviceProperties2.calloc(stack).sType$Default();
            VK12.vkGetPhysicalDeviceProperties2(vkPhysicalDevice, deviceProperties);
            return deviceProperties.properties().deviceNameString();
        }
    }

    private static boolean isFeatureSupported(final VkPhysicalDevice vkPhysicalDevice, final VulkanFeature feature) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceFeatures2 deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();
            feature.struct().findOrCreateStructInPNextChain(deviceFeatures, stack);
            VK12.vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, deviceFeatures);
            return feature.get(deviceFeatures);
        }
    }

    private static void throwForMissingRequrements(final VkPhysicalDevice vkPhysicalDevice) throws BackendCreationException {
        List<String> missingCapabilities = new ReferenceArrayList<>();
        BackendCreationException.Reason mostProminentReason = BackendCreationException.Reason.OTHER;
        if (!deviceMeetsFeatureQueryRequirements(vkPhysicalDevice)) {
            throw new BackendCreationException(
                "Device missing capabilities", BackendCreationException.Reason.VULKAN_DEVICE_VERSION_TOO_LOW, List.of("VULKAN_CORE_1_1")
            );
        }

        try (
            VulkanPhysicalDevice physicalDevice = new VulkanPhysicalDevice(vkPhysicalDevice);
            MemoryStack stack = MemoryStack.stackPush();
        ) {
            VkPhysicalDeviceFeatures2 deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();

            for (VulkanFeature requiredDeviceFeature : REQUIRED_DEVICE_FEATURES) {
                requiredDeviceFeature.struct().findOrCreateStructInPNextChain(deviceFeatures, stack);
            }

            VK12.vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, deviceFeatures);

            for (VulkanFeature requiredDeviceFeature : REQUIRED_DEVICE_FEATURES) {
                if (!requiredDeviceFeature.get(deviceFeatures)) {
                    mostProminentReason = BackendCreationException.Reason.VULKAN_MISSING_FEATURE;
                    missingCapabilities.add(requiredDeviceFeature.name());
                }
            }

            Set<String> missingExtensions = physicalDevice.getMissingExtensions(REQUIRED_DEVICE_EXTENSIONS);
            if (!missingExtensions.isEmpty()) {
                mostProminentReason = BackendCreationException.Reason.VULKAN_MISSING_EXTENSION;
                missingCapabilities.addAll(missingExtensions);
            }

            if (physicalDevice.graphicsQueueFamilyAndIndex() == null) {
                mostProminentReason = BackendCreationException.Reason.VULKAN_NO_GRAPHICS_QUEUE;
                missingCapabilities.add("COMBINED_GRAPHICS_COMPUTE_PRESENT_QUEUE");
            }

            if (physicalDevice.vkPhysicalDeviceProperties().apiVersion() < VK12.VK_API_VERSION_1_2) {
                mostProminentReason = BackendCreationException.Reason.VULKAN_DEVICE_VERSION_TOO_LOW;
                missingCapabilities.add("VULKAN_CORE_1_2");
            }
        }

        throw new BackendCreationException("Device missing capabilities", mostProminentReason, missingCapabilities);
    }

    private static VkDevice createDevice(
        final Collection<String> deviceExtensions, final VulkanPhysicalDevice physicalDevice, final Set<VulkanFeature> vulkanFeatures
    ) throws BackendCreationException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceFeatures2 deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();

            for (VulkanFeature requiredDeviceFeature : vulkanFeatures) {
                requiredDeviceFeature.set(deviceFeatures, true, stack);
            }

            Int2IntMap queuesToCreate = physicalDevice.queueFamilyCreateInfoMap();
            Buffer queueCreationInfo = VkDeviceQueueCreateInfo.calloc(queuesToCreate.size(), stack);

            for (Entry familyCount : queuesToCreate.int2IntEntrySet()) {
                queueCreationInfo.sType$Default();
                queueCreationInfo.queueFamilyIndex(familyCount.getIntKey());
                queueCreationInfo.pQueuePriorities(stack.callocFloat(familyCount.getIntValue()));
                queueCreationInfo.position(queueCreationInfo.position() + 1);
            }

            queueCreationInfo.position(0);
            PointerBuffer enabledExtensionsBuffer = stack.callocPointer(deviceExtensions.size());

            for (String name : deviceExtensions) {
                enabledExtensionsBuffer.put(stack.UTF8(name));
            }

            enabledExtensionsBuffer.flip();
            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack).sType$Default();
            deviceCreateInfo.pNext(deviceFeatures.pNext());
            deviceCreateInfo.pQueueCreateInfos(queueCreationInfo);
            deviceCreateInfo.ppEnabledExtensionNames(enabledExtensionsBuffer);
            deviceCreateInfo.pEnabledFeatures(deviceFeatures.features());
            PointerBuffer pointer = stack.callocPointer(1);
            VulkanUtils.throwIfFailure(
                VK12.vkCreateDevice(physicalDevice.vkPhysicalDevice(), deviceCreateInfo, null, pointer),
                "Failed to create device",
                BackendCreationException.Reason.VULKAN_NO_DEVICE
            );
            return new VkDevice(pointer.get(0), physicalDevice.vkPhysicalDevice(), deviceCreateInfo);
        }
    }
}