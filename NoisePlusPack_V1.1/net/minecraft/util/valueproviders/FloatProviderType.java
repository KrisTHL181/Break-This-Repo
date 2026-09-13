package net.minecraft.util.valueproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public interface FloatProviderType<P extends FloatProvider> {
   FloatProviderType<ConstantFloat> CONSTANT = register("constant", ConstantFloat.CODEC);
   FloatProviderType<UniformFloat> UNIFORM = register("uniform", UniformFloat.CODEC);
   FloatProviderType<ClampedNormalFloat> CLAMPED_NORMAL = register("clamped_normal", ClampedNormalFloat.CODEC);
   FloatProviderType<TrapezoidFloat> TRAPEZOID = register("trapezoid", TrapezoidFloat.CODEC);

   MapCodec<P> codec();

   static <P extends FloatProvider> FloatProviderType<P> register(String p_146527_, MapCodec<P> p_331093_) {
      return Registry.register(BuiltInRegistries.FLOAT_PROVIDER_TYPE, p_146527_, () -> p_331093_);
   }
}
