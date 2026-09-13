package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public abstract class HeightProvider {
   private static final Codec<Either<VerticalAnchor, HeightProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(
      VerticalAnchor.CODEC, BuiltInRegistries.HEIGHT_PROVIDER_TYPE.byNameCodec().dispatch(HeightProvider::getType, HeightProviderType::codec)
   );
   public static final Codec<HeightProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap(
      p_161974_ -> (HeightProvider)p_161974_.map(ConstantHeight::of, p_161980_ -> p_161980_),
      p_161976_ -> p_161976_.getType() == HeightProviderType.CONSTANT ? Either.left(((ConstantHeight)p_161976_).getValue()) : Either.right(p_161976_)
   );

   public abstract int sample(RandomSource var1, WorldGenerationContext var2);

   public abstract HeightProviderType<?> getType();
}
