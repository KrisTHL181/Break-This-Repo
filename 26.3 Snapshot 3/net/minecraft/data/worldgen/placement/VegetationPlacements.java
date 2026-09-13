package net.minecraft.data.worldgen.placement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.ClampedInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.NoiseThresholdCountPlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import org.jspecify.annotations.Nullable;

public class VegetationPlacements {
   public static final ResourceKey<PlacedFeature> BAMBOO_LIGHT = PlacementUtils.createKey("bamboo_light");
   public static final ResourceKey<PlacedFeature> BAMBOO = PlacementUtils.createKey("bamboo");
   public static final ResourceKey<PlacedFeature> BAMBOO_IN_STRUCTURE = PlacementUtils.createKey("bamboo_in_structure");
   public static final ResourceKey<PlacedFeature> VINES = PlacementUtils.createKey("vines");
   public static final ResourceKey<PlacedFeature> PATCH_SUNFLOWER = PlacementUtils.createKey("patch_sunflower");
   public static final ResourceKey<PlacedFeature> PATCH_PUMPKIN = PlacementUtils.createKey("patch_pumpkin");
   public static final ResourceKey<PlacedFeature> PATCH_GRASS_PLAIN = PlacementUtils.createKey("patch_grass_plain");
   public static final ResourceKey<PlacedFeature> PATCH_GRASS_MEADOW = PlacementUtils.createKey("patch_grass_meadow");
   public static final ResourceKey<PlacedFeature> PATCH_GRASS_FOREST = PlacementUtils.createKey("patch_grass_forest");
   public static final ResourceKey<PlacedFeature> PATCH_GRASS_BADLANDS = PlacementUtils.createKey("patch_grass_badlands");
   public static final ResourceKey<PlacedFeature> PATCH_GRASS_SAVANNA = PlacementUtils.createKey("patch_grass_savanna");
   public static final ResourceKey<PlacedFeature> PATCH_GRASS_NORMAL = PlacementUtils.createKey("patch_grass_normal");
   public static final ResourceKey<PlacedFeature> PATCH_GRASS_TAIGA_2 = PlacementUtils.createKey("patch_grass_taiga_2");
   public static final ResourceKey<PlacedFeature> PATCH_GRASS_TAIGA = PlacementUtils.createKey("patch_grass_taiga");
   public static final ResourceKey<PlacedFeature> PATCH_GRASS_JUNGLE = PlacementUtils.createKey("patch_grass_jungle");
   public static final ResourceKey<PlacedFeature> GRASS_BONEMEAL = PlacementUtils.createKey("grass_bonemeal");
   public static final ResourceKey<PlacedFeature> PATCH_DEAD_BUSH_2 = PlacementUtils.createKey("patch_dead_bush_2");
   public static final ResourceKey<PlacedFeature> PATCH_DEAD_BUSH = PlacementUtils.createKey("patch_dead_bush");
   public static final ResourceKey<PlacedFeature> PATCH_DEAD_BUSH_BADLANDS = PlacementUtils.createKey("patch_dead_bush_badlands");
   public static final ResourceKey<PlacedFeature> PATCH_DRY_GRASS_BADLANDS = PlacementUtils.createKey("patch_dry_grass_badlands");
   public static final ResourceKey<PlacedFeature> PATCH_DRY_GRASS_DESERT = PlacementUtils.createKey("patch_dry_grass_desert");
   public static final ResourceKey<PlacedFeature> PATCH_MELON = PlacementUtils.createKey("patch_melon");
   public static final ResourceKey<PlacedFeature> PATCH_MELON_SPARSE = PlacementUtils.createKey("patch_melon_sparse");
   public static final ResourceKey<PlacedFeature> PATCH_BERRY_COMMON = PlacementUtils.createKey("patch_berry_common");
   public static final ResourceKey<PlacedFeature> PATCH_BERRY_RARE = PlacementUtils.createKey("patch_berry_rare");
   public static final ResourceKey<PlacedFeature> PATCH_WATERLILY = PlacementUtils.createKey("patch_waterlily");
   public static final ResourceKey<PlacedFeature> PATCH_TALL_GRASS_2 = PlacementUtils.createKey("patch_tall_grass_2");
   public static final ResourceKey<PlacedFeature> PATCH_TALL_GRASS = PlacementUtils.createKey("patch_tall_grass");
   public static final ResourceKey<PlacedFeature> PATCH_LARGE_FERN = PlacementUtils.createKey("patch_large_fern");
   public static final ResourceKey<PlacedFeature> PATCH_BUSH = PlacementUtils.createKey("patch_bush");
   public static final ResourceKey<PlacedFeature> PATCH_RED_SHRUB = PlacementUtils.createKey("patch_red_shrub");
   public static final ResourceKey<PlacedFeature> PATCH_LEAF_LITTER = PlacementUtils.createKey("patch_leaf_litter");
   public static final ResourceKey<PlacedFeature> PATCH_CACTUS_DESERT = PlacementUtils.createKey("patch_cactus_desert");
   public static final ResourceKey<PlacedFeature> PATCH_CACTUS_DECORATED = PlacementUtils.createKey("patch_cactus_decorated");
   public static final ResourceKey<PlacedFeature> PATCH_SUGAR_CANE_SWAMP = PlacementUtils.createKey("patch_sugar_cane_swamp");
   public static final ResourceKey<PlacedFeature> PATCH_SUGAR_CANE_DESERT = PlacementUtils.createKey("patch_sugar_cane_desert");
   public static final ResourceKey<PlacedFeature> PATCH_SUGAR_CANE_BADLANDS = PlacementUtils.createKey("patch_sugar_cane_badlands");
   public static final ResourceKey<PlacedFeature> PATCH_SUGAR_CANE = PlacementUtils.createKey("patch_sugar_cane");
   public static final ResourceKey<PlacedFeature> PATCH_FIREFLY_BUSH_SWAMP = PlacementUtils.createKey("patch_firefly_bush_swamp");
   public static final ResourceKey<PlacedFeature> PATCH_FIREFLY_BUSH_NEAR_WATER_SWAMP = PlacementUtils.createKey("patch_firefly_bush_near_water_swamp");
   public static final ResourceKey<PlacedFeature> PATCH_FIREFLY_BUSH_NEAR_WATER = PlacementUtils.createKey("patch_firefly_bush_near_water");
   public static final ResourceKey<PlacedFeature> BROWN_MUSHROOM_NETHER = PlacementUtils.createKey("brown_mushroom_nether");
   public static final ResourceKey<PlacedFeature> RED_MUSHROOM_NETHER = PlacementUtils.createKey("red_mushroom_nether");
   public static final ResourceKey<PlacedFeature> BROWN_MUSHROOM_NORMAL = PlacementUtils.createKey("brown_mushroom_normal");
   public static final ResourceKey<PlacedFeature> RED_MUSHROOM_NORMAL = PlacementUtils.createKey("red_mushroom_normal");
   public static final ResourceKey<PlacedFeature> BROWN_MUSHROOM_TAIGA = PlacementUtils.createKey("brown_mushroom_taiga");
   public static final ResourceKey<PlacedFeature> RED_MUSHROOM_TAIGA = PlacementUtils.createKey("red_mushroom_taiga");
   public static final ResourceKey<PlacedFeature> BROWN_MUSHROOM_OLD_GROWTH = PlacementUtils.createKey("brown_mushroom_old_growth");
   public static final ResourceKey<PlacedFeature> RED_MUSHROOM_OLD_GROWTH = PlacementUtils.createKey("red_mushroom_old_growth");
   public static final ResourceKey<PlacedFeature> BROWN_MUSHROOM_SWAMP = PlacementUtils.createKey("brown_mushroom_swamp");
   public static final ResourceKey<PlacedFeature> RED_MUSHROOM_SWAMP = PlacementUtils.createKey("red_mushroom_swamp");
   public static final ResourceKey<PlacedFeature> BROWN_MUSHROOM_DAPPLED_FOREST = PlacementUtils.createKey("brown_mushroom_dappled_forest");
   public static final ResourceKey<PlacedFeature> FLOWER_WARM = PlacementUtils.createKey("flower_warm");
   public static final ResourceKey<PlacedFeature> FLOWER_DEFAULT = PlacementUtils.createKey("flower_default");
   public static final ResourceKey<PlacedFeature> FLOWER_FLOWER_FOREST = PlacementUtils.createKey("flower_flower_forest");
   public static final ResourceKey<PlacedFeature> FLOWER_SWAMP = PlacementUtils.createKey("flower_swamp");
   public static final ResourceKey<PlacedFeature> FLOWER_PLAINS = PlacementUtils.createKey("flower_plains");
   public static final ResourceKey<PlacedFeature> FLOWER_MEADOW = PlacementUtils.createKey("flower_meadow");
   public static final ResourceKey<PlacedFeature> FLOWER_CHERRY = PlacementUtils.createKey("flower_cherry");
   public static final ResourceKey<PlacedFeature> FLOWER_PALE_GARDEN = PlacementUtils.createKey("flower_pale_garden");
   public static final ResourceKey<PlacedFeature> WILDFLOWERS_BIRCH_FOREST = PlacementUtils.createKey("wildflowers_birch_forest");
   public static final ResourceKey<PlacedFeature> WILDFLOWERS_MEADOW = PlacementUtils.createKey("wildflowers_meadow");
   public static final ResourceKey<PlacedFeature> TREES_PLAINS = PlacementUtils.createKey("trees_plains");
   public static final ResourceKey<PlacedFeature> DARK_FOREST_VEGETATION = PlacementUtils.createKey("dark_forest_vegetation");
   public static final ResourceKey<PlacedFeature> PALE_GARDEN_VEGETATION = PlacementUtils.createKey("pale_garden_vegetation");
   public static final ResourceKey<PlacedFeature> FLOWER_FOREST_FLOWERS = PlacementUtils.createKey("flower_forest_flowers");
   public static final ResourceKey<PlacedFeature> FOREST_FLOWERS = PlacementUtils.createKey("forest_flowers");
   public static final ResourceKey<PlacedFeature> PALE_GARDEN_FLOWERS = PlacementUtils.createKey("pale_garden_flowers");
   public static final ResourceKey<PlacedFeature> PALE_MOSS_PATCH = PlacementUtils.createKey("pale_moss_patch");
   public static final ResourceKey<PlacedFeature> TREES_FLOWER_FOREST = PlacementUtils.createKey("trees_flower_forest");
   public static final ResourceKey<PlacedFeature> TREES_MEADOW = PlacementUtils.createKey("trees_meadow");
   public static final ResourceKey<PlacedFeature> TREES_CHERRY = PlacementUtils.createKey("trees_cherry");
   public static final ResourceKey<PlacedFeature> TREES_TAIGA = PlacementUtils.createKey("trees_taiga");
   public static final ResourceKey<PlacedFeature> TREES_GROVE = PlacementUtils.createKey("trees_grove");
   public static final ResourceKey<PlacedFeature> TREES_BADLANDS = PlacementUtils.createKey("trees_badlands");
   public static final ResourceKey<PlacedFeature> TREES_SNOWY = PlacementUtils.createKey("trees_snowy");
   public static final ResourceKey<PlacedFeature> TREES_SWAMP = PlacementUtils.createKey("trees_swamp");
   public static final ResourceKey<PlacedFeature> TREES_WINDSWEPT_SAVANNA = PlacementUtils.createKey("trees_windswept_savanna");
   public static final ResourceKey<PlacedFeature> TREES_SAVANNA = PlacementUtils.createKey("trees_savanna");
   public static final ResourceKey<PlacedFeature> BIRCH_TALL = PlacementUtils.createKey("birch_tall");
   public static final ResourceKey<PlacedFeature> TREES_BIRCH = PlacementUtils.createKey("trees_birch");
   public static final ResourceKey<PlacedFeature> TREES_WINDSWEPT_FOREST = PlacementUtils.createKey("trees_windswept_forest");
   public static final ResourceKey<PlacedFeature> TREES_WINDSWEPT_HILLS = PlacementUtils.createKey("trees_windswept_hills");
   public static final ResourceKey<PlacedFeature> TREES_WATER = PlacementUtils.createKey("trees_water");
   public static final ResourceKey<PlacedFeature> TREES_BIRCH_AND_OAK_LEAF_LITTER = PlacementUtils.createKey("trees_birch_and_oak_leaf_litter");
   public static final ResourceKey<PlacedFeature> TREES_SPARSE_JUNGLE = PlacementUtils.createKey("trees_sparse_jungle");
   public static final ResourceKey<PlacedFeature> TREES_OLD_GROWTH_SPRUCE_TAIGA = PlacementUtils.createKey("trees_old_growth_spruce_taiga");
   public static final ResourceKey<PlacedFeature> TREES_OLD_GROWTH_PINE_TAIGA = PlacementUtils.createKey("trees_old_growth_pine_taiga");
   public static final ResourceKey<PlacedFeature> TREES_JUNGLE = PlacementUtils.createKey("trees_jungle");
   public static final ResourceKey<PlacedFeature> TREES_DAPPLED_FOREST = PlacementUtils.createKey("trees_dappled_forest");
   public static final ResourceKey<PlacedFeature> BAMBOO_VEGETATION = PlacementUtils.createKey("bamboo_vegetation");
   public static final ResourceKey<PlacedFeature> MUSHROOM_ISLAND_VEGETATION = PlacementUtils.createKey("mushroom_island_vegetation");
   public static final ResourceKey<PlacedFeature> TREES_MANGROVE = PlacementUtils.createKey("trees_mangrove");
   private static final PlacementModifier TREE_THRESHOLD = SurfaceWaterDepthFilter.forMaxDepth(0);

