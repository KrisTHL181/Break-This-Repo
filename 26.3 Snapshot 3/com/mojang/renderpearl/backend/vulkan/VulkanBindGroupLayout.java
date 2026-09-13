package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.api.GpuFormat;
import java.nio.LongBuffer;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding.Buffer;

public record VulkanBindGroupLayout(long handle, List<VulkanBindGroupLayout.Entry> entries) {
   public static final VulkanBindGroupLayout INVALID_LAYOUT = new VulkanBindGroupLayout(0L, List.of());

   public static VulkanBindGroupLayout create(final VulkanDevice device, final List<VulkanBindGroupLayout.Entry> entries, final String name) {
      MemoryStack stack = MemoryStack.stackPush();

      long layoutHandle;
      try {
         Buffer bindings = VkDescriptorSetLayoutBinding.calloc(entries.size(), stack);

         for (int i = 0; i < entries.size(); i++) {
            VkDescriptorSetLayoutBinding binding = VkDescriptorSetLayoutBinding.calloc(stack).descriptorType(switch (entries.get(i).type()) {
               case UNIFORM_BUFFER -> 6;
               case SAMPLED_IMAGE -> 1;
               case TEXEL_BUFFER -> 4;
            }).descriptorCount(1).binding(i).stageFlags(17);
            bindings.put(binding);
         }

         bindings.flip();
         VkDescriptorSetLayoutCreateInfo setCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().flags(1).pBindings(bindings);
         LongBuffer pointer = stack.callocLong(1);
         VulkanUtils.crashIfFailure(device, VK12.vkCreateDescriptorSetLayout(device.vkDevice(), setCreateInfo, null, pointer), "Can't set layout for " + name);
         layoutHandle = pointer.get(0);
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

      return new VulkanBindGroupLayout(layoutHandle, entries);
   }

   public record Entry(VulkanBindGroupLayout.VulkanBindGroupEntryType type, String name, @Nullable GpuFormat texelBufferFormat) {
   }

   public enum VulkanBindGroupEntryType {
      UNIFORM_BUFFER,
      SAMPLED_IMAGE,
      TEXEL_BUFFER;
   }
}
