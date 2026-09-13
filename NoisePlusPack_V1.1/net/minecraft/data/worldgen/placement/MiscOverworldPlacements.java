package net.minecraft.data.worldgen.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.heightproviders.VeryBiasedToBottomHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceRelativeThresholdFilter;
import net.minecraft.world.level.material.Fluids;

public class MiscOverworldPlacements {
   public static final ResourceKey<PlacedFeature> ICE_SPIKE = PlacementUtils.createKey("ice_spike");
   public static final ResourceKey<PlacedFeature> ICE_PATCH = PlacementUtils.createKey("ice_patch");
   public static final ResourceKey<PlacedFeature> FOREST_ROCK = PlacementUtils.createKey("forest_rock");
   public static final ResourceKey<PlacedFeature> ICEBERG_PACKED = PlacementUtils.createKey("iceberg_packed");
   public static final ResourceKey<PlacedFeature> ICEBERG_BLUE = PlacementUtils.createKey("iceberg_blue");
   public static final ResourceKey<PlacedFeature> BLUE_ICE = PlacementUtils.createKey("blue_ice");
   public static final ResourceKey<PlacedFeature> LAKE_LAVA_UNDERGROUND = PlacementUtils.createKey("lake_lava_underground");
   public static final ResourceKey<PlacedFeature> LAKE_LAVA_SURFACE = PlacementUtils.createKey("lake_lava_surface");
   public static final ResourceKey<PlacedFeature> DISK_CLAY = PlacementUtils.createKey("disk_clay");
   public static final ResourceKey<PlacedFeature> DISK_GRAVEL = PlacementUtils.createKey("disk_gravel");
   public static final ResourceKey<PlacedFeature> DISK_SAND = PlacementUtils.createKey("disk_sand");
   public static final ResourceKey<PlacedFeature> DISK_GRASS = PlacementUtils.createKey("disk_grass");
   public static final ResourceKey<PlacedFeature> FREEZE_TOP_LAYER = PlacementUtils.createKey("freeze_top_layer");
   public static final ResourceKey<PlacedFeature> VOID_START_PLATFORM = PlacementUtils.createKey("void_start_platform");
   public static final ResourceKey<PlacedFeature> DESERT_WELL = PlacementUtils.createKey("desert_well");
   public static final ResourceKey<PlacedFeature> SPRING_LAVA = PlacementUtils.createKey("spring_lava");
   public static final ResourceKey<PlacedFeature> SPRING_LAVA_FROZEN = PlacementUtils.createKey("spring_lava_frozen");
   public static final ResourceKey<PlacedFeature> SPRING_WATER = PlacementUtils.createKey("spring_water");

