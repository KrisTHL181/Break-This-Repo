package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class LongJumpUtil {
   public static Optional<Vec3> calculateJumpVectorForAngle(Mob p_312589_, Vec3 p_311721_, float p_310433_, int p_310545_, boolean p_310611_) {
      Vec3 vec3 = p_312589_.position();
      Vec3 vec31 = new Vec3(p_311721_.x - vec3.x, 0.0, p_311721_.z - vec3.z).normalize().scale(0.5);
      Vec3 vec32 = p_311721_.subtract(vec31);
      Vec3 vec33 = vec32.subtract(vec3);
      float f = p_310545_ * (float) Math.PI / 180.0F;
      double d0 = Math.atan2(vec33.z, vec33.x);
      double d1 = vec33.subtract(0.0, vec33.y, 0.0).lengthSqr();
      double d2 = Math.sqrt(d1);
      double d3 = vec33.y;
      double d4 = p_312589_.getGravity();
      double d5 = Math.sin(2.0F * f);
      double d6 = Math.pow(Math.cos(f), 2.0);
      double d7 = Math.sin(f);
      double d8 = Math.cos(f);
      double d9 = Math.sin(d0);
      double d10 = Math.cos(d0);
      double d11 = d1 * d4 / (d2 * d5 - 2.0 * d3 * d6);
      if (d11 < 0.0) {
         return Optional.empty();
      }

      double d12 = Math.sqrt(d11);
      if (d12 > p_310433_) {
         return Optional.empty();
      }

      double d13 = d12 * d8;
      double d14 = d12 * d7;
      if (p_310611_) {
         int i = Mth.ceil(d2 / d13) * 2;
         double d15 = 0.0;
         Vec3 vec34 = null;
         EntityDimensions entitydimensions = p_312589_.getDimensions(Pose.LONG_JUMPING);

         for (int j = 0; j < i - 1; j++) {
            d15 += d2 / i;
            double d16 = d7 / d8 * d15 - Math.pow(d15, 2.0) * d4 / (2.0 * d11 * Math.pow(d8, 2.0));
            double d17 = d15 * d10;
            double d18 = d15 * d9;
            Vec3 vec35 = new Vec3(vec3.x + d17, vec3.y + d16, vec3.z + d18);
            if (vec34 != null && !isClearTransition(p_312589_, entitydimensions, vec34, vec35)) {
               return Optional.empty();
            }

            vec34 = vec35;
         }
      }

      return Optional.of(new Vec3(d13 * d10, d14, d13 * d9).scale(0.95F));
   }

   private static boolean isClearTransition(Mob p_310914_, EntityDimensions p_310152_, Vec3 p_313099_, Vec3 p_311144_) {
      Vec3 vec3 = p_311144_.subtract(p_313099_);
      double d0 = Math.min(p_310152_.width(), p_310152_.height());
      int i = Mth.ceil(vec3.length() / d0);
      Vec3 vec31 = vec3.normalize();
      Vec3 vec32 = p_313099_;

      for (int j = 0; j < i; j++) {
         vec32 = j == i - 1 ? p_311144_ : vec32.add(vec31.scale(d0 * 0.9F));
         if (!p_310914_.level().noCollision(p_310914_, p_310152_.makeBoundingBox(vec32))) {
            return false;
         }
      }

      return true;
   }
}
