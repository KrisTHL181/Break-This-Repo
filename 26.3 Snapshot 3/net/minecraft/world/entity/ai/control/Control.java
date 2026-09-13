package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;

public interface Control {
   default float rotateTowards(final float fromAngle, final float toAngle, final float maxRot) {
      float diff = Mth.degreesDifference(fromAngle, toAngle);
      float diffClamped = Mth.clamp(diff, -maxRot, maxRot);
      return fromAngle + diffClamped;
   }
}
