package net.minecraft.data.worldgen.placement;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ClampedNormalInt;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceRelativeThresholdFilter;

public class CavePlacements {
   public static final ResourceKey<PlacedFeature> MONSTER_ROOM = PlacementUtils.createKey("monster_room");
   public static final ResourceKey<PlacedFeature> MONSTER_ROOM_DEEP = PlacementUtils.createKey("monster_room_deep");
   public static final ResourceKey<PlacedFeature> FOSSIL_UPPER = PlacementUtils.createKey("fossil_upper");
   public static final ResourceKey<PlacedFeature> FOSSIL_LOWER = PlacementUtils.createKey("fossil_lower");
   public static final ResourceKey<PlacedFeature> DRIPSTONE_CLUSTER = PlacementUtils.createKey("dripstone_cluster");
   public static final ResourceKey<PlacedFeature> LARGE_DRIPSTONE = PlacementUtils.createKey("large_dripstone");
   public static final ResourceKey<PlacedFeature> POINTED_DRIPSTONE = PlacementUtils.createKey("pointed_dripstone");
   public static final ResourceKey<PlacedFeature> UNDERWATER_MAGMA = PlacementUtils.createKey("underwater_magma");
   public static final ResourceKey<PlacedFeature> GLOW_LICHEN = PlacementUtils.createKey("glow_lichen");
   public static final ResourceKey<PlacedFeature> ROOTED_AZALEA_TREE = PlacementUtils.createKey("rooted_azalea_tree");
   public static final ResourceKey<PlacedFeature> CAVE_VINES = PlacementUtils.createKey("cave_vines");
   public static final ResourceKey<PlacedFeature> LUSH_CAVES_VEGETATION = PlacementUtils.createKey("lush_caves_vegetation");
   public static final ResourceKey<PlacedFeature> LUSH_CAVES_CLAY = PlacementUtils.createKey("lush_caves_clay");
   public static final ResourceKey<PlacedFeature> LUSH_CAVES_CEILING_VEGETATION = PlacementUtils.createKey("lush_caves_ceiling_vegetation");
   public static final ResourceKey<PlacedFeature> SPORE_BLOSSOM = PlacementUtils.createKey("spore_blossom");
   public static final ResourceKey<PlacedFeature> CLASSIC_VINES = PlacementUtils.createKey("classic_vines_cave_feature");
   public static final ResourceKey<PlacedFeature> AMETHYST_GEODE = PlacementUtils.createKey("amethyst_geode");
   public static final ResourceKey<PlacedFeature> SCULK_PATCH_DEEP_DARK = PlacementUtils.createKey("sculk_patch_deep_dark");
   public static final ResourceKey<PlacedFeature> SCULK_PATCH_ANCIENT_CITY = PlacementUtils.createKey("sculk_patch_ancient_city");
   public static final ResourceKey<PlacedFeature> SCULK_VEIN = PlacementUtils.createKey("sculk_vein");

