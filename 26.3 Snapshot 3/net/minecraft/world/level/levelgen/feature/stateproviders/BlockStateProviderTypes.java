package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public interface BlockStateProviderTypes {
   static MapCodec<? extends BlockStateProvider> bootstrap(final Registry<MapCodec<? extends BlockStateProvider>> registry) {
      Registry.register(registry, "copy_properties_provider", CopyPropertiesProvider.CODEC);
      Registry.register(registry, "dual_noise_provider", DualNoiseProvider.CODEC);
      Registry.register(registry, "noise_provider", NoiseProvider.CODEC);
      Registry.register(registry, "noise_threshold_provider", NoiseThresholdProvider.CODEC);
      Registry.register(registry, "random_block_provider", RandomBlockProvider.CODEC);
      Registry.register(registry, "randomized_int_state_provider", RandomizedIntStateProvider.CODEC);
      Registry.register(registry, "rotated_block_provider", RotatedBlockProvider.CODEC);
      Registry.register(registry, "rule_based_state_provider", RuleBasedStateProvider.CODEC);
      Registry.register(registry, "simple_state_provider", SimpleStateProvider.CODEC);
      return Registry.register(registry, "weighted_state_provider", WeightedStateProvider.CODEC);
   }
}
