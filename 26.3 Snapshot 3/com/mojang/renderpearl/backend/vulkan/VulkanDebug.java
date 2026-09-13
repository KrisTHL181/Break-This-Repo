package com.mojang.renderpearl.backend.vulkan;

import com.mojang.logging.LogUtils;
import java.lang.StackWalker.Option;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Set;
import net.minecraft.SharedConstants;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDebugUtilsLabelEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugUtilsObjectNameInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.slf4j.Logger;

public interface VulkanDebug {
   static VulkanDebug create(final int verbosity, final boolean wantsDebugLabels, final Set<String> availableExtensions, final Set<String> enabledExtensions) {
      if ((verbosity > 0 || wantsDebugLabels) && availableExtensions.contains("VK_EXT_debug_utils") || SharedConstants.DEBUG_FORCE_VULKAN_RENDERER) {
         enabledExtensions.add("VK_EXT_debug_utils");
         return new VulkanDebug.Enabled(verbosity, wantsDebugLabels);
      } else {
         return new VulkanDebug.Disabled();
      }
   }

   void chainCreateInfo(VkInstanceCreateInfo instanceCreateInfo, MemoryStack stack);

   void setup(VkInstance vkInstance);

   void setObjectName(VkDevice device, int objectType, long objectHandle, String label);

   void setObjectName(VkDevice device, int objectType, long objectHandle, Supplier<String> label);

   void beginDebugGroup(VkCommandBuffer buffer, Supplier<String> label);

   void endDebugGroup(VkCommandBuffer buffer);

   void destroy(VkInstance instance);

   boolean enabled();

   class Disabled implements VulkanDebug {
      @Override
      public void chainCreateInfo(final VkInstanceCreateInfo instanceCreateInfo, final MemoryStack stack) {
      }

      @Override
      public void setup(final VkInstance vkInstance) {
      }

      @Override
      public void setObjectName(final VkDevice device, final int objectType, final long objectHandle, final String label) {
      }

      @Override
      public void setObjectName(final VkDevice device, final int objectType, final long objectHandle, final Supplier<String> label) {
      }

      @Override
      public void beginDebugGroup(final VkCommandBuffer buffer, final Supplier<String> label) {
      }

      @Override
      public void endDebugGroup(final VkCommandBuffer buffer) {
      }

      @Override
      public void destroy(final VkInstance instance) {
      }

      @Override
      public boolean enabled() {
         return false;
      }
   }

   class Enabled implements VulkanDebug {
      private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(Option.RETAIN_CLASS_REFERENCE), 3);
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final int MESSAGE_TYPE_BITMASK = 7;
      private static final int[] DEBUG_LEVELS = new int[]{4096, 256, 16, 1};
      private final boolean wantsDebugLabels;
      private long messenger;
      private final int severityBitmask;

      public Enabled(final int verbosity, final boolean wantsDebugLabels) {
         this.wantsDebugLabels = wantsDebugLabels;
         int severityBitmask = 0;
         if (verbosity > 0) {
            for (int i = 0; i < Math.min(verbosity, DEBUG_LEVELS.length); i++) {
               severityBitmask |= DEBUG_LEVELS[i];
            }
         }

         this.severityBitmask = severityBitmask;
      }

      @Override
      public void chainCreateInfo(final VkInstanceCreateInfo instanceCreateInfo, final MemoryStack stack) {
         if (this.severityBitmask > 0) {
            instanceCreateInfo.pNext(
               VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                  .sType$Default()
                  .messageSeverity(this.severityBitmask)
                  .messageType(7)
                  .pfnUserCallback(this::onDebugMessage)
            );
         }
      }

      @Override
      public void setup(final VkInstance vkInstance) {
         MemoryStack stack = MemoryStack.stackPush();

         label43: {
            try {
               LongBuffer pointer = stack.callocLong(1);
               VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                  .sType$Default()
                  .messageSeverity(this.severityBitmask)
                  .messageType(7)
                  .pfnUserCallback(this::onDebugMessage);
               int result = EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(vkInstance, createInfo, null, pointer);
               if (result != 0) {
                  LOGGER.error("Error creating debug utils messenger: {}", VulkanUtils.resultToString(result));
                  break label43;
               }

               this.messenger = pointer.get(0);
            } catch (Throwable var7) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (stack != null) {
               stack.close();
            }

            return;
         }

         if (stack != null) {
            stack.close();
         }
      }

      @Override
      public void setObjectName(final VkDevice device, final int objectType, final long objectHandle, final String label) {
         if (this.wantsDebugLabels) {
            MemoryStack stack = MemoryStack.stackPush();

            try {
               ByteBuffer name = stack.UTF8(label);
               VkDebugUtilsObjectNameInfoEXT nameInfo = VkDebugUtilsObjectNameInfoEXT.calloc(stack)
                  .sType$Default()
                  .pObjectName(name)
                  .objectType(objectType)
                  .objectHandle(objectHandle);
               EXTDebugUtils.vkSetDebugUtilsObjectNameEXT(device, nameInfo);
            } catch (Throwable var10) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (stack != null) {
               stack.close();
            }
         }
      }

      @Override
      public void setObjectName(final VkDevice device, final int objectType, final long objectHandle, final Supplier<String> label) {
         if (this.wantsDebugLabels) {
            this.setObjectName(device, objectType, objectHandle, label.get());
         }
      }

      private int onDebugMessage(final int messageSeverity, final int messageTypes, final long pCallbackData, final long pUserData) {
         VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
         String message = callbackData.pMessageString();
         if ((messageSeverity & 16) != 0) {
            LOGGER.info("{}", message);
         } else if ((messageSeverity & 256) != 0) {
            LOGGER.warn("{}", message);
         } else {
            if ((messageSeverity & 4096) != 0) {
               if (message == null || !message.contains("vkDestroyInstance") && !message.contains("vkDestroyDevice")) {
                  String callStack = STACK_WALKER.walk(
                     s -> s.filter(
                           frame -> frame.getDeclaringClass() != VulkanDebug.Enabled.class
                              && !frame.getDeclaringClass().getPackageName().startsWith("org.lwjgl")
                        )
                        .limit(5L)
                        .map(frame -> "\t" + frame)
                        .collect(Collectors.joining("\n"))
                  );
                  LOGGER.error("{}\n{}", message, callStack);
               } else {
                  LOGGER.error("{}", message);
               }

               return 1;
            }

            LOGGER.debug("{}", message);
         }

         return 0;
      }

      @Override
      public void beginDebugGroup(final VkCommandBuffer buffer, final Supplier<String> label) {
         if (this.wantsDebugLabels) {
            MemoryStack stack = MemoryStack.stackPush();

            try {
               ByteBuffer name = stack.UTF8(label.get());
               VkDebugUtilsLabelEXT nameInfo = VkDebugUtilsLabelEXT.calloc(stack).sType$Default().pLabelName(name);
               EXTDebugUtils.vkCmdBeginDebugUtilsLabelEXT(buffer, nameInfo);
            } catch (Throwable var7) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (stack != null) {
               stack.close();
            }
         }
      }

      @Override
      public void endDebugGroup(final VkCommandBuffer buffer) {
         if (this.wantsDebugLabels) {
            EXTDebugUtils.vkCmdEndDebugUtilsLabelEXT(buffer);
         }
      }

      @Override
      public void destroy(final VkInstance instance) {
         if (this.messenger != 0L) {
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, this.messenger, null);
         }
      }

      @Override
      public boolean enabled() {
         return true;
      }
   }
}
