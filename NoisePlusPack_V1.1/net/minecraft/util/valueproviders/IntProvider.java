package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;

public abstract class IntProvider {
   private static final Codec<Either<Integer, IntProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(
      Codec.INT, BuiltInRegistries.INT_PROVIDER_TYPE.byNameCodec().dispatch(IntProvider::getType, IntProviderType::codec)
   );
   public static final Codec<IntProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap(
      p_146543_ -> (IntProvider)p_146543_.map(ConstantInt::of, p_146549_ -> p_146549_),
      p_146541_ -> p_146541_.getType() == IntProviderType.CONSTANT ? Either.left(((ConstantInt)p_146541_).getValue()) : Either.right(p_146541_)
   );
   public static final Codec<IntProvider> NON_NEGATIVE_CODEC = codec(0, Integer.MAX_VALUE);
   public static final Codec<IntProvider> POSITIVE_CODEC = codec(1, Integer.MAX_VALUE);

   public static Codec<IntProvider> codec(int p_146546_, int p_146547_) {
      return validateCodec(p_146546_, p_146547_, CODEC);
   }

   public static <T extends IntProvider> Codec<T> validateCodec(int p_330202_, int p_327757_, Codec<T> p_336105_) {
      return p_336105_.validate(p_326740_ -> validate(p_330202_, p_327757_, p_326740_));
   }

   private static <T extends IntProvider> DataResult<T> validate(int p_331801_, int p_334933_, T p_329862_) {
      if (p_329862_.getMinValue() < p_331801_) {
         return DataResult.error(() -> "Value provider too low: " + p_331801_ + " [" + p_329862_.getMinValue() + "-" + p_329862_.getMaxValue() + "]");
      } else {
         return p_329862_.getMaxValue() > p_334933_
            ? DataResult.error(() -> "Value provider too high: " + p_334933_ + " [" + p_329862_.getMinValue() + "-" + p_329862_.getMaxValue() + "]")
            : DataResult.success(p_329862_);
      }
   }

   public abstract int sample(RandomSource var1);

   public abstract int getMinValue();

   public abstract int getMaxValue();

   public abstract IntProviderType<?> getType();
}
