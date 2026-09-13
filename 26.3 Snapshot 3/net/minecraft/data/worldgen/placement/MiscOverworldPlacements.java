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
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.heightproviders.VeryBiasedToBottomHeight;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
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
   public static final ResourceKey<PlacedFeature> SULFUR_POOL = PlacementUtils.createKey("sulfur_pool");
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

   public static void bootstrap(final BootstrapContext<PlacedFeature> context) {
      HolderGetter<Feature> configuredFeatures = context.lookup(Registries.FEATURE);
      Holder<Feature> iceSpike = configuredFeatures.getOrThrow(MiscOverworldFeatures.ICE_SPIKE);
      Holder<Feature> icePatch = configuredFeatures.getOrThrow(MiscOverworldFeatures.ICE_PATCH);
      Holder<Feature> forestRock = configuredFeatures.getOrThrow(MiscOverworldFeatures.FOREST_ROCK);
      Holder<Feature> icebergPacked = configuredFeatures.getOrThrow(MiscOverworldFeatures.ICEBERG_PACKED);
      Holder<Feature> icebergBlue = configuredFeatures.getOrThrow(MiscOverworldFeatures.ICEBERG_BLUE);
      Holder<Feature> blueIce = configuredFeatures.getOrThrow(MiscOverworldFeatures.BLUE_ICE);
      Holder<Feature> lakeLava = configuredFeatures.getOrThrow(MiscOverworldFeatures.LAKE_LAVA);
      Holder<Feature> sulfurPool = configuredFeatures.getOrThrow(MiscOverworldFeatures.SULFUR_POOL);
      Holder<Feature> diskClay = configuredFeatures.getOrThrow(MiscOverworldFeatures.DISK_CLAY);
      Holder<Feature> diskGravel = configuredFeatures.getOrThrow(MiscOverworldFeatures.DISK_GRAVEL);
      Holder<Feature> diskSand = configuredFeatures.getOrThrow(MiscOverworldFeatures.DISK_SAND);
      Holder<Feature> diskGrass = configuredFeatures.getOrThrow(MiscOverworldFeatures.DISK_GRASS);
      Holder<Feature> freezeTopLayer = configuredFeatures.getOrThrow(MiscOverworldFeatures.FREEZE_TOP_LAYER);
      Holder<Feature> voidStartPlatform = configuredFeatures.getOrThrow(MiscOverworldFeatures.VOID_START_PLATFORM);
      Holder<Feature> desertWell = configuredFeatures.getOrThrow(MiscOverworldFeatures.DESERT_WELL);
      Holder<Feature> springLavaOverworld = configuredFeatures.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_OVERWORLD);
      Holder<Feature> springLavaFrozen = configuredFeatures.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_FROZEN);
      Holder<Feature> springWater = configuredFeatures.getOrThrow(MiscOverworldFeatures.SPRING_WATER);
      PlacementUtils.register(context, ICE_SPIKE, iceSpike, CountPlacement.of(3), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
      PlacementUtils.register(
         context,
         ICE_PATCH,
         icePatch,
         CountPlacement.of(2),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         OffsetPlacement.vertical(ConstantInt.of(-1)),
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Blocks.SNOW_BLOCK)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(context, FOREST_ROCK, forestRock, CountPlacement.of(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
      PlacementUtils.register(context, ICEBERG_BLUE, icebergBlue, RarityFilter.onAverageOnceEvery(200), InSquarePlacement.spread(), BiomeFilter.biome());
      PlacementUtils.register(context, ICEBERG_PACKED, icebergPacked, RarityFilter.onAverageOnceEvery(16), InSquarePlacement.spread(), BiomeFilter.biome());
      PlacementUtils.register(
         context,
         BLUE_ICE,
         blueIce,
         CountPlacement.of(UniformInt.of(0, 19)),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.absolute(30), VerticalAnchor.absolute(61)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         LAKE_LAVA_UNDERGROUND,
         lakeLava,
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
         context,
         LAKE_LAVA_SURFACE,
         lakeLava,
         RarityFilter.onAverageOnceEvery(200),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         SULFUR_POOL,
         sulfurPool,
         CountPlacement.of(256),
         InSquarePlacement.spread(),
         PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
         BlockPredicateFilter.forPredicate(BlockPredicate.solid()),
         EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, 32),
         OffsetPlacement.vertical(ConstantInt.of(-1)),
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Blocks.SULFUR)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         DISK_CLAY,
         diskClay,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         DISK_GRAVEL,
         diskGravel,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         DISK_SAND,
         diskSand,
         CountPlacement.of(3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         DISK_GRASS,
         diskGrass,
         CountPlacement.of(1),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_TOP_SOLID,
         OffsetPlacement.vertical(ConstantInt.of(-1)),
         BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Blocks.MUD)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(context, FREEZE_TOP_LAYER, freezeTopLayer, BiomeFilter.biome());
      PlacementUtils.register(context, VOID_START_PLATFORM, voidStartPlatform, BiomeFilter.biome());
      PlacementUtils.register(
         context, DESERT_WELL, desertWell, RarityFilter.onAverageOnceEvery(1000), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         SPRING_LAVA,
         springLavaOverworld,
         CountPlacement.of(20),
         InSquarePlacement.spread(),
         HeightRangePlacement.of(VeryBiasedToBottomHeight.of(VerticalAnchor.bottom(), VerticalAnchor.belowTop(8), 8)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         SPRING_LAVA_FROZEN,
         springLavaFrozen,
         CountPlacement.of(20),
         InSquarePlacement.spread(),
         HeightRangePlacement.of(VeryBiasedToBottomHeight.of(VerticalAnchor.bottom(), VerticalAnchor.belowTop(8), 8)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         SPRING_WATER,
         springWater,
         CountPlacement.of(25),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(192)),
         BiomeFilter.biome()
      );
   }
}
