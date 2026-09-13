package com.mojang.blaze3d.resource;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GraphicsResourceAllocator {
   GraphicsResourceAllocator UNPOOLED = new GraphicsResourceAllocator() {
      @Override
      public <T> T acquire(ResourceDescriptor<T> p_369530_) {
         T t = p_369530_.allocate();
         p_369530_.prepare(t);
         return t;
      }

      @Override
      public <T> void release(ResourceDescriptor<T> p_367442_, T p_361994_) {
         p_367442_.free(p_361994_);
      }
   };

   <T> T acquire(ResourceDescriptor<T> var1);

   <T> void release(ResourceDescriptor<T> var1, T var2);
}
