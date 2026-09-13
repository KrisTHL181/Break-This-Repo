package net.minecraft.world.attribute;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public interface LerpFunction<T> {
   LerpFunction<?> CONSTANT = ofStep(1.0F);

   static LerpFunction<Float> ofFloat() {
      return Mth::lerp;
   }

   static LerpFunction<Integer> ofInteger() {
      return Mth::lerpInt;
   }

   static LerpFunction<Float> ofDegrees(final float maxDelta) {
      return (alpha, from, to) -> {
         float delta = Mth.wrapDegrees(to - from);
         return Math.abs(delta) >= maxDelta ? to : from + alpha * delta;
      };
   }

   static <T> LerpFunction<T> ofConstant() {
      return (LerpFunction<T>)CONSTANT;
   }

   static <T> LerpFunction<T> ofStep(final float threshold) {
      return (alpha, from, to) -> alpha >= threshold ? to : from;
   }

   static LerpFunction<Integer> ofColor() {
      return ARGB::srgbLerp;
   }

   T apply(float alpha, T from, T to);
}