   public static void bootstrap(BootstrapContext<PlacedFeature> p_331286_) {
      HolderGetter<ConfiguredFeature<?, ?>> holdergetter = p_331286_.lookup(Registries.CONFIGURED_FEATURE);
      Holder<ConfiguredFeature<?, ?>> holder = holdergetter.getOrThrow(MiscOverworldFeatures.ICE_SPIKE);
      Holder<ConfiguredFeature<?, ?>> holder1 = holdergetter.getOrThrow(MiscOverworldFeatures.ICE_PATCH);
      Holder<ConfiguredFeature<?, ?>> holder2 = holdergetter.getOrThrow(MiscOverworldFeatures.FOREST_ROCK);
      Holder<ConfiguredFeature<?, ?>> holder3 = holdergetter.getOrThrow(MiscOverworldFeatures.ICEBERG_PACKED);
      Holder<ConfiguredFeature<?, ?>> holder4 = holdergetter.getOrThrow(MiscOverworldFeatures.ICEBERG_BLUE);
      Holder<ConfiguredFeature<?, ?>> holder5 = holdergetter.getOrThrow(MiscOverworldFeatures.BLUE_ICE);
      Holder<ConfiguredFeature<?, ?>> holder6 = holdergetter.getOrThrow(MiscOverworldFeatures.LAKE_LAVA);
      Holder<ConfiguredFeature<?, ?>> holder7 = holdergetter.getOrThrow(MiscOverworldFeatures.DISK_CLAY);
      Holder<ConfiguredFeature<?, ?>> holder8 = holdergetter.getOrThrow(MiscOverworldFeatures.DISK_GRAVEL);
      Holder<ConfiguredFeature<?, ?>> holder9 = holdergetter.getOrThrow(MiscOverworldFeatures.DISK_SAND);
      Holder<ConfiguredFeature<?, ?>> holder10 = holdergetter.getOrThrow(MiscOverworldFeatures.DISK_GRASS);
      Holder<ConfiguredFeature<?, ?>> holder11 = holdergetter.getOrThrow(MiscOverworldFeatures.FREEZE_TOP_LAYER);
      Holder<ConfiguredFeature<?, ?>> holder12 = holdergetter.getOrThrow(MiscOverworldFeatures.VOID_START_PLATFORM);
      Holder<ConfiguredFeature<?, ?>> holder13 = holdergetter.getOrThrow(MiscOverworldFeatures.DESERT_WELL);
      Holder<ConfiguredFeature<?, ?>> holder14 = holdergetter.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_OVERWORLD);
      Holder<ConfiguredFeature<?, ?>> holder15 = holdergetter.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_FROZEN);
      Holder<ConfiguredFeature<?, ?>> holder16 = holdergetter.getOrThrow(MiscOverworldFeatures.SPRING_WATER);
      PlacementUtils.register(p_331286_, ICE_SPIKE, holder, CountPlacement.of(3), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
      PlacementUtils.register(
         p_331286_,
         ICE_PATCH,
         holder1,
         CountPlacement.of(2),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Blocks.SNOW_BLOCK)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(p_331286_, FOREST_ROCK, holder2, CountPlacement.of(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
      PlacementUtils.register(p_331286_, ICEBERG_BLUE, holder4, RarityFilter.onAverageOnceEvery(200), InSquarePlacement.spread(), BiomeFilter.biome());
      PlacementUtils.register(p_331286_, ICEBERG_PACKED, holder3, RarityFilter.onAverageOnceEvery(16), InSquarePlacement.spread(), BiomeFilter.biome());
      PlacementUtils.register(
         p_331286_,
         BLUE_ICE,
         holder5,
         CountPlacement.of(UniformInt.of(0, 19)),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.absolute(30), VerticalAnchor.absolute(61)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_331286_,
         LAKE_LAVA_UNDERGROUND,
         holder6,
         RarityFilter.onAverageOnceEvery(9),
         InSquarePlacement.spread(),
         HeightRangePlacement.of(UniformHeight.of(VerticalAnchor.absolute(0), VerticalAnchor.top())),
         EnvironmentScanPlacement.scanningFor(
            Direction.DOWN,
            BlockPredicate.allOf(BlockPredicate.not(BlockPredicate.ONLY_IN_AIR_PREDICATE), BlockPredicate.insideWorld(new BlockPos(0, -5, 0))),
            32
         ),
         SurfaceRelativeThresholdFilter.of(Heightmap.Types.OCEAN_FLOOR_WG, Integer.MIN_VALUE, -5),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_331286_,
         LAKE_LAVA_SURFACE,
         holder6,
         RarityFilter.onAverageOnceEvery(200),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_331286_,
         DISK_CLAY,
         holder7,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_331286_,
         DISK_GRAVEL,
         holder8,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_331286_,
         DISK_SAND,
         holder9,
         CountPlacement.of(3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_331286_,
         DISK_GRASS,
         holder10,
         CountPlacement.of(1),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Blocks.MUD)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(p_331286_, FREEZE_TOP_LAYER, holder11, BiomeFilter.biome());
      PlacementUtils.register(p_331286_, VOID_START_PLATFORM, holder12, BiomeFilter.biome());
      PlacementUtils.register(
         p_331286_, DESERT_WELL, holder13, RarityFilter.onAverageOnceEvery(1000), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_331286_,
         SPRING_LAVA,
         holder14,
         CountPlacement.of(20),
         InSquarePlacement.spread(),
         HeightRangePlacement.of(VeryBiasedToBottomHeight.of(VerticalAnchor.bottom(), VerticalAnchor.belowTop(8), 8)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_331286_,
         SPRING_LAVA_FROZEN,
         holder15,
         CountPlacement.of(20),
         InSquarePlacement.spread(),
         HeightRangePlacement.of(VeryBiasedToBottomHeight.of(VerticalAnchor.bottom(), VerticalAnchor.belowTop(8), 8)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_331286_,
         SPRING_WATER,
         holder16,
         CountPlacement.of(25),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(192)),
         BiomeFilter.biome()
      );
   }
}
