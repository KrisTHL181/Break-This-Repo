package net.minecraft.world.attribute;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public interface LerpFunction<T> {
   static LerpFunction<Float> ofFloat() {
      return Mth::lerp;
   }

   static LerpFunction<Float> ofDegrees(float p_453410_) {
      return (p_457545_, p_451932_, p_457337_) -> {
         float f = Mth.wrapDegrees(p_457337_ - p_451932_);
         return Math.abs(f) >= p_453410_ ? p_457337_ : p_451932_ + p_457545_ * f;
      };
   }

   static <T> LerpFunction<T> ofConstant() {
      return (p_453262_, p_451500_, p_459969_) -> p_451500_;
   }

   static <T> LerpFunction<T> ofStep(float p_458669_) {
      return (p_459552_, p_456464_, p_457325_) -> p_459552_ >= p_458669_ ? p_457325_ : p_456464_;
   }

   static LerpFunction<Integer> ofColor() {
      return ARGB::srgbLerp;
   }

   T apply(float var1, T var2, T var3);
}