   public static List<PlacementModifier> worldSurfaceSquaredWithCount(final int count) {
      return List.of(CountPlacement.of(count), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
   }

   private static List<PlacementModifier> getMushroomPlacement(final int rarity, final @Nullable PlacementModifier prefix) {
      Builder<PlacementModifier> builder = ImmutableList.builder();
      if (prefix != null) {
         builder.add(prefix);
      }

      if (rarity != 0) {
         builder.add(RarityFilter.onAverageOnceEvery(rarity));
      }

      builder.add(InSquarePlacement.spread());
      builder.add(PlacementUtils.HEIGHTMAP);
      builder.add(BiomeFilter.biome());
      builder.add(CountPlacement.of(96));
      builder.add(OffsetPlacement.ofTriangle(7, 3));
      builder.add(BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE));
      return builder.build();
   }

   private static Builder<PlacementModifier> treePlacementBase(final PlacementModifier frequency) {
      return ImmutableList.builder()
         .add(frequency)
         .add(InSquarePlacement.spread())
         .add(TREE_THRESHOLD)
         .add(PlacementUtils.HEIGHTMAP_OCEAN_FLOOR)
         .add(BiomeFilter.biome());
   }

   public static List<PlacementModifier> treePlacement(final PlacementModifier frequency) {
      return treePlacementBase(frequency).build();
   }

