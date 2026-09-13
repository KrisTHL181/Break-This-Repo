package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuDeviceLossException;
import com.mojang.renderpearl.backend.vulkan.checkpoints.CheckpointExtension;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Set;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkExtensionProperties.Buffer;

public class VulkanUtils {
   public static final Set<VulkanUtils.DeviceUUID> KNOWN_PROBLEMATIC_DEVICES = Set.of(
      new VulkanUtils.DeviceUUID(14, 32902, 338),
      new VulkanUtils.DeviceUUID(14, 32902, 341),
      new VulkanUtils.DeviceUUID(14, 32902, 343),
      new VulkanUtils.DeviceUUID(14, 32902, 346),
      new VulkanUtils.DeviceUUID(14, 32902, 354),
      new VulkanUtils.DeviceUUID(14, 32902, 358),
      new VulkanUtils.DeviceUUID(14, 32902, 362),
      new VulkanUtils.DeviceUUID(14, 32902, 1026),
      new VulkanUtils.DeviceUUID(14, 32902, 1030),
      new VulkanUtils.DeviceUUID(14, 32902, 1034),
      new VulkanUtils.DeviceUUID(14, 32902, 1035),
      new VulkanUtils.DeviceUUID(14, 32902, 1038),
      new VulkanUtils.DeviceUUID(14, 32902, 1042),
      new VulkanUtils.DeviceUUID(14, 32902, 1046),
      new VulkanUtils.DeviceUUID(14, 32902, 1050),
      new VulkanUtils.DeviceUUID(14, 32902, 1051),
      new VulkanUtils.DeviceUUID(14, 32902, 1054),
      new VulkanUtils.DeviceUUID(14, 32902, 1058),
      new VulkanUtils.DeviceUUID(14, 32902, 1062),
      new VulkanUtils.DeviceUUID(14, 32902, 1066),
      new VulkanUtils.DeviceUUID(14, 32902, 1067),
      new VulkanUtils.DeviceUUID(14, 32902, 1070),
      new VulkanUtils.DeviceUUID(14, 32902, 2562),
      new VulkanUtils.DeviceUUID(14, 32902, 2566),
      new VulkanUtils.DeviceUUID(14, 32902, 2570),
      new VulkanUtils.DeviceUUID(14, 32902, 2571),
      new VulkanUtils.DeviceUUID(14, 32902, 2574),
      new VulkanUtils.DeviceUUID(14, 32902, 2578),
      new VulkanUtils.DeviceUUID(14, 32902, 2582),
      new VulkanUtils.DeviceUUID(14, 32902, 2586),
      new VulkanUtils.DeviceUUID(14, 32902, 2587),
      new VulkanUtils.DeviceUUID(14, 32902, 2590),
      new VulkanUtils.DeviceUUID(14, 32902, 2594),
      new VulkanUtils.DeviceUUID(14, 32902, 2598),
      new VulkanUtils.DeviceUUID(14, 32902, 2602),
      new VulkanUtils.DeviceUUID(14, 32902, 2603),
      new VulkanUtils.DeviceUUID(14, 32902, 2606),
      new VulkanUtils.DeviceUUID(14, 32902, 3362),
      new VulkanUtils.DeviceUUID(14, 32902, 3366),
      new VulkanUtils.DeviceUUID(14, 32902, 3370),
      new VulkanUtils.DeviceUUID(14, 32902, 3371),
      new VulkanUtils.DeviceUUID(14, 32902, 3374),
      new VulkanUtils.DeviceUUID(14, 32902, 3888),
      new VulkanUtils.DeviceUUID(14, 32902, 3889),
      new VulkanUtils.DeviceUUID(14, 32902, 3890),
      new VulkanUtils.DeviceUUID(14, 32902, 3891),
      new VulkanUtils.DeviceUUID(14, 32902, 5638),
      new VulkanUtils.DeviceUUID(14, 32902, 5650),
      new VulkanUtils.DeviceUUID(14, 32902, 5654),
      new VulkanUtils.DeviceUUID(14, 32902, 5662),
      new VulkanUtils.DeviceUUID(14, 32902, 5666),
      new VulkanUtils.DeviceUUID(14, 32902, 5670),
      new VulkanUtils.DeviceUUID(14, 32902, 5674),
      new VulkanUtils.DeviceUUID(14, 32902, 5675),
      new VulkanUtils.DeviceUUID(14, 32902, 8880),
      new VulkanUtils.DeviceUUID(14, 32902, 8881),
      new VulkanUtils.DeviceUUID(14, 32902, 8882),
      new VulkanUtils.DeviceUUID(14, 32902, 8883)
   );

   public static void throwIfFailure(final int result, final String message, final BackendCreationException.Reason reason) throws BackendCreationException {
      if (result < 0) {
         throw new BackendCreationException(resultToString(result) + ": " + message, reason);
      }
   }

