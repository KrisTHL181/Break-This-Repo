package com.mojang.blaze3d.resource;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResourceDescriptor<T> {
   T allocate();

   default void prepare(T p_395300_) {
   }

   void free(T var1);

   default boolean canUsePhysicalResource(ResourceDescriptor<?> p_395946_) {
      return this.equals(p_395946_);
   }
}