   public static List<PlacementModifier> treePlacement(final PlacementModifier frequency, final Block sapling) {
      return treePlacementBase(frequency).add(BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(sapling))).build();
   }

   public static void bootstrap(final BootstrapContext<PlacedFeature> context) {
      HolderGetter<Feature> configuredFeatures = context.lookup(Registries.FEATURE);
      Holder<Feature> bambooNoPodzol = configuredFeatures.getOrThrow(VegetationFeatures.BAMBOO_NO_PODZOL);
      Holder<Feature> bambooSomePodzol = configuredFeatures.getOrThrow(VegetationFeatures.BAMBOO_SOME_PODZOL);
      Holder<Feature> vines = configuredFeatures.getOrThrow(VegetationFeatures.VINES);
      Holder<Feature> sunflower = configuredFeatures.getOrThrow(VegetationFeatures.SUNFLOWER);
      Holder<Feature> pumpkin = configuredFeatures.getOrThrow(VegetationFeatures.PUMPKIN);
      Holder<Feature> grass = configuredFeatures.getOrThrow(VegetationFeatures.GRASS);
      Holder<Feature> leafLitter = configuredFeatures.getOrThrow(VegetationFeatures.LEAF_LITTER);
      Holder<Feature> taigaGrass = configuredFeatures.getOrThrow(VegetationFeatures.TAIGA_GRASS);
      Holder<Feature> grassJungle = configuredFeatures.getOrThrow(VegetationFeatures.GRASS_JUNGLE);
      Holder<Feature> deadBush = configuredFeatures.getOrThrow(VegetationFeatures.DEAD_BUSH);
      Holder<Feature> dryGrass = configuredFeatures.getOrThrow(VegetationFeatures.DRY_GRASS);
      Holder<Feature> fireflyBush = configuredFeatures.getOrThrow(VegetationFeatures.FIREFLY_BUSH);
      Holder<Feature> melon = configuredFeatures.getOrThrow(VegetationFeatures.MELON);
      Holder<Feature> berryBush = configuredFeatures.getOrThrow(VegetationFeatures.BERRY_BUSH);
      Holder<Feature> waterlily = configuredFeatures.getOrThrow(VegetationFeatures.WATERLILY);
      Holder<Feature> tallGrass = configuredFeatures.getOrThrow(VegetationFeatures.TALL_GRASS);
      Holder<Feature> largeFern = configuredFeatures.getOrThrow(VegetationFeatures.LARGE_FERN);
      Holder<Feature> bush = configuredFeatures.getOrThrow(VegetationFeatures.BUSH);
      Holder<Feature> redShrub = configuredFeatures.getOrThrow(VegetationFeatures.RED_SHRUB);
      Holder<Feature> cactus = configuredFeatures.getOrThrow(VegetationFeatures.CACTUS);
      Holder<Feature> sugarCane = configuredFeatures.getOrThrow(VegetationFeatures.SUGAR_CANE);
      Holder<Feature> brownMushroom = configuredFeatures.getOrThrow(VegetationFeatures.BROWN_MUSHROOM);
      Holder<Feature> redMushroom = configuredFeatures.getOrThrow(VegetationFeatures.RED_MUSHROOM);
      Holder<Feature> flowerDefault = configuredFeatures.getOrThrow(VegetationFeatures.FLOWER_DEFAULT);
      Holder<Feature> flowerFlowerForest = configuredFeatures.getOrThrow(VegetationFeatures.FLOWER_FLOWER_FOREST);
      Holder<Feature> flowerSwamp = configuredFeatures.getOrThrow(VegetationFeatures.FLOWER_SWAMP);
      Holder<Feature> flowerPlain = configuredFeatures.getOrThrow(VegetationFeatures.FLOWER_PLAIN);
      Holder<Feature> flowerMeadow = configuredFeatures.getOrThrow(VegetationFeatures.FLOWER_MEADOW);
      Holder<Feature> flowerCherry = configuredFeatures.getOrThrow(VegetationFeatures.FLOWER_CHERRY);
      Holder<Feature> flowerPaleGarden = configuredFeatures.getOrThrow(VegetationFeatures.FLOWER_PALE_GARDEN);
      Holder<Feature> wildflower = configuredFeatures.getOrThrow(VegetationFeatures.WILDFLOWER);
      Holder<Feature> treesPlains = configuredFeatures.getOrThrow(VegetationFeatures.TREES_PLAINS);
      Holder<Feature> darkForestVegetation = configuredFeatures.getOrThrow(VegetationFeatures.DARK_FOREST_VEGETATION);
      Holder<Feature> paleGardenVegetation = configuredFeatures.getOrThrow(VegetationFeatures.PALE_GARDEN_VEGETATION);
      Holder<Feature> forestFlowers = configuredFeatures.getOrThrow(VegetationFeatures.FOREST_FLOWERS);
      Holder<Feature> paleForestFlower = configuredFeatures.getOrThrow(VegetationFeatures.PALE_FOREST_FLOWER);
      Holder<Feature> paleMossPatch = configuredFeatures.getOrThrow(VegetationFeatures.PALE_MOSS_PATCH);
      Holder<Feature> treesFlowerForest = configuredFeatures.getOrThrow(VegetationFeatures.TREES_FLOWER_FOREST);
      Holder<Feature> meadowTrees = configuredFeatures.getOrThrow(VegetationFeatures.MEADOW_TREES);
      Holder<Feature> treesTaiga = configuredFeatures.getOrThrow(VegetationFeatures.TREES_TAIGA);
      Holder<Feature> treesBadlands = configuredFeatures.getOrThrow(VegetationFeatures.TREES_BADLANDS);
      Holder<Feature> treesGrove = configuredFeatures.getOrThrow(VegetationFeatures.TREES_GROVE);
      Holder<Feature> treesSnowy = configuredFeatures.getOrThrow(VegetationFeatures.TREES_SNOWY);
      Holder<Feature> cherryBees005 = configuredFeatures.getOrThrow(TreeFeatures.CHERRY_BEES_005);
      Holder<Feature> swampOak = configuredFeatures.getOrThrow(TreeFeatures.SWAMP_OAK);
      Holder<Feature> treesSavanna = configuredFeatures.getOrThrow(VegetationFeatures.TREES_SAVANNA);
      Holder<Feature> birchTall = configuredFeatures.getOrThrow(VegetationFeatures.BIRCH_TALL);
      Holder<Feature> treesBirch = configuredFeatures.getOrThrow(VegetationFeatures.TREES_BIRCH);
      Holder<Feature> treesWindsweptHills = configuredFeatures.getOrThrow(VegetationFeatures.TREES_WINDSWEPT_HILLS);
      Holder<Feature> treesWater = configuredFeatures.getOrThrow(VegetationFeatures.TREES_WATER);
      Holder<Feature> treesBirchAndOakLeafLitter = configuredFeatures.getOrThrow(VegetationFeatures.TREES_BIRCH_AND_OAK_LEAF_LITTER);
      Holder<Feature> treesSparseJungle = configuredFeatures.getOrThrow(VegetationFeatures.TREES_SPARSE_JUNGLE);
      Holder<Feature> treesOldGrowthSpruceTaiga = configuredFeatures.getOrThrow(VegetationFeatures.TREES_OLD_GROWTH_SPRUCE_TAIGA);
      Holder<Feature> treesOldGrowthPineTaiga = configuredFeatures.getOrThrow(VegetationFeatures.TREES_OLD_GROWTH_PINE_TAIGA);
      Holder<Feature> treesJungle = configuredFeatures.getOrThrow(VegetationFeatures.TREES_JUNGLE);
      Holder<Feature> treesDappledForest = configuredFeatures.getOrThrow(VegetationFeatures.TREES_DAPPLED_FOREST);
      Holder<Feature> bambooVegetation = configuredFeatures.getOrThrow(VegetationFeatures.BAMBOO_VEGETATION);
      Holder<Feature> mushroomIslandVegetation = configuredFeatures.getOrThrow(VegetationFeatures.MUSHROOM_ISLAND_VEGETATION);
      Holder<Feature> mangroveVegetation = configuredFeatures.getOrThrow(VegetationFeatures.MANGROVE_VEGETATION);
      PlacementUtils.register(
         context, BAMBOO_LIGHT, bambooNoPodzol, RarityFilter.onAverageOnceEvery(4), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         BAMBOO,
         bambooSomePodzol,
         NoiseBasedCountPlacement.of(160, 80.0, 0.3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(context, BAMBOO_IN_STRUCTURE, bambooNoPodzol, PlacementUtils.isEmpty());
      PlacementUtils.register(
         context,
         VINES,
         vines,
         CountPlacement.of(127),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(100)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         PATCH_SUNFLOWER,
         sunflower,
         RarityFilter.onAverageOnceEvery(3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_PUMPKIN,
         pumpkin,
         RarityFilter.onAverageOnceEvery(300),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(
            BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.GRASS_BLOCK))
         )
      );
      PlacementUtils.register(
         context,
         PATCH_GRASS_PLAIN,
         grass,
         NoiseThresholdCountPlacement.of(-0.8, 5, 10),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome(),
         CountPlacement.of(32),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_GRASS_MEADOW,
         grass,
         NoiseThresholdCountPlacement.of(-0.8, 5, 10),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome(),
         CountPlacement.of(16),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_GRASS_FOREST,
         grass,
         Util.copyAndAdd(
            worldSurfaceSquaredWithCount(2),
            CountPlacement.of(32),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
         )
      );
      PlacementUtils.register(
         context,
         PATCH_LEAF_LITTER,
         leafLitter,
         Util.copyAndAdd(
            worldSurfaceSquaredWithCount(2),
            CountPlacement.of(32),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(
               BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.GRASS_BLOCK))
            )
         )
      );
      PlacementUtils.register(
         context,
         PATCH_GRASS_BADLANDS,
         grass,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome(),
         CountPlacement.of(32),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_GRASS_SAVANNA,
         grass,
         Util.copyAndAdd(
            worldSurfaceSquaredWithCount(20),
            CountPlacement.of(32),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
         )
      );
      PlacementUtils.register(
         context,
         PATCH_GRASS_NORMAL,
         grass,
         Util.copyAndAdd(
            worldSurfaceSquaredWithCount(5),
            CountPlacement.of(32),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
         )
      );
      PlacementUtils.register(
         context,
         PATCH_GRASS_TAIGA_2,
         taigaGrass,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome(),
         CountPlacement.of(32),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_GRASS_TAIGA,
         taigaGrass,
         Util.copyAndAdd(
            worldSurfaceSquaredWithCount(7),
            CountPlacement.of(32),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
         )
      );
      PlacementUtils.register(
         context,
         PATCH_GRASS_JUNGLE,
         grassJungle,
         Util.copyAndAdd(
            worldSurfaceSquaredWithCount(25),
            CountPlacement.of(32),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(
               BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.not(BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.PODZOL)))
            )
         )
      );
      PlacementUtils.register(context, GRASS_BONEMEAL, grass, PlacementUtils.isEmpty());
      PlacementUtils.register(
         context,
         PATCH_DEAD_BUSH_2,
         deadBush,
         Util.copyAndAdd(
            worldSurfaceSquaredWithCount(2),
            CountPlacement.of(4),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
         )
      );
      PlacementUtils.register(
         context,
         PATCH_DEAD_BUSH,
         deadBush,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome(),
         CountPlacement.of(4),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_DEAD_BUSH_BADLANDS,
         deadBush,
         Util.copyAndAdd(
            worldSurfaceSquaredWithCount(20),
            CountPlacement.of(4),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
         )
      );
      PlacementUtils.register(
         context,
         PATCH_DRY_GRASS_BADLANDS,
         dryGrass,
         RarityFilter.onAverageOnceEvery(6),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(64),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_DRY_GRASS_DESERT,
         dryGrass,
         RarityFilter.onAverageOnceEvery(3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(64),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      BlockPredicate melonPlacementPredicate = BlockPredicate.allOf(
         BlockPredicate.replaceable(), BlockPredicate.noFluid(), BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.GRASS_BLOCK)
      );
      PlacementUtils.register(
         context,
         PATCH_MELON,
         melon,
         RarityFilter.onAverageOnceEvery(6),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(64),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(melonPlacementPredicate)
      );
      PlacementUtils.register(
         context,
         PATCH_MELON_SPARSE,
         melon,
         RarityFilter.onAverageOnceEvery(64),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(64),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(melonPlacementPredicate)
      );
      PlacementUtils.register(
         context,
         PATCH_BERRY_COMMON,
         berryBush,
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(
            BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.GRASS_BLOCK))
         )
      );
      PlacementUtils.register(
         context,
         PATCH_BERRY_RARE,
         berryBush,
         RarityFilter.onAverageOnceEvery(384),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(
            BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.GRASS_BLOCK))
         )
      );
      PlacementUtils.register(
         context,
         PATCH_WATERLILY,
         waterlily,
         Util.copyAndAdd(
            worldSurfaceSquaredWithCount(4),
            CountPlacement.of(10),
            OffsetPlacement.ofTriangle(7, 3),
            BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
         )
      );
      PlacementUtils.register(
         context,
         PATCH_TALL_GRASS_2,
         tallGrass,
         NoiseThresholdCountPlacement.of(-0.8, 0, 7),
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_TALL_GRASS,
         tallGrass,
         RarityFilter.onAverageOnceEvery(5),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_LARGE_FERN,
         largeFern,
         RarityFilter.onAverageOnceEvery(5),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_BUSH,
         bush,
         RarityFilter.onAverageOnceEvery(4),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(24),
         OffsetPlacement.ofTriangle(5, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_RED_SHRUB,
         redShrub,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome(),
         CountPlacement.of(8),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_CACTUS_DESERT,
         cactus,
         RarityFilter.onAverageOnceEvery(6),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(10),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.wouldSurvive(Blocks.CACTUS)))
      );
      PlacementUtils.register(
         context,
         PATCH_CACTUS_DECORATED,
         cactus,
         RarityFilter.onAverageOnceEvery(13),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(10),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.wouldSurvive(Blocks.CACTUS)))
      );
      PlacementUtils.register(
         context,
         PATCH_SUGAR_CANE_SWAMP,
         sugarCane,
         RarityFilter.onAverageOnceEvery(3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(20),
         OffsetPlacement.ofTriangle(4, 0),
         VegetationFeatures.nearWaterPredicate(Blocks.SUGAR_CANE)
      );
      PlacementUtils.register(
         context,
         PATCH_SUGAR_CANE_DESERT,
         sugarCane,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(20),
         OffsetPlacement.ofTriangle(4, 0),
         VegetationFeatures.nearWaterPredicate(Blocks.SUGAR_CANE)
      );
      PlacementUtils.register(
         context,
         PATCH_SUGAR_CANE_BADLANDS,
         sugarCane,
         RarityFilter.onAverageOnceEvery(5),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(20),
         OffsetPlacement.ofTriangle(4, 0),
         VegetationFeatures.nearWaterPredicate(Blocks.SUGAR_CANE)
      );
      PlacementUtils.register(
         context,
         PATCH_SUGAR_CANE,
         sugarCane,
         RarityFilter.onAverageOnceEvery(6),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(20),
         OffsetPlacement.ofTriangle(4, 0),
         VegetationFeatures.nearWaterPredicate(Blocks.SUGAR_CANE)
      );
      PlacementUtils.register(
         context,
         PATCH_FIREFLY_BUSH_NEAR_WATER,
         fireflyBush,
         CountPlacement.of(2),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_NO_LEAVES,
         BiomeFilter.biome(),
         VegetationFeatures.nearWaterPredicate(Blocks.FIREFLY_BUSH),
         CountPlacement.of(20),
         OffsetPlacement.ofTriangle(4, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_FIREFLY_BUSH_NEAR_WATER_SWAMP,
         fireflyBush,
         CountPlacement.of(3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         VegetationFeatures.nearWaterPredicate(Blocks.FIREFLY_BUSH),
         CountPlacement.of(20),
         OffsetPlacement.ofTriangle(4, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         PATCH_FIREFLY_BUSH_SWAMP,
         fireflyBush,
         RarityFilter.onAverageOnceEvery(8),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(20),
         OffsetPlacement.ofTriangle(4, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         BROWN_MUSHROOM_NETHER,
         brownMushroom,
         RarityFilter.onAverageOnceEvery(2),
         InSquarePlacement.spread(),
         PlacementUtils.FULL_RANGE,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         RED_MUSHROOM_NETHER,
         redMushroom,
         RarityFilter.onAverageOnceEvery(2),
         InSquarePlacement.spread(),
         PlacementUtils.FULL_RANGE,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(context, BROWN_MUSHROOM_NORMAL, brownMushroom, getMushroomPlacement(256, null));
      PlacementUtils.register(context, RED_MUSHROOM_NORMAL, redMushroom, getMushroomPlacement(512, null));
      PlacementUtils.register(context, BROWN_MUSHROOM_TAIGA, brownMushroom, getMushroomPlacement(4, null));
      PlacementUtils.register(context, RED_MUSHROOM_TAIGA, redMushroom, getMushroomPlacement(256, null));
      PlacementUtils.register(context, BROWN_MUSHROOM_OLD_GROWTH, brownMushroom, getMushroomPlacement(4, CountPlacement.of(3)));
      PlacementUtils.register(context, RED_MUSHROOM_OLD_GROWTH, redMushroom, getMushroomPlacement(171, null));
      PlacementUtils.register(context, BROWN_MUSHROOM_SWAMP, brownMushroom, getMushroomPlacement(0, CountPlacement.of(2)));
      PlacementUtils.register(context, RED_MUSHROOM_SWAMP, redMushroom, getMushroomPlacement(64, null));
      PlacementUtils.register(context, BROWN_MUSHROOM_DAPPLED_FOREST, brownMushroom, getMushroomPlacement(2, null));
      PlacementUtils.register(
         context,
         FLOWER_WARM,
         flowerDefault,
         RarityFilter.onAverageOnceEvery(16),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(64),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         FLOWER_DEFAULT,
         flowerDefault,
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(64),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         FLOWER_FLOWER_FOREST,
         flowerFlowerForest,
         CountPlacement.of(3),
         RarityFilter.onAverageOnceEvery(2),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(6, 2),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         FLOWER_SWAMP,
         flowerSwamp,
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(64),
         OffsetPlacement.ofTriangle(6, 2),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         FLOWER_PLAINS,
         flowerPlain,
         NoiseThresholdCountPlacement.of(-0.8, 15, 4),
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(64),
         OffsetPlacement.ofTriangle(6, 2),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         FLOWER_CHERRY,
         flowerCherry,
         NoiseThresholdCountPlacement.of(-0.8, 5, 10),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(6, 2),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         FLOWER_MEADOW,
         flowerMeadow,
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(6, 2),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         FLOWER_PALE_GARDEN,
         flowerPaleGarden,
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         WILDFLOWERS_BIRCH_FOREST,
         wildflower,
         CountPlacement.of(3),
         RarityFilter.onAverageOnceEvery(2),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(64),
         OffsetPlacement.ofTriangle(6, 2),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context,
         WILDFLOWERS_MEADOW,
         wildflower,
         NoiseThresholdCountPlacement.of(-0.8, 5, 10),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         CountPlacement.of(8),
         OffsetPlacement.ofTriangle(6, 2),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementModifier treeThreshold = SurfaceWaterDepthFilter.forMaxDepth(0);
      PlacementUtils.register(
         context,
         TREES_PLAINS,
         treesPlains,
         PlacementUtils.countExtra(0, 0.05F, 1),
         InSquarePlacement.spread(),
         treeThreshold,
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(Blocks.OAK_SAPLING)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         DARK_FOREST_VEGETATION,
         darkForestVegetation,
         CountPlacement.of(16),
         InSquarePlacement.spread(),
         treeThreshold,
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         TREES_DAPPLED_FOREST,
         treesDappledForest,
         CountPlacement.of(6),
         InSquarePlacement.spread(),
         treeThreshold,
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         PALE_GARDEN_VEGETATION,
         paleGardenVegetation,
         CountPlacement.of(16),
         InSquarePlacement.spread(),
         treeThreshold,
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         FLOWER_FOREST_FLOWERS,
         forestFlowers,
         RarityFilter.onAverageOnceEvery(7),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         CountPlacement.of(ClampedInt.of(UniformInt.of(-1, 3), 0, 3)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         FOREST_FLOWERS,
         forestFlowers,
         RarityFilter.onAverageOnceEvery(7),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         CountPlacement.of(ClampedInt.of(UniformInt.of(-3, 1), 0, 1)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         PALE_GARDEN_FLOWERS,
         paleForestFlower,
         RarityFilter.onAverageOnceEvery(8),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_NO_LEAVES,
         BiomeFilter.biome(),
         CountPlacement.of(96),
         OffsetPlacement.ofTriangle(7, 3),
         BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
      );
      PlacementUtils.register(
         context, PALE_MOSS_PATCH, paleMossPatch, CountPlacement.of(1), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_NO_LEAVES, BiomeFilter.biome()
      );
      PlacementUtils.register(context, TREES_FLOWER_FOREST, treesFlowerForest, treePlacement(PlacementUtils.countExtra(6, 0.1F, 1)));
      PlacementUtils.register(context, TREES_MEADOW, meadowTrees, treePlacement(RarityFilter.onAverageOnceEvery(100)));
      PlacementUtils.register(context, TREES_CHERRY, cherryBees005, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1), Blocks.CHERRY_SAPLING));
      PlacementUtils.register(context, TREES_TAIGA, treesTaiga, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(context, TREES_GROVE, treesGrove, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(context, TREES_BADLANDS, treesBadlands, treePlacement(PlacementUtils.countExtra(5, 0.1F, 1), Blocks.OAK_SAPLING));
      PlacementUtils.register(context, TREES_SNOWY, treesSnowy, treePlacement(PlacementUtils.countExtra(0, 0.1F, 1), Blocks.SPRUCE_SAPLING));
      PlacementUtils.register(
         context,
         TREES_SWAMP,
         swampOak,
         PlacementUtils.countExtra(2, 0.1F, 1),
         InSquarePlacement.spread(),
         SurfaceWaterDepthFilter.forMaxDepth(2),
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BiomeFilter.biome(),
         BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(Blocks.OAK_SAPLING))
      );
      PlacementUtils.register(context, TREES_WINDSWEPT_SAVANNA, treesSavanna, treePlacement(PlacementUtils.countExtra(2, 0.1F, 1)));
      PlacementUtils.register(context, TREES_SAVANNA, treesSavanna, treePlacement(PlacementUtils.countExtra(1, 0.1F, 1)));
      PlacementUtils.register(context, BIRCH_TALL, birchTall, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(context, TREES_BIRCH, treesBirch, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1), Blocks.BIRCH_SAPLING));
      PlacementUtils.register(context, TREES_WINDSWEPT_FOREST, treesWindsweptHills, treePlacement(PlacementUtils.countExtra(3, 0.1F, 1)));
      PlacementUtils.register(context, TREES_WINDSWEPT_HILLS, treesWindsweptHills, treePlacement(PlacementUtils.countExtra(0, 0.1F, 1)));
      PlacementUtils.register(context, TREES_WATER, treesWater, treePlacement(PlacementUtils.countExtra(0, 0.1F, 1)));
      PlacementUtils.register(context, TREES_BIRCH_AND_OAK_LEAF_LITTER, treesBirchAndOakLeafLitter, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(context, TREES_SPARSE_JUNGLE, treesSparseJungle, treePlacement(PlacementUtils.countExtra(2, 0.1F, 1)));
      PlacementUtils.register(context, TREES_OLD_GROWTH_SPRUCE_TAIGA, treesOldGrowthSpruceTaiga, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(context, TREES_OLD_GROWTH_PINE_TAIGA, treesOldGrowthPineTaiga, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(context, TREES_JUNGLE, treesJungle, treePlacement(PlacementUtils.countExtra(50, 0.1F, 1)));
      PlacementUtils.register(context, BAMBOO_VEGETATION, bambooVegetation, treePlacement(PlacementUtils.countExtra(30, 0.1F, 1)));
      PlacementUtils.register(
         context, MUSHROOM_ISLAND_VEGETATION, mushroomIslandVegetation, InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         TREES_MANGROVE,
         mangroveVegetation,
         CountPlacement.of(25),
         InSquarePlacement.spread(),
         SurfaceWaterDepthFilter.forMaxDepth(5),
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BiomeFilter.biome()
      );
   }
}