   public static void crashIfFailure(final VulkanDevice device, final int result, final String message) {
      if (result < 0) {
         String error = resultToString(result) + ": " + message;
         if (result == -4) {
            List<CheckpointExtension.QueueCheckpoints> checkpoints = device.checkpointExtension().retrieveCheckpoints(true);
            throw new GpuDeviceLossException(error + "\n" + formatCheckpoints(checkpoints));
         } else {
            throw new IllegalStateException(error);
         }
      }
   }

   public static String formatCheckpoints(final List<CheckpointExtension.QueueCheckpoints> queueCheckpoints) {
      StringBuilder result = new StringBuilder();

      for (CheckpointExtension.QueueCheckpoints queue : queueCheckpoints) {
         result.append("Queue 0x").append(Long.toHexString(queue.queue())).append('\n');

         for (CheckpointExtension.StageCheckpoint checkpoint : queue.checkpoints()) {
            result.append(' ')
               .append(pipelineStageToString(checkpoint.stage()))
               .append(" = ")
               .append(checkpoint.type())
               .append(' ')
               .append(checkpoint.label())
               .append('\n');
         }
      }

      return result.toString();
   }

   public static String pipelineStageToString(final long pipelineStage) {
      if (pipelineStage == 0L) {
         return "NONE";
      } else if (pipelineStage == 1L) {
         return "TOP_OF_PIPE";
      } else if (pipelineStage == 2L) {
         return "DRAW_INDIRECT";
      } else if (pipelineStage == 4L) {
         return "VERTEX_INPUT";
      } else if (pipelineStage == 8L) {
         return "VERTEX_SHADER";
      } else if (pipelineStage == 16L) {
         return "TESSELLATION_CONTROL_SHADER";
      } else if (pipelineStage == 32L) {
         return "TESSELLATION_EVALUATION_SHADER";
      } else if (pipelineStage == 64L) {
         return "GEOMETRY_SHADER";
      } else if (pipelineStage == 128L) {
         return "FRAGMENT_SHADER";
      } else if (pipelineStage == 256L) {
         return "EARLY_FRAGMENT_TESTS";
      } else if (pipelineStage == 512L) {
         return "LATE_FRAGMENT_TESTS";
      } else if (pipelineStage == 1024L) {
         return "COLOR_ATTACHMENT_OUTPUT";
      } else if (pipelineStage == 2048L) {
         return "COMPUTE_SHADER";
      } else if (pipelineStage == 4096L) {
         return "TRANSFER";
      } else if (pipelineStage == 8192L) {
         return "BOTTOM_OF_PIPE";
      } else if (pipelineStage == 16384L) {
         return "HOST";
      } else if (pipelineStage == 32768L) {
         return "ALL_GRAPHICS";
      } else if (pipelineStage == 65536L) {
         return "ALL_COMMANDS";
      } else if (pipelineStage == 131072L) {
         return "COMMAND_PREPROCESS";
      } else if (pipelineStage == 262144L) {
         return "CONDITIONAL_RENDERING";
      } else if (pipelineStage == 524288L) {
         return "TASK_SHADER";
      } else if (pipelineStage == 1048576L) {
         return "MESH_SHADER";
      } else if (pipelineStage == 2097152L) {
         return "RAY_TRACING_SHADER";
      } else if (pipelineStage == 4194304L) {
         return "FRAGMENT_SHADING_RATE_ATTACHMENT";
      } else if (pipelineStage == 8388608L) {
         return "FRAGMENT_DENSITY_PROCESS";
      } else if (pipelineStage == 16777216L) {
         return "TRANSFORM_FEEDBACK";
      } else if (pipelineStage == 33554432L) {
         return "ACCELERATION_STRUCTURE_BUILD";
      } else if (pipelineStage == 4294967296L) {
         return "COPY";
      } else if (pipelineStage == 8589934592L) {
         return "RESOLVE";
      } else if (pipelineStage == 17179869184L) {
         return "BLIT";
      } else if (pipelineStage == 34359738368L) {
         return "CLEAR";
      } else if (pipelineStage == 68719476736L) {
         return "INDEX_INPUT";
      } else if (pipelineStage == 137438953472L) {
         return "VERTEX_ATTRIBUTE_INPUT";
      } else {
         return pipelineStage == 274877906944L ? "PRE_RASTERIZATION_SHADERS" : "0x" + Long.toHexString(pipelineStage);
      }
   }

