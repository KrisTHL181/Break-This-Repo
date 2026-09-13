package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.api.commands.GpuQueryPool;
import java.nio.LongBuffer;
import java.util.OptionalLong;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;

public class VulkanQueryPool implements GpuQueryPool, Destroyable {
   private final VulkanDevice device;
   private final int size;
   private final long vkQueryPool;

   public VulkanQueryPool(final VulkanDevice device, final int size) {
      this.device = device;
      this.size = size;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         VkQueryPoolCreateInfo createInfo = VkQueryPoolCreateInfo.calloc(stack).sType$Default();
         createInfo.queryType(2);
         createInfo.queryCount(size);
         LongBuffer pointer = stack.callocLong(1);
         VulkanUtils.crashIfFailure(device, VK12.vkCreateQueryPool(device.vkDevice(), createInfo, null, pointer), "Cannot create query pool");
         this.vkQueryPool = pointer.get(0);
         VK12.vkResetQueryPool(device.vkDevice(), this.vkQueryPool, 0, size);
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

   @Override
   public int size() {
      return this.size;
   }

   @Override
   public OptionalLong getValue(final int index) {
      return this.getValues(index, 1)[0];
   }

   @Override
   public OptionalLong[] getValues(final int index, final int count) {
      if (index + count > this.size) {
         throw new IndexOutOfBoundsException(
            "getValues would read out-of-bounds for an array of " + count + " starting at " + index + ", when total size is " + this.size
         );
      }

      OptionalLong[] result = new OptionalLong[count];
      MemoryStack stack = MemoryStack.stackPush();

      try {
         LongBuffer values = stack.callocLong(2 * count);
         VulkanUtils.crashIfFailure(
            this.device, VK12.vkGetQueryPoolResults(this.device.vkDevice(), this.vkQueryPool, index, count, values, 16L, 5), "Cannot fetch query results"
         );

         for (int i = 0; i < count; i++) {
            if (values.get(i * 2 + 1) != 0L) {
               result[i] = OptionalLong.of(values.get(i * 2));
            } else {
               result[i] = OptionalLong.empty();
            }
         }
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

      return result;
   }

   protected long vkQueryPool() {
      return this.vkQueryPool;
   }

   @Override
   public void close() {
      this.device.createCommandEncoder().queueForDestroy(this);
   }

   @Override
   public void destroy() {
      VK12.vkDestroyQueryPool(this.device.vkDevice(), this.vkQueryPool, null);
   }
}
