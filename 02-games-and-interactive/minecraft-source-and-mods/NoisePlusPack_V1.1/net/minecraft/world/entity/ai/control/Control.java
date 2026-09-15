package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;

public interface Control {
   default float rotateTowards(float p_366413_, float p_365368_, float p_363676_) {
      float f = Mth.degreesDifference(p_366413_, p_365368_);
      float f1 = Mth.clamp(f, -p_363676_, p_363676_);
      return p_366413_ + f1;
   }
}
