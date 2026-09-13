package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.DeviceType;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceDriverProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceMultiDrawPropertiesEXT;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties2;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan11Properties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkExtensionProperties.Buffer;

@OnlyIn(Dist.CLIENT)
public class VulkanPhysicalDevice implements AutoCloseable {
    private final VkPhysicalDevice vkPhysicalDevice;
    private final Buffer vkDeviceExtensions;
    private final VkPhysicalDeviceFeatures2 vkPhysicalDeviceFeatures;
    private final VkPhysicalDeviceProperties2 vkPhysicalDeviceProperties;
    private final VkPhysicalDeviceVulkan11Properties vkPhysicalDeviceVulkan11Properties;
    private final VkPhysicalDeviceDriverProperties vkPhysicalDeviceDriverProperties;
    private final VkPhysicalDeviceMultiDrawPropertiesEXT vkPhysicalDeviceMultiDrawPropertiesEXT;
    private final Int2IntMap queueFamilyCreateInfoMap;
    private final @Nullable IntIntPair graphicsQueueFamilyAndIndex;
    private final @Nullable IntIntPair computeQueueFamilyAndIndex;
    private final @Nullable IntIntPair transferQueueFamilyAndIndex;

    public VulkanPhysicalDevice(final VkPhysicalDevice vkPhysicalDevice) throws BackendCreationException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.vkPhysicalDevice = vkPhysicalDevice;
            IntBuffer intBuffer = stack.callocInt(1);
            VulkanUtils.throwIfFailure(
                VK12.vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, (String)null, intBuffer, null),
                "Failed to get number of device extension properties",
                BackendCreationException.Reason.VULKAN_NO_DEVICE
            );
            this.vkDeviceExtensions = VkExtensionProperties.calloc(intBuffer.get(0));
            VulkanUtils.throwIfFailure(
                VK12.vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, (String)null, intBuffer, this.vkDeviceExtensions),
                "Failed to get extension properties",
                BackendCreationException.Reason.VULKAN_NO_DEVICE
            );
            this.vkPhysicalDeviceProperties = VkPhysicalDeviceProperties2.calloc().sType$Default();
            this.vkPhysicalDeviceVulkan11Properties = VkPhysicalDeviceVulkan11Properties.calloc().sType$Default();
            this.vkPhysicalDeviceDriverProperties = VkPhysicalDeviceDriverProperties.calloc().sType$Default();
            this.vkPhysicalDeviceMultiDrawPropertiesEXT = VkPhysicalDeviceMultiDrawPropertiesEXT.calloc().sType$Default();
            this.vkPhysicalDeviceProperties.pNext(this.vkPhysicalDeviceDriverProperties);
            this.vkPhysicalDeviceProperties.pNext(this.vkPhysicalDeviceVulkan11Properties);
            if (this.hasDeviceExtension("VK_EXT_multi_draw")) {
                this.vkPhysicalDeviceProperties.pNext(this.vkPhysicalDeviceMultiDrawPropertiesEXT);
            }

            VK12.vkGetPhysicalDeviceProperties2(vkPhysicalDevice, this.vkPhysicalDeviceProperties);
            VK12.vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, intBuffer, null);
            org.lwjgl.vulkan.VkQueueFamilyProperties.Buffer vkQueueFamilyProps = VkQueueFamilyProperties.calloc(intBuffer.get(0), stack);
            VK12.vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, intBuffer, vkQueueFamilyProps);
            this.vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures2.calloc().sType$Default();
            VK12.vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, this.vkPhysicalDeviceFeatures);
            int graphicsQueueFamily = -1;
            int computeQueueFamily = -1;
            int transferQueueFamily = -1;
            int computeQueueFamilyBits = -1;
            int transferQueueFamilyBits = -1;
            int numQueueFamilies = vkQueueFamilyProps.capacity();

            for (int i = 0; i < numQueueFamilies; i++) {
                int familyUsedQueues = 0;
                VkQueueFamilyProperties queueFamilyProperties = vkQueueFamilyProps.get(i);
                if (graphicsQueueFamily == -1
                    && VulkanUtils.hasAllBits(queueFamilyProperties.queueFlags(), 3)
                    && GLFWVulkan.glfwGetPhysicalDevicePresentationSupport(vkPhysicalDevice.getInstance(), vkPhysicalDevice, i)) {
                    graphicsQueueFamily = i;
                    familyUsedQueues++;
                }

                if (queueFamilyProperties.queueCount() > familyUsedQueues) {
                    if (VulkanUtils.hasAllBits(queueFamilyProperties.queueFlags(), 2)
                        && (computeQueueFamily == -1 || Integer.bitCount(queueFamilyProperties.queueFlags()) <= Integer.bitCount(computeQueueFamilyBits))) {
                        computeQueueFamily = i;
                        computeQueueFamilyBits = queueFamilyProperties.queueFlags();
                        familyUsedQueues++;
                    }

                    if (queueFamilyProperties.queueCount() > familyUsedQueues
                        && VulkanUtils.hasAnyBit(queueFamilyProperties.queueFlags(), 7)
                        && (transferQueueFamily == -1 || Integer.bitCount(queueFamilyProperties.queueFlags()) <= Integer.bitCount(transferQueueFamilyBits))) {
                        transferQueueFamily = i;
                        transferQueueFamilyBits = queueFamilyProperties.queueFlags();
                        familyUsedQueues++;
                    }
                }
            }

            Int2IntMap familyMap = new Int2IntArrayMap();
            int graphicsQueueIndex = familyMap.put(graphicsQueueFamily, familyMap.get(graphicsQueueFamily) + 1);
            int computeQueueIndex = familyMap.put(computeQueueFamily, familyMap.get(computeQueueFamily) + 1);
            int transferQueueIndex = familyMap.put(transferQueueFamily, familyMap.get(transferQueueFamily) + 1);
            familyMap.remove(-1);
            this.queueFamilyCreateInfoMap = Int2IntMaps.unmodifiable(familyMap);
            this.graphicsQueueFamilyAndIndex = graphicsQueueFamily == -1 ? null : new IntIntImmutablePair(graphicsQueueFamily, graphicsQueueIndex);
            this.computeQueueFamilyAndIndex = computeQueueFamily == -1 ? null : new IntIntImmutablePair(computeQueueFamily, computeQueueIndex);
            this.transferQueueFamilyAndIndex = transferQueueFamily == -1 ? null : new IntIntImmutablePair(transferQueueFamily, transferQueueIndex);
        }
    }

    @Override
    public void close() {
        this.vkPhysicalDeviceFeatures.free();
        this.vkDeviceExtensions.free();
        this.vkPhysicalDeviceMultiDrawPropertiesEXT.free();
        this.vkPhysicalDeviceVulkan11Properties.free();
        this.vkPhysicalDeviceDriverProperties.free();
        this.vkPhysicalDeviceProperties.free();
    }

    public String deviceName() {
        return this.vkPhysicalDeviceProperties.properties().deviceNameString();
    }

    public String vendorName() {
        int vendorId = this.vkPhysicalDeviceProperties.properties().vendorID();

        return switch (vendorId) {
            case 4098, 4130 -> "AMD";
            case 4112 -> "IMGTEC";
            case 4203 -> "APPLE";
            case 4318, 4818 -> "NVIDIA";
            case 5045 -> "ARM";
            case 5140 -> "MICROSOFT";
            case 5348 -> "BROADCOM";
            case 5772, 6091, 6505, 20803 -> "QUALCOMM";
            case 32902 -> "INTEL";
            default -> String.format(Locale.ROOT, "0x%x", vendorId);
        };
    }

    public VkPhysicalDevice vkPhysicalDevice() {
        return this.vkPhysicalDevice;
    }

    public VkPhysicalDeviceProperties vkPhysicalDeviceProperties() {
        return this.vkPhysicalDeviceProperties.properties();
    }

    public VkPhysicalDeviceVulkan11Properties vkPhysicalDeviceVulkan11Properties() {
        return this.vkPhysicalDeviceVulkan11Properties;
    }

    public VkPhysicalDeviceDriverProperties vkPhysicalDeviceDriverProperties() {
        return this.vkPhysicalDeviceDriverProperties;
    }

    public VkPhysicalDeviceMultiDrawPropertiesEXT vkPhysicalDeviceMultiDrawPropertiesEXT() {
        return this.vkPhysicalDeviceMultiDrawPropertiesEXT;
    }

    public boolean hasDeviceExtension(final String name) {
        return this.vkDeviceExtensions.stream().anyMatch(e -> e.extensionNameString().equals(name));
    }

    public Set<String> getMissingExtensions(final Collection<String> required) {
        Set<String> remaining = new HashSet<>(required);

        for (VkExtensionProperties extension : this.vkDeviceExtensions) {
            remaining.remove(extension.extensionNameString());
        }

        return remaining;
    }

    public Int2IntMap queueFamilyCreateInfoMap() {
        return this.queueFamilyCreateInfoMap;
    }

    public @Nullable IntIntPair graphicsQueueFamilyAndIndex() {
        return this.graphicsQueueFamilyAndIndex;
    }

    public @Nullable IntIntPair computeQueueFamilyAndIndex() {
        return this.computeQueueFamilyAndIndex;
    }

    public @Nullable IntIntPair transferQueueFamilyAndIndex() {
        return this.transferQueueFamilyAndIndex;
    }

    private static String getStandardEncodingVersion(final int version) {
        int major = version >>> 22 & 127;
        int minor = version >>> 12 & 1023;
        int patch = version & 4095;
        return String.format(Locale.ROOT, "%d.%d.%d", major, minor, patch);
    }

    public String driverInfo() {
        int apiVersion = this.vkPhysicalDeviceProperties.properties().apiVersion();
        String versionString = getStandardEncodingVersion(apiVersion);
        return String.format(
            Locale.ROOT,
            "%s %s %s",
            versionString,
            this.vkPhysicalDeviceDriverProperties.driverNameString(),
            this.vkPhysicalDeviceDriverProperties.driverInfoString()
        );
    }

    public DeviceType deviceType() {
        return switch (this.vkPhysicalDeviceProperties.properties().deviceType()) {
            case 1 -> DeviceType.INTEGRATED;
            case 2 -> DeviceType.DISCRETE;
            case 3 -> DeviceType.VIRTUAL;
            case 4 -> DeviceType.CPU;
            default -> DeviceType.OTHER;
        };
    }
}