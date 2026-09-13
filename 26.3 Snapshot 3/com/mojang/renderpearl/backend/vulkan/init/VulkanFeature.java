package com.mojang.renderpearl.backend.vulkan.init;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;

public record VulkanFeature(VulkanPNextStruct struct, String name, long offset) {
   public VulkanFeature(final VulkanPNextStruct struct, final String name) {
      String structClassName = struct.pNextStructClass().getSimpleName();
      if (!structClassName.contains("Features")) {
         throw new IllegalArgumentException(
            "Struct name \"" + structClassName + "\" does not contain \"Features\". All Vulkan features structs are expected to have \"Features\" in the name."
         );
      }

      this(struct, name, struct.fieldOffset(name));
   }

   public boolean get(final VkPhysicalDeviceFeatures2 features2) {
      return this.get(features2.address());
   }

   public boolean get(final long pNextChain) {
      long structAddr = this.struct.findStructInPNextChain(pNextChain);
      return structAddr == 0L ? false : this.getVkBool32(structAddr);
   }

   private boolean getVkBool32(final long pointer) {
      return MemoryUtil.memGetInt(pointer + this.offset) != 0;
   }

   private void putVkBool32(final boolean value, final long structAddr) {
      MemoryUtil.memPutInt(structAddr + this.offset, value ? 1 : 0);
   }

   public boolean set(final VkPhysicalDeviceFeatures2 features2, final boolean value) {
      return this.set(features2.address(), value);
   }

   private boolean set(final long pNextChain, final boolean value) {
      long structAddr = this.struct.findStructInPNextChain(pNextChain);
      if (structAddr == 0L) {
         return false;
      }

      this.putVkBool32(value, structAddr);
      return true;
   }

   public void set(final VkPhysicalDeviceFeatures2 features2, final boolean value, final MemoryStack stack) {
      this.set(features2.address(), value, stack);
   }

   public void set(final long pNextChain, final boolean value, final MemoryStack stack) {
      long structAddr = this.struct.findOrCreateStructInPNextChain(pNextChain, stack);
      this.putVkBool32(value, structAddr);
   }

   @Override
   public String toString() {
      return this.struct + "." + this.name;
   }
}
