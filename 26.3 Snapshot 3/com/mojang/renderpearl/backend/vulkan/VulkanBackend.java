package com.mojang.renderpearl.backend.vulkan;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.platform.NativeLibrariesBootstrap;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.backend.vulkan.checkpoints.AmdCheckpointExtension;
import com.mojang.renderpearl.backend.vulkan.checkpoints.CheckpointExtension;
import com.mojang.renderpearl.backend.vulkan.checkpoints.NoopCheckpointExtension;
import com.mojang.renderpearl.backend.vulkan.checkpoints.NvidiaCheckpointExtension;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties2;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo.Buffer;
import org.slf4j.Logger;

public class VulkanBackend implements GpuBackend {
   private static final Logger LOGGER = LogUtils.getLogger();

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

      Set<FeatureSet> requiredFeatureSets = VulkanFeatureSets.requiredFeatureSets();
      Set<FeatureSet> requiredIfExtensionsAvailableFeatureSets = VulkanFeatureSets.requiredIfExtensionsAvailableFeatureSets();

      try (
         VulkanInstance instance = new VulkanInstance(0, false, false);
         VulkanPhysicalDevice physicalDevice = findPhysicalDevice(instance, requiredFeatureSets, requiredIfExtensionsAvailableFeatureSets);
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
   public GpuDevice createDevice(final long window, final GpuDebugOptions debugOptions) throws BackendCreationException {
      if (!NativeLibrariesBootstrap.isVulkanLoaderAvailable()) {
         throw new BackendCreationException("Vulkan loader library is missing", BackendCreationException.Reason.VULKAN_LOADER_MISSING);
      }

      if (!GLFWVulkan.glfwVulkanSupported()) {
         throw new BackendCreationException("Vulkan is not supported", BackendCreationException.Reason.GLFW_ERROR);
      }

      Set<FeatureSet> requiredFeatureSets = VulkanFeatureSets.requiredFeatureSets();
      Set<FeatureSet> requiredIfExtensionsAvailableFeatureSets = VulkanFeatureSets.requiredIfExtensionsAvailableFeatureSets();
      Set<FeatureSet> optionalFeatureSets = VulkanFeatureSets.optionalFeatureSets();
      VulkanInstance instance = null;
      VulkanPhysicalDevice physicalDevice = null;
      VkDevice device = null;
      long vma = 0L;
      CheckpointExtension checkpointExtension = NoopCheckpointExtension.INSTANCE;

      FeatureSet enabledFeatures;
      try {
         boolean renderdocAttached = "1".equals(System.getenv("ENABLE_VULKAN_RENDERDOC_CAPTURE"));
         instance = new VulkanInstance(debugOptions.logLevel(), debugOptions.useLabels() || renderdocAttached, debugOptions.useValidationLayers());
         physicalDevice = findPhysicalDevice(instance, requiredFeatureSets, requiredIfExtensionsAvailableFeatureSets);
         Set<String> deviceExtensions = VulkanUtils.enumerateExtensions(physicalDevice.vkPhysicalDevice());
         Set<FeatureSet> enabledFeatureSets = new ObjectOpenHashSet(requiredFeatureSets);

         for (FeatureSet featureSet : requiredIfExtensionsAvailableFeatureSets) {
            if (featureSet.isSupported(physicalDevice.vkPhysicalDevice(), deviceExtensions)) {
               enabledFeatureSets.add(featureSet);
               LOGGER.info("Enabling required for device FeatureSet [{}]", featureSet.name());
            }
         }

         for (FeatureSet featureSet : optionalFeatureSets) {
            if (featureSet.isSupported(physicalDevice.vkPhysicalDevice(), deviceExtensions)) {
               if (featureSet.checkCondition(physicalDevice.vkPhysicalDevice())) {
                  enabledFeatureSets.add(featureSet);
                  LOGGER.info("Enabling optional FeatureSet [{}]", featureSet.name());
               } else {
                  LOGGER.info("Optional FeatureSet [{}] supported but device condition failed and will not be enabled", featureSet.name());
               }
            } else {
               LOGGER.info("Optional FeatureSet [{}] not supported", featureSet.name());
            }
         }

         if (enabledFeatureSets.contains(VulkanFeatureSets.AMD_BUFFER_MARKER_FEATURESET)) {
            checkpointExtension = new AmdCheckpointExtension();
         } else if (enabledFeatureSets.contains(VulkanFeatureSets.NV_DIAGNOSTIC_CHECKPOINT_FEATURESET)) {
            checkpointExtension = new NvidiaCheckpointExtension();
         }

         enabledFeatures = new FeatureSet("Enabled", enabledFeatureSets);
         device = createDevice(enabledFeatures, physicalDevice);
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

      return new GpuDevice(new VulkanDevice(instance, physicalDevice, enabledFeatures, device, vma, checkpointExtension));
   }

   private static long createVma(final VkDevice vkDevice) throws BackendCreationException {
      MemoryStack stack = MemoryStack.stackPush();

      long var5;
      try {
         VmaVulkanFunctions vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack).set(vkDevice.getPhysicalDevice().getInstance(), vkDevice);
         VmaAllocatorCreateInfo createInfo = VmaAllocatorCreateInfo.calloc(stack)
            .instance(vkDevice.getPhysicalDevice().getInstance())
            .vulkanApiVersion(VK12.VK_API_VERSION_1_2)
            .device(vkDevice)
            .physicalDevice(vkDevice.getPhysicalDevice())
            .pVulkanFunctions(vmaVulkanFunctions);
         PointerBuffer pointer = stack.callocPointer(1);
         VulkanUtils.throwIfFailure(Vma.vmaCreateAllocator(createInfo, pointer), "Failed to create VMA allocator", BackendCreationException.Reason.OTHER);
         var5 = pointer.get(0);
      } catch (Throwable var8) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (stack != null) {
         stack.close();
      }

      return var5;
   }

   private static VulkanPhysicalDevice findPhysicalDevice(
      final VulkanInstance instance, final Set<FeatureSet> requiredFeatureSets, final Set<FeatureSet> requiredIfExtensionsAvailable
   ) throws BackendCreationException {
      BackendCreationException deviceFailureReason = null;
      VkPhysicalDevice selectedDevice = null;
      MemoryStack stack = MemoryStack.stackPush();

      try {
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
               BackendCreationException failureReason = checkDeviceSuitability(currentDevice, requiredFeatureSets, requiredIfExtensionsAvailable);
               if (failureReason != null) {
                  if (deviceFailureReason == null) {
                     deviceFailureReason = failureReason;
                  }
               } else if (selectedDevice == null) {
                  selectedDevice = currentDevice;
               } else if (isDeviceDiscrete(currentDevice) && !isDeviceDiscrete(selectedDevice)) {
                  LOGGER.info("Preferring discrete GPU: {}", getDeviceName(currentDevice));
                  selectedDevice = currentDevice;
                  break;
               }
            }
         }
      } catch (Throwable var13) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }
         }

         throw var13;
      }

      if (stack != null) {
         stack.close();
      }

      if (selectedDevice == null) {
         if (deviceFailureReason == null) {
            throw new BackendCreationException("No Vulkan capable devices", BackendCreationException.Reason.VULKAN_NO_DEVICE);
         } else {
            throw deviceFailureReason;
         }
      } else {
         return new VulkanPhysicalDevice(selectedDevice);
      }
   }

   private static boolean deviceMeetsFeatureQueryRequirements(final VkPhysicalDevice vkPhysicalDevice) {
      MemoryStack stack = MemoryStack.stackPush();

      boolean var3;
      try {
         VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);
         VK12.vkGetPhysicalDeviceProperties(vkPhysicalDevice, properties);
         var3 = properties.apiVersion() >= VK12.VK_API_VERSION_1_1;
      } catch (Throwable var5) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (stack != null) {
         stack.close();
      }

      return var3;
   }

   private static boolean isDeviceDiscrete(final VkPhysicalDevice vkPhysicalDevice) {
      MemoryStack stack = MemoryStack.stackPush();

      boolean var3;
      try {
         VkPhysicalDeviceProperties2 deviceProperties = VkPhysicalDeviceProperties2.calloc(stack).sType$Default();
         VK12.vkGetPhysicalDeviceProperties2(vkPhysicalDevice, deviceProperties);
         var3 = deviceProperties.properties().deviceType() == 2;
      } catch (Throwable var5) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (stack != null) {
         stack.close();
      }

      return var3;
   }

   private static String getDeviceName(final VkPhysicalDevice vkPhysicalDevice) {
      MemoryStack stack = MemoryStack.stackPush();

      String var3;
      try {
         VkPhysicalDeviceProperties2 deviceProperties = VkPhysicalDeviceProperties2.calloc(stack).sType$Default();
         VK12.vkGetPhysicalDeviceProperties2(vkPhysicalDevice, deviceProperties);
         var3 = deviceProperties.properties().deviceNameString();
      } catch (Throwable var5) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (stack != null) {
         stack.close();
      }

      return var3;
   }

   private static BackendCreationException checkDeviceSuitability(
      final VkPhysicalDevice vkPhysicalDevice, final Set<FeatureSet> requiredFeatureSets, final Set<FeatureSet> requiredIfExtensionsAvailable
   ) throws BackendCreationException {
      try (VulkanPhysicalDevice physicalDevice = new VulkanPhysicalDevice(vkPhysicalDevice)) {
         String deviceName = physicalDevice.deviceName();
         if (!deviceMeetsFeatureQueryRequirements(vkPhysicalDevice)) {
            LOGGER.warn("Device [{}] does not support Vulkan 1.1, skipping further capability checks", deviceName);
            return new BackendCreationException(
               "Device missing capabilities", BackendCreationException.Reason.VULKAN_DEVICE_VERSION_TOO_LOW, List.of("VULKAN_CORE_1_1")
            );
         }

         VulkanUtils.DeviceUUID deviceUUID = new VulkanUtils.DeviceUUID(
            physicalDevice.vkPhysicalDeviceDriverProperties().driverID(),
            physicalDevice.vkPhysicalDeviceProperties().vendorID(),
            physicalDevice.vkPhysicalDeviceProperties().deviceID()
         );
         if (VulkanUtils.KNOWN_PROBLEMATIC_DEVICES.contains(deviceUUID)) {
            LOGGER.warn("Device [{}] is known to be problematic, skipping", deviceName);
            return new BackendCreationException("Device known problematic", BackendCreationException.Reason.VULKAN_KNOWN_PROBLEMATIC, List.of());
         }

         Set<FeatureSet> deviceRequiredFeatureSets = new ObjectOpenHashSet(requiredFeatureSets);
         Set<String> deviceExtensions = VulkanUtils.enumerateExtensions(vkPhysicalDevice);

         for (FeatureSet featureSet : requiredIfExtensionsAvailable) {
            if (deviceExtensions.containsAll(featureSet.extensions())) {
               LOGGER.warn("Device [{}] supports all extensions from FeatureSet [{}], making required", deviceName, featureSet.name());
               deviceRequiredFeatureSets.add(featureSet);
            }
         }

         List<String> missingCapabilities = new ReferenceArrayList();
         BackendCreationException.Reason mostProminentReason = null;

         for (FeatureSet featureSet : deviceRequiredFeatureSets) {
            Set<VulkanFeature> missingFeatures = featureSet.unsupportedFeatures(vkPhysicalDevice);
            if (!missingFeatures.isEmpty()) {
               LOGGER.warn(
                  "Device [{}] does not support required features from FeatureSet [{}], missing: {}",
                  new Object[]{deviceName, featureSet.name(), missingFeatures}
               );
               mostProminentReason = BackendCreationException.Reason.VULKAN_MISSING_FEATURE;

               for (VulkanFeature missingFeature : missingFeatures) {
                  missingCapabilities.add(missingFeature.name());
               }
            }
         }

         for (FeatureSet featureSet : deviceRequiredFeatureSets) {
            Set<String> missingExtensions = featureSet.unsupportedExtensions(deviceExtensions);
            if (!missingExtensions.isEmpty()) {
               LOGGER.warn(
                  "Device [{}] does not support required extensions from FeatureSet [{}], missing: {}",
                  new Object[]{deviceName, featureSet.name(), missingExtensions}
               );
               mostProminentReason = BackendCreationException.Reason.VULKAN_MISSING_EXTENSION;
               missingCapabilities.addAll(missingExtensions);
            }
         }

         if (physicalDevice.graphicsQueueFamilyAndIndex() == null) {
            LOGGER.warn("Device [{}] does not have a graphics queue", deviceName);
            mostProminentReason = BackendCreationException.Reason.VULKAN_NO_GRAPHICS_QUEUE;
            missingCapabilities.add("COMBINED_GRAPHICS_COMPUTE_PRESENT_QUEUE");
         }

         if (physicalDevice.vkPhysicalDeviceProperties().apiVersion() < VK12.VK_API_VERSION_1_2) {
            LOGGER.warn("Device [{}] does not support Vulkan 1.2", deviceName);
            mostProminentReason = BackendCreationException.Reason.VULKAN_DEVICE_VERSION_TOO_LOW;
            missingCapabilities.add("VULKAN_CORE_1_2");
         }

         if (mostProminentReason != null) {
            LOGGER.debug("Device [{}] is not suitable", deviceName);
            return new BackendCreationException("Device missing capabilities", mostProminentReason, missingCapabilities);
         } else {
            assert missingCapabilities.isEmpty();
            LOGGER.debug("Device [{}] is suitable", deviceName);
            return null;
         }
      }
   }

   private static VkDevice createDevice(final FeatureSet featureSet, final VulkanPhysicalDevice physicalDevice) throws BackendCreationException {
      MemoryStack stack = MemoryStack.stackPush();

      VkDevice var9;
      try {
         VkPhysicalDeviceFeatures2 deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();

         for (VulkanFeature requiredDeviceFeature : featureSet.features()) {
            requiredDeviceFeature.set(deviceFeatures, true, stack);
         }

         Int2IntMap queuesToCreate = physicalDevice.queueFamilyCreateInfoMap();
         Buffer queueCreationInfo = VkDeviceQueueCreateInfo.calloc(queuesToCreate.size(), stack);
         ObjectIterator enabledExtensionsBuffer = queuesToCreate.int2IntEntrySet().iterator();

         while (enabledExtensionsBuffer.hasNext()) {
            Entry familyCount = (Entry)enabledExtensionsBuffer.next();
            queueCreationInfo.sType$Default();
            queueCreationInfo.queueFamilyIndex(familyCount.getIntKey());
            queueCreationInfo.pQueuePriorities(stack.callocFloat(familyCount.getIntValue()));
            queueCreationInfo.position(queueCreationInfo.position() + 1);
         }

         queueCreationInfo.position(0);
         PointerBuffer enabledExtensionsBufferx = stack.callocPointer(featureSet.extensions().size());

         for (String name : featureSet.extensions()) {
            enabledExtensionsBufferx.put(stack.UTF8(name));
         }

         enabledExtensionsBufferx.flip();
         VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack).sType$Default();
         deviceCreateInfo.pNext(deviceFeatures.pNext());
         deviceCreateInfo.pQueueCreateInfos(queueCreationInfo);
         deviceCreateInfo.ppEnabledExtensionNames(enabledExtensionsBufferx);
         deviceCreateInfo.pEnabledFeatures(deviceFeatures.features());
         PointerBuffer pointer = stack.callocPointer(1);
         VulkanUtils.throwIfFailure(
            VK12.vkCreateDevice(physicalDevice.vkPhysicalDevice(), deviceCreateInfo, null, pointer),
            "Failed to create device",
            BackendCreationException.Reason.VULKAN_NO_DEVICE
         );
         var9 = new VkDevice(pointer.get(0), physicalDevice.vkPhysicalDevice(), deviceCreateInfo);
      } catch (Throwable var11) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (stack != null) {
         stack.close();
      }

      return var9;
   }
}
