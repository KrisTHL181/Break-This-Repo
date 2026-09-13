package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import com.mojang.renderpearl.backend.vulkan.init.VulkanPNextStruct;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import org.lwjgl.vulkan.VkPhysicalDeviceDynamicRenderingFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceMultiDrawFeaturesEXT;
import org.lwjgl.vulkan.VkPhysicalDevicePortabilitySubsetFeaturesKHR;
import org.lwjgl.vulkan.VkPhysicalDeviceSynchronization2Features;
import org.lwjgl.vulkan.VkPhysicalDeviceVertexAttributeDivisorFeaturesEXT;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan11Features;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan12Features;

public class VulkanFeatureSets {
   public static final VulkanPNextStruct VK10_FEATURES_STRUCT = new VulkanPNextStruct(VkPhysicalDeviceFeatures2.class);
   public static final VulkanPNextStruct VK11_FEATURES_STRUCT = new VulkanPNextStruct(VkPhysicalDeviceVulkan11Features.class);
   public static final VulkanPNextStruct VK12_FEATURES_STRUCT = new VulkanPNextStruct(VkPhysicalDeviceVulkan12Features.class);
   public static final VulkanPNextStruct SYNC2_FEATURES_STRUCT = new VulkanPNextStruct(VkPhysicalDeviceSynchronization2Features.class);
   public static final VulkanPNextStruct DYNAMIC_RENDERING_FEATURES_STRUCT = new VulkanPNextStruct(VkPhysicalDeviceDynamicRenderingFeatures.class);
   public static final VulkanPNextStruct VERTEX_ATTRIB_DIVISOR_FEATURES_STRUCT = new VulkanPNextStruct(VkPhysicalDeviceVertexAttributeDivisorFeaturesEXT.class);
   public static final FeatureSet REQUIRED_FEATURESET = new FeatureSet(
      "Minecraft base required",
      Set.of("VK_KHR_dynamic_rendering", "VK_KHR_push_descriptor", "VK_KHR_synchronization2", "VK_EXT_vertex_attribute_divisor", "VK_KHR_swapchain"),
      Set.of(
         new VulkanFeature(VK10_FEATURES_STRUCT, "multiDrawIndirect"),
         new VulkanFeature(VK10_FEATURES_STRUCT, "fillModeNonSolid"),
         new VulkanFeature(VK10_FEATURES_STRUCT, "samplerAnisotropy"),
         new VulkanFeature(VK11_FEATURES_STRUCT, "shaderDrawParameters"),
         new VulkanFeature(VK12_FEATURES_STRUCT, "timelineSemaphore"),
         new VulkanFeature(VK12_FEATURES_STRUCT, "hostQueryReset"),
         new VulkanFeature(SYNC2_FEATURES_STRUCT, "synchronization2"),
         new VulkanFeature(DYNAMIC_RENDERING_FEATURES_STRUCT, "dynamicRendering"),
         new VulkanFeature(VERTEX_ATTRIB_DIVISOR_FEATURES_STRUCT, "vertexAttributeInstanceRateDivisor")
      )
   );
   public static final VulkanPNextStruct PORTABILITY_SUBSET_FEATURES_STRUCT = new VulkanPNextStruct(VkPhysicalDevicePortabilitySubsetFeaturesKHR.class);
   public static final FeatureSet PORTABILITY_SUBSET_FEATURESET = new FeatureSet(
      "Vulkan Portability Subset", Set.of("VK_KHR_portability_subset"), Set.of(new VulkanFeature(PORTABILITY_SUBSET_FEATURES_STRUCT, "triangleFans"))
   );
   public static final VulkanPNextStruct MULTI_DRAW_FEATURES_STRUCT = new VulkanPNextStruct(VkPhysicalDeviceMultiDrawFeaturesEXT.class);
   public static final FeatureSet MULTI_DRAW_FEATURESET = new FeatureSet(
      "EXT MultiDraw", Set.of("VK_EXT_multi_draw"), Set.of(new VulkanFeature(MULTI_DRAW_FEATURES_STRUCT, "multiDraw"))
   );
   public static final FeatureSet AMD_BUFFER_MARKER_FEATURESET = new FeatureSet("AMD Buffer Marker", Set.of("VK_AMD_buffer_marker"), Set.of());
   public static final FeatureSet NV_DIAGNOSTIC_CHECKPOINT_FEATURESET = new FeatureSet(
      "NV Diagnostic Checkpoint", Set.of("VK_NV_device_diagnostic_checkpoints"), Set.of(), device -> !AMD_BUFFER_MARKER_FEATURESET.isSupported(device)
   );

   public static Set<FeatureSet> requiredFeatureSets() {
      return ObjectOpenHashSet.of(REQUIRED_FEATURESET);
   }

   public static Set<FeatureSet> requiredIfExtensionsAvailableFeatureSets() {
      return ObjectOpenHashSet.of(PORTABILITY_SUBSET_FEATURESET);
   }

   public static Set<FeatureSet> optionalFeatureSets() {
      return ObjectOpenHashSet.of(NV_DIAGNOSTIC_CHECKPOINT_FEATURESET, AMD_BUFFER_MARKER_FEATURESET, MULTI_DRAW_FEATURESET);
   }

   public static void bootstrap() {
   }
}
