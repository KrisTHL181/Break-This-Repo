package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import java.nio.LongBuffer;
import java.util.OptionalDouble;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

public class VulkanGpuSampler extends GpuSampler implements Destroyable {
   private final long vkSampler;
   private final VulkanDevice device;
   private final AddressMode addressModeU;
   private final AddressMode addressModeV;
   private final FilterMode minFilter;
   private final FilterMode magFilter;
   private final int maxAnisotropy;
   private final OptionalDouble maxLod;
   private boolean closed;

   public VulkanGpuSampler(
      final VulkanDevice device,
      final AddressMode addressModeU,
      final AddressMode addressModeV,
      final FilterMode minFilter,
      final FilterMode magFilter,
      final int maxAnisotropy,
      final OptionalDouble maxLod
   ) {
      this.device = device;
      this.addressModeU = addressModeU;
      this.addressModeV = addressModeV;
      this.minFilter = minFilter;
      this.magFilter = magFilter;
      this.maxAnisotropy = maxAnisotropy;
      this.maxLod = maxLod;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         VkSamplerCreateInfo createInfo = VkSamplerCreateInfo.calloc(stack).sType$Default();
         createInfo.magFilter(VulkanConst.toVk(magFilter));
         createInfo.minFilter(VulkanConst.toVk(minFilter));
         createInfo.mipmapMode(maxLod.orElse(1000.0) > 0.25 ? 1 : 0);
         createInfo.addressModeU(VulkanConst.toVk(addressModeU));
         createInfo.addressModeV(VulkanConst.toVk(addressModeV));
         createInfo.mipLodBias(0.0F);
         createInfo.maxLod(Math.max(0.25F, (float)maxLod.orElse(1000.0)));
         createInfo.anisotropyEnable(maxAnisotropy > 1);
         createInfo.maxAnisotropy(maxAnisotropy);
         LongBuffer pointer = stack.callocLong(1);
         VulkanUtils.crashIfFailure(device, VK12.vkCreateSampler(device.vkDevice(), createInfo, null, pointer), "Can't create sampler");
         this.vkSampler = pointer.get(0);
      } catch (Throwable var12) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var11) {
               var12.addSuppressed(var11);
            }
         }

         throw var12;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Override
   public void destroy() {
      VK12.vkDestroySampler(this.device.vkDevice(), this.vkSampler, null);
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         this.device.createCommandEncoder().queueForDestroy(this);
      }
   }

   @Override
   public AddressMode getAddressModeU() {
      return this.addressModeU;
   }

   @Override
   public AddressMode getAddressModeV() {
      return this.addressModeV;
   }

   @Override
   public FilterMode getMinFilter() {
      return this.minFilter;
   }

   @Override
   public FilterMode getMagFilter() {
      return this.magFilter;
   }

   @Override
   public int getMaxAnisotropy() {
      return this.maxAnisotropy;
   }

   @Override
   public OptionalDouble getMaxLod() {
      return this.maxLod;
   }

   public long vkSampler() {
      return this.vkSampler;
   }
}