   public static String resultToString(final int error) {
      return switch (error) {
         case -1000257000 -> "VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS";
         case -1000161000 -> "VK_ERROR_FRAGMENTATION";
         case -1000072003 -> "VK_ERROR_INVALID_EXTERNAL_HANDLE";
         case -1000069000 -> "VK_ERROR_OUT_OF_POOL_MEMORY";
         case -1000001004 -> "VK_ERROR_OUT_OF_DATE_KHR";
         case -1000000001 -> "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR";
         case -1000000000 -> "VK_ERROR_SURFACE_LOST_KHR";
         case -13 -> "VK_ERROR_UNKNOWN";
         case -12 -> "VK_ERROR_FRAGMENTED_POOL";
         case -11 -> "VK_ERROR_FORMAT_NOT_SUPPORTED";
         case -10 -> "VK_ERROR_TOO_MANY_OBJECTS";
         case -9 -> "VK_ERROR_INCOMPATIBLE_DRIVER";
         case -8 -> "VK_ERROR_FEATURE_NOT_PRESENT";
         case -7 -> "VK_ERROR_EXTENSION_NOT_PRESENT";
         case -6 -> "VK_ERROR_LAYER_NOT_PRESENT";
         case -5 -> "VK_ERROR_MEMORY_MAP_FAILED";
         case -4 -> "VK_ERROR_DEVICE_LOST";
         case -3 -> "VK_ERROR_INITIALIZATION_FAILED";
         case -2 -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
         case -1 -> "VK_ERROR_OUT_OF_HOST_MEMORY";
         case 0 -> "VK_SUCCESS";
         case 1 -> "VK_NOT_READY";
         case 2 -> "VK_TIMEOUT";
         case 3 -> "VK_EVENT_SET";
         case 4 -> "VK_EVENT_RESET";
         case 5 -> "VK_INCOMPLETE";
         case 1000001003 -> "VK_SUBOPTIMAL_KHR";
         default -> "0x" + Integer.toHexString(error);
      };
   }

   public static boolean hasAllBits(final int bitfield, final int bitmask) {
      return (bitfield & bitmask) == bitmask;
   }

   public static boolean hasAllBits(final long bitfield, final long bitmask) {
      return (bitfield & bitmask) == bitmask;
   }

   public static boolean hasAnyBit(final int bitfield, final int bitmask) {
      return (bitfield & bitmask) != 0;
   }

   public static boolean hasAnyBit(final long bitfield, final long bitmask) {
      return (bitfield & bitmask) != 0L;
   }

   public static boolean hasNoBit(final int bitfield, final int bitmask) {
      return (bitfield & bitmask) == 0;
   }

   public static boolean hasNoBit(final long bitfield, final long bitmask) {
      return (bitfield & bitmask) == 0L;
   }

   public static VkClearColorValue putArgb(final VkClearColorValue vkClearColor, final Vector4fc argb) {
      vkClearColor.float32(0, argb.x());
      vkClearColor.float32(1, argb.y());
      vkClearColor.float32(2, argb.z());
      vkClearColor.float32(3, argb.w());
      return vkClearColor;
   }

   public static Set<String> enumerateExtensions(final VkPhysicalDevice vkPhysicalDevice) throws BackendCreationException {
      Set<String> extensions = new ObjectOpenHashSet();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer extensionCount = stack.callocInt(1);
         throwIfFailure(
            VK12.vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, (String)null, extensionCount, null),
            "Failed to get number of device extension properties",
            BackendCreationException.Reason.VULKAN_NO_DEVICE
         );
         Buffer vkDeviceExtensions = VkExtensionProperties.calloc(extensionCount.get(0));

         try {
            throwIfFailure(
               VK12.vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, (String)null, extensionCount, vkDeviceExtensions),
               "Failed to get extension properties",
               BackendCreationException.Reason.VULKAN_NO_DEVICE
            );
            vkDeviceExtensions.limit(extensionCount.get(0));

            for (int i = 0; i < vkDeviceExtensions.limit(); i++) {
               vkDeviceExtensions.position(i);
               extensions.add(vkDeviceExtensions.extensionNameString());
            }
         } catch (Throwable var9) {
            if (vkDeviceExtensions != null) {
               try {
                  vkDeviceExtensions.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (vkDeviceExtensions != null) {
            vkDeviceExtensions.close();
         }
      } catch (Throwable var10) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (stack != null) {
         stack.close();
      }

      return extensions;
   }

   public static VkPhysicalDeviceFeatures2 enumerateFeatures(
      final VkPhysicalDevice vkPhysicalDevice, final Set<VulkanFeature> features, final MemoryStack stack
   ) {
      VkPhysicalDeviceFeatures2 deviceFeatures = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();

      for (VulkanFeature requiredDeviceFeature : features) {
         requiredDeviceFeature.struct().findOrCreateStructInPNextChain(deviceFeatures, stack);
      }

      VK12.vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, deviceFeatures);
      return deviceFeatures;
   }

   public record DeviceUUID(int driverID, int vendorID, int deviceID) {
   }
}
