package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.logging.LogUtils;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkExtensionProperties.Buffer;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class VulkanInstance implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String APPLICATION_NAME = "Minecraft Java Edition";
    private static final int APPLICATION_VERSION = SharedConstants.getCurrentVersion().dataVersion().version();
    private static final String ENGINE_NAME = "MinecraftJE";
    private static final int ENGINE_VERSION = 0;
    private final Set<String> enabledExtensions = new HashSet<>();
    private final VkInstance vkInstance;
    private final VulkanDebug debug;

    protected VulkanInstance(final int debugVerbosity, boolean wantsDebugLabels, final boolean validation) throws BackendCreationException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                .sType$Default()
                .pApplicationName(stack.UTF8("Minecraft Java Edition"))
                .applicationVersion(APPLICATION_VERSION)
                .pEngineName(stack.UTF8("MinecraftJE"))
                .engineVersion(0)
                .apiVersion(VK12.VK_API_VERSION_1_2);
            List<String> validationLayers = this.getSupportedValidationLayers();
            PointerBuffer requiredLayers = null;
            if (validation) {
                if (validationLayers.contains("VK_LAYER_KHRONOS_validation")) {
                    requiredLayers = stack.callocPointer(1);
                    requiredLayers.put(0, stack.ASCII("VK_LAYER_KHRONOS_validation"));
                    LOGGER.warn("Enabling Vulkan validation layers");
                    wantsDebugLabels = true;
                } else {
                    LOGGER.warn("Vulkan validation layers requested but not found");
                }
            }

            Set<String> availableExtensions = this.getSupportedInstanceExtensions();
            PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (glfwExtensions == null) {
                throw new BackendCreationException("Failed to find the GLFW platform surface extensions", BackendCreationException.Reason.GLFW_ERROR);
            }

            while (glfwExtensions.remaining() > 0) {
                this.enabledExtensions.add(MemoryUtil.memUTF8(glfwExtensions.get()));
            }

            this.debug = VulkanDebug.create(debugVerbosity, wantsDebugLabels, availableExtensions, this.enabledExtensions);
            boolean usePortability = availableExtensions.contains("VK_KHR_portability_enumeration") && Util.getPlatform() == Util.OS.OSX;
            if (usePortability) {
                this.enabledExtensions.add("VK_KHR_portability_enumeration");
            }

            PointerBuffer enabledExtensionsBuffer = stack.callocPointer(this.enabledExtensions.size());

            for (String name : this.enabledExtensions) {
                enabledExtensionsBuffer.put(stack.UTF8(name));
            }

            enabledExtensionsBuffer.flip();
            VkInstanceCreateInfo instanceInfo = VkInstanceCreateInfo.calloc(stack)
                .sType$Default()
                .pApplicationInfo(appInfo)
                .ppEnabledLayerNames(requiredLayers)
                .ppEnabledExtensionNames(enabledExtensionsBuffer);
            if (usePortability) {
                instanceInfo.flags(1);
            }

            this.debug.chainCreateInfo(instanceInfo, stack);
            PointerBuffer pInstance = stack.callocPointer(1);
            VulkanUtils.throwIfFailure(
                VK12.vkCreateInstance(instanceInfo, null, pInstance),
                "Error creating instance",
                BackendCreationException.Reason.VULKAN_INSTANCE_CREATION_FAILED
            );
            this.vkInstance = new VkInstance(pInstance.get(0), instanceInfo);
            this.debug.setup(this.vkInstance);
        }
    }

    public VkInstance vkInstance() {
        return this.vkInstance;
    }

    private Set<String> getSupportedInstanceExtensions() throws BackendCreationException {
        Set<String> instanceExtensions = new HashSet<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer numExtensionsBuf = stack.callocInt(1);
            VulkanUtils.throwIfFailure(
                VK12.vkEnumerateInstanceExtensionProperties((String)null, numExtensionsBuf, null),
                "Error enumerating instance extensions",
                BackendCreationException.Reason.VULKAN_INSTANCE_CREATION_FAILED
            );
            int numExtensions = numExtensionsBuf.get(0);
            Buffer instanceExtensionsProps = VkExtensionProperties.calloc(numExtensions, stack);
            VulkanUtils.throwIfFailure(
                VK12.vkEnumerateInstanceExtensionProperties((String)null, numExtensionsBuf, instanceExtensionsProps),
                "Error enumerating instance extensions",
                BackendCreationException.Reason.VULKAN_INSTANCE_CREATION_FAILED
            );

            for (int i = 0; i < numExtensions; i++) {
                VkExtensionProperties props = instanceExtensionsProps.get(i);
                String extensionName = props.extensionNameString();
                instanceExtensions.add(extensionName);
            }
        }

        return instanceExtensions;
    }

    private List<String> getSupportedValidationLayers() throws BackendCreationException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer numLayersArr = stack.callocInt(1);
            VulkanUtils.throwIfFailure(
                VK12.vkEnumerateInstanceLayerProperties(numLayersArr, null),
                "Error enumerating validation layers",
                BackendCreationException.Reason.VULKAN_INSTANCE_CREATION_FAILED
            );
            int numLayers = numLayersArr.get(0);
            org.lwjgl.vulkan.VkLayerProperties.Buffer propsBuf = VkLayerProperties.calloc(numLayers, stack);
            VulkanUtils.throwIfFailure(
                VK12.vkEnumerateInstanceLayerProperties(numLayersArr, propsBuf),
                "Error enumerating validation layers",
                BackendCreationException.Reason.VULKAN_INSTANCE_CREATION_FAILED
            );
            List<String> supportedLayers = new ArrayList<>();

            for (int i = 0; i < numLayers; i++) {
                VkLayerProperties props = propsBuf.get(i);
                String layerName = props.layerNameString();
                supportedLayers.add(layerName);
            }

            return supportedLayers;
        }
    }

    @Override
    public void close() {
        this.debug.destroy(this.vkInstance);
        VK12.vkDestroyInstance(this.vkInstance, null);
    }

    public Set<String> getEnabledExtensions() {
        return this.enabledExtensions;
    }

    public VulkanDebug debug() {
        return this.debug;
    }
}