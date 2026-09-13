package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;

public abstract class FloatProvider implements SampledFloat {
   private static final Codec<Either<Float, FloatProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(
      Codec.FLOAT, BuiltInRegistries.FLOAT_PROVIDER_TYPE.byNameCodec().dispatch(FloatProvider::getType, FloatProviderType::codec)
   );
   public static final Codec<FloatProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap(
      p_146515_ -> (FloatProvider)p_146515_.map(ConstantFloat::of, p_146518_ -> p_146518_),
      p_146513_ -> p_146513_.getType() == FloatProviderType.CONSTANT ? Either.left(((ConstantFloat)p_146513_).getValue()) : Either.right(p_146513_)
   );

   public static Codec<FloatProvider> codec(float p_146506_, float p_146507_) {
      return CODEC.validate(
         p_274942_ -> {
            if (p_274942_.getMinValue() < p_146506_) {
               return DataResult.error(() -> "Value provider too low: " + p_146506_ + " [" + p_274942_.getMinValue() + "-" + p_274942_.getMaxValue() + "]");
            } else {
               return p_274942_.getMaxValue() > p_146507_
                  ? DataResult.error(() -> "Value provider too high: " + p_146507_ + " [" + p_274942_.getMinValue() + "-" + p_274942_.getMaxValue() + "]")
                  : DataResult.success(p_274942_);
            }
         }
      );
   }

   public abstract float getMinValue();

   public abstract float getMaxValue();

   public abstract FloatProviderType<?> getType();
}
