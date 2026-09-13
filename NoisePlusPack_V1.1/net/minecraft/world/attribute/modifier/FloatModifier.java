package net.minecraft.world.attribute.modifier;

import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;

public interface FloatModifier<Argument> extends AttributeModifier<Float, Argument> {
   FloatModifier<FloatWithAlpha> ALPHA_BLEND = new FloatModifier<FloatWithAlpha>() {
      public Float apply(Float p_452517_, FloatWithAlpha p_456434_) {
         return Mth.lerp(p_456434_.alpha(), p_452517_, p_456434_.value());
      }

      @Override
      public Codec<FloatWithAlpha> argumentCodec(EnvironmentAttribute<Float> p_455636_) {
         return FloatWithAlpha.CODEC;
      }

      @Override
      public LerpFunction<FloatWithAlpha> argumentKeyframeLerp(EnvironmentAttribute<Float> p_454110_) {
         return (p_452311_, p_456765_, p_460775_) -> new FloatWithAlpha(
            Mth.lerp(p_452311_, p_456765_.value(), p_460775_.value()), Mth.lerp(p_452311_, p_456765_.alpha(), p_460775_.alpha())
         );
      }
   };
   FloatModifier<Float> ADD = Float::sum;
   FloatModifier<Float> SUBTRACT = (FloatModifier.Simple)(p_453793_, p_460749_) -> p_453793_ - p_460749_;
   FloatModifier<Float> MULTIPLY = (FloatModifier.Simple)(p_455467_, p_459500_) -> p_455467_ * p_459500_;
   FloatModifier<Float> MINIMUM = Math::min;
   FloatModifier<Float> MAXIMUM = Math::max;

   @FunctionalInterface
   interface Simple extends FloatModifier<Float> {
      @Override
      default Codec<Float> argumentCodec(EnvironmentAttribute<Float> p_452113_) {
         return Codec.FLOAT;
      }

      @Override
      default LerpFunction<Float> argumentKeyframeLerp(EnvironmentAttribute<Float> p_450154_) {
         return LerpFunction.ofFloat();
      }
   }
}
