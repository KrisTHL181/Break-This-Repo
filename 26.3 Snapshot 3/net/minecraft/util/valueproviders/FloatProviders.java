package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class FloatProviders {
   private static final Codec<Either<Float, FloatProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(
      Codec.FLOAT, BuiltInRegistries.FLOAT_PROVIDER_TYPE.byNameCodec().dispatch(FloatProvider::codec, t -> t)
   );
   public static final Codec<FloatProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap(
      either -> (FloatProvider)either.map(ConstantFloat::of, f -> f),
      f -> f instanceof ConstantFloat constantFloat ? Either.left(constantFloat.value()) : Either.right(f)
   );

   public static Codec<FloatProvider> codec(final float minValue, final float maxValue) {
      return CODEC.validate(
         value -> {
            if (value.min() < minValue) {
               return DataResult.error(() -> "Value provider too low: " + minValue + " [" + value.min() + "-" + value.max() + "]");
            } else {
               return value.max() > maxValue
                  ? DataResult.error(() -> "Value provider too high: " + maxValue + " [" + value.min() + "-" + value.max() + "]")
                  : DataResult.success(value);
            }
         }
      );
   }

   public static Codec<FloatProvider> codec(final float minValue) {
      return CODEC.validate(
         value -> value.min() < minValue
            ? DataResult.error(() -> "Value provider too low: " + minValue + " [" + value.min() + "-" + value.max() + "]")
            : DataResult.success(value)
      );
   }

   public static MapCodec<? extends FloatProvider> bootstrap(final Registry<MapCodec<? extends FloatProvider>> registry) {
      Registry.register(registry, "constant", ConstantFloat.MAP_CODEC);
      Registry.register(registry, "uniform", UniformFloat.MAP_CODEC);
      Registry.register(registry, "clamped_normal", ClampedNormalFloat.MAP_CODEC);
      return Registry.register(registry, "trapezoid", TrapezoidFloat.MAP_CODEC);
   }
}