   public static void bootstrap(BootstrapContext<PlacedFeature> p_328522_) {
      HolderGetter<ConfiguredFeature<?, ?>> holdergetter = p_328522_.lookup(Registries.CONFIGURED_FEATURE);
      Holder<ConfiguredFeature<?, ?>> holder = holdergetter.getOrThrow(CaveFeatures.MONSTER_ROOM);
      Holder<ConfiguredFeature<?, ?>> holder1 = holdergetter.getOrThrow(CaveFeatures.FOSSIL_COAL);
      Holder<ConfiguredFeature<?, ?>> holder2 = holdergetter.getOrThrow(CaveFeatures.FOSSIL_DIAMONDS);
      Holder<ConfiguredFeature<?, ?>> holder3 = holdergetter.getOrThrow(CaveFeatures.DRIPSTONE_CLUSTER);
      Holder<ConfiguredFeature<?, ?>> holder4 = holdergetter.getOrThrow(CaveFeatures.LARGE_DRIPSTONE);
      Holder<ConfiguredFeature<?, ?>> holder5 = holdergetter.getOrThrow(CaveFeatures.POINTED_DRIPSTONE);
      Holder<ConfiguredFeature<?, ?>> holder6 = holdergetter.getOrThrow(CaveFeatures.UNDERWATER_MAGMA);
      Holder<ConfiguredFeature<?, ?>> holder7 = holdergetter.getOrThrow(CaveFeatures.GLOW_LICHEN);
      Holder<ConfiguredFeature<?, ?>> holder8 = holdergetter.getOrThrow(CaveFeatures.ROOTED_AZALEA_TREE);
      Holder<ConfiguredFeature<?, ?>> holder9 = holdergetter.getOrThrow(CaveFeatures.CAVE_VINE);
      Holder<ConfiguredFeature<?, ?>> holder10 = holdergetter.getOrThrow(CaveFeatures.MOSS_PATCH);
      Holder<ConfiguredFeature<?, ?>> holder11 = holdergetter.getOrThrow(CaveFeatures.LUSH_CAVES_CLAY);
      Holder<ConfiguredFeature<?, ?>> holder12 = holdergetter.getOrThrow(CaveFeatures.MOSS_PATCH_CEILING);
      Holder<ConfiguredFeature<?, ?>> holder13 = holdergetter.getOrThrow(CaveFeatures.SPORE_BLOSSOM);
      Holder<ConfiguredFeature<?, ?>> holder14 = holdergetter.getOrThrow(VegetationFeatures.VINES);
      Holder<ConfiguredFeature<?, ?>> holder15 = holdergetter.getOrThrow(CaveFeatures.AMETHYST_GEODE);
      Holder<ConfiguredFeature<?, ?>> holder16 = holdergetter.getOrThrow(CaveFeatures.SCULK_PATCH_DEEP_DARK);
      Holder<ConfiguredFeature<?, ?>> holder17 = holdergetter.getOrThrow(CaveFeatures.SCULK_PATCH_ANCIENT_CITY);
      Holder<ConfiguredFeature<?, ?>> holder18 = holdergetter.getOrThrow(CaveFeatures.SCULK_VEIN);
      PlacementUtils.register(
         p_328522_,
         MONSTER_ROOM,
         holder,
         CountPlacement.of(10),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.top()),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         MONSTER_ROOM_DEEP,
         holder,
         CountPlacement.of(4),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(6), VerticalAnchor.absolute(-1)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         FOSSIL_UPPER,
         holder1,
         RarityFilter.onAverageOnceEvery(64),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.top()),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         FOSSIL_LOWER,
         holder2,
         RarityFilter.onAverageOnceEvery(64),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(-8)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         DRIPSTONE_CLUSTER,
         holder3,
         CountPlacement.of(UniformInt.of(48, 96)),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         LARGE_DRIPSTONE,
         holder4,
         CountPlacement.of(UniformInt.of(10, 48)),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         POINTED_DRIPSTONE,
         holder5,
         CountPlacement.of(UniformInt.of(192, 256)),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         CountPlacement.of(UniformInt.of(1, 5)),
         RandomOffsetPlacement.of(ClampedNormalInt.of(0.0F, 3.0F, -10, 10), ClampedNormalInt.of(0.0F, 0.6F, -2, 2)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         UNDERWATER_MAGMA,
         holder6,
         CountPlacement.of(UniformInt.of(44, 52)),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         SurfaceRelativeThresholdFilter.of(Heightmap.Types.OCEAN_FLOOR_WG, Integer.MIN_VALUE, -2),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         GLOW_LICHEN,
         holder7,
         CountPlacement.of(UniformInt.of(104, 157)),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         InSquarePlacement.spread(),
         SurfaceRelativeThresholdFilter.of(Heightmap.Types.OCEAN_FLOOR_WG, Integer.MIN_VALUE, -13),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         ROOTED_AZALEA_TREE,
         holder8,
         CountPlacement.of(UniformInt.of(1, 2)),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
         RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         CAVE_VINES,
         holder9,
         CountPlacement.of(188),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.hasSturdyFace(Direction.DOWN), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
         RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         LUSH_CAVES_VEGETATION,
         holder10,
         CountPlacement.of(125),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
         RandomOffsetPlacement.vertical(ConstantInt.of(1)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         LUSH_CAVES_CLAY,
         holder11,
         CountPlacement.of(62),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
         RandomOffsetPlacement.vertical(ConstantInt.of(1)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         LUSH_CAVES_CEILING_VEGETATION,
         holder12,
         CountPlacement.of(125),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
         RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         SPORE_BLOSSOM,
         holder13,
         CountPlacement.of(25),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
         RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         CLASSIC_VINES,
         holder14,
         CountPlacement.of(256),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         AMETHYST_GEODE,
         holder15,
         RarityFilter.onAverageOnceEvery(24),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(6), VerticalAnchor.absolute(30)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_328522_,
         SCULK_PATCH_DEEP_DARK,
         holder16,
         CountPlacement.of(ConstantInt.of(256)),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         BiomeFilter.biome()
      );
      PlacementUtils.register(p_328522_, SCULK_PATCH_ANCIENT_CITY, holder17);
      PlacementUtils.register(
         p_328522_,
         SCULK_VEIN,
         holder18,
         CountPlacement.of(UniformInt.of(204, 250)),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         BiomeFilter.biome()
      );
   }
}
