package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class FeatureSizeType<P extends FeatureSize> {
   public static final FeatureSizeType<TwoLayersFeatureSize> TWO_LAYERS_FEATURE_SIZE = register("two_layers_feature_size", TwoLayersFeatureSize.CODEC);
   public static final FeatureSizeType<ThreeLayersFeatureSize> THREE_LAYERS_FEATURE_SIZE = register("three_layers_feature_size", ThreeLayersFeatureSize.CODEC);
   private final MapCodec<P> codec;

   private static <P extends FeatureSize> FeatureSizeType<P> register(final String name, final MapCodec<P> codec) {
      return Registry.register(BuiltInRegistries.FEATURE_SIZE_TYPE, name, new FeatureSizeType<>(codec));
   }

   private FeatureSizeType(final MapCodec<P> codec) {
      this.codec = codec;
   }

   public MapCodec<P> codec() {
      return this.codec;
   }
}
