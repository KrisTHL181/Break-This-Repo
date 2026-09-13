package net.minecraft.data.worldgen.placement;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.AquaticFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

public class AquaticPlacements {
   public static final ResourceKey<PlacedFeature> SEAGRASS_WARM = PlacementUtils.createKey("seagrass_warm");
   public static final ResourceKey<PlacedFeature> SEAGRASS_NORMAL = PlacementUtils.createKey("seagrass_normal");
   public static final ResourceKey<PlacedFeature> SEAGRASS_COLD = PlacementUtils.createKey("seagrass_cold");
   public static final ResourceKey<PlacedFeature> SEAGRASS_RIVER = PlacementUtils.createKey("seagrass_river");
   public static final ResourceKey<PlacedFeature> SEAGRASS_SWAMP = PlacementUtils.createKey("seagrass_swamp");
   public static final ResourceKey<PlacedFeature> SEAGRASS_DEEP_WARM = PlacementUtils.createKey("seagrass_deep_warm");
   public static final ResourceKey<PlacedFeature> SEAGRASS_DEEP = PlacementUtils.createKey("seagrass_deep");
   public static final ResourceKey<PlacedFeature> SEAGRASS_DEEP_COLD = PlacementUtils.createKey("seagrass_deep_cold");
   public static final ResourceKey<PlacedFeature> SEA_PICKLE = PlacementUtils.createKey("sea_pickle");
   public static final ResourceKey<PlacedFeature> KELP_COLD = PlacementUtils.createKey("kelp_cold");
   public static final ResourceKey<PlacedFeature> KELP_WARM = PlacementUtils.createKey("kelp_warm");
   public static final ResourceKey<PlacedFeature> WARM_OCEAN_VEGETATION = PlacementUtils.createKey("warm_ocean_vegetation");

   private static List<PlacementModifier> seagrassPlacement(int p_195234_) {
      return List.of(InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, CountPlacement.of(p_195234_), BiomeFilter.biome());
   }

   public static void bootstrap(BootstrapContext<PlacedFeature> p_330407_) {
      HolderGetter<ConfiguredFeature<?, ?>> holdergetter = p_330407_.lookup(Registries.CONFIGURED_FEATURE);
      Holder.Reference<ConfiguredFeature<?, ?>> reference = holdergetter.getOrThrow(AquaticFeatures.SEAGRASS_SHORT);
      Holder.Reference<ConfiguredFeature<?, ?>> reference1 = holdergetter.getOrThrow(AquaticFeatures.SEAGRASS_SLIGHTLY_LESS_SHORT);
      Holder.Reference<ConfiguredFeature<?, ?>> reference2 = holdergetter.getOrThrow(AquaticFeatures.SEAGRASS_MID);
      Holder.Reference<ConfiguredFeature<?, ?>> reference3 = holdergetter.getOrThrow(AquaticFeatures.SEAGRASS_TALL);
      Holder.Reference<ConfiguredFeature<?, ?>> reference4 = holdergetter.getOrThrow(AquaticFeatures.SEA_PICKLE);
      Holder.Reference<ConfiguredFeature<?, ?>> reference5 = holdergetter.getOrThrow(AquaticFeatures.KELP);
      Holder.Reference<ConfiguredFeature<?, ?>> reference6 = holdergetter.getOrThrow(AquaticFeatures.WARM_OCEAN_VEGETATION);
      PlacementUtils.register(p_330407_, SEAGRASS_WARM, reference, seagrassPlacement(80));
      PlacementUtils.register(p_330407_, SEAGRASS_NORMAL, reference, seagrassPlacement(48));
      PlacementUtils.register(p_330407_, SEAGRASS_COLD, reference, seagrassPlacement(32));
      PlacementUtils.register(p_330407_, SEAGRASS_RIVER, reference1, seagrassPlacement(48));
      PlacementUtils.register(p_330407_, SEAGRASS_SWAMP, reference2, seagrassPlacement(64));
      PlacementUtils.register(p_330407_, SEAGRASS_DEEP_WARM, reference3, seagrassPlacement(80));
      PlacementUtils.register(p_330407_, SEAGRASS_DEEP, reference3, seagrassPlacement(48));
      PlacementUtils.register(p_330407_, SEAGRASS_DEEP_COLD, reference3, seagrassPlacement(40));
      PlacementUtils.register(
         p_330407_,
         SEA_PICKLE,
         reference4,
         RarityFilter.onAverageOnceEvery(16),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_330407_,
         KELP_COLD,
         reference5,
         NoiseBasedCountPlacement.of(120, 80.0, 0.0),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_330407_,
         KELP_WARM,
         reference5,
         NoiseBasedCountPlacement.of(80, 80.0, 0.0),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_330407_,
         WARM_OCEAN_VEGETATION,
         reference6,
         NoiseBasedCountPlacement.of(20, 400.0, 0.0),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BiomeFilter.biome()
      );
   }
}
