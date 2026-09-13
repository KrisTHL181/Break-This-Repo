package net.minecraft.data.worldgen.placement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ClampedInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.NoiseThresholdCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import org.jspecify.annotations.Nullable;

public class VegetationPlacements {
   public static final ResourceKey<PlacedFeature> BAMBOO_LIGHT = PlacementUtils.createKey("bamboo_light");
   public static final ResourceKey<PlacedFeature> BAMBOO = PlacementUtils.createKey("bamboo");
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
   public static final ResourceKey<PlacedFeature> BAMBOO_VEGETATION = PlacementUtils.createKey("bamboo_vegetation");
   public static final ResourceKey<PlacedFeature> MUSHROOM_ISLAND_VEGETATION = PlacementUtils.createKey("mushroom_island_vegetation");
   public static final ResourceKey<PlacedFeature> TREES_MANGROVE = PlacementUtils.createKey("trees_mangrove");
   private static final PlacementModifier TREE_THRESHOLD = SurfaceWaterDepthFilter.forMaxDepth(0);

   public static List<PlacementModifier> worldSurfaceSquaredWithCount(int p_195475_) {
      return List.of(CountPlacement.of(p_195475_), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
   }

   private static List<PlacementModifier> getMushroomPlacement(int p_195477_, @Nullable PlacementModifier p_195478_) {
      Builder<PlacementModifier> builder = ImmutableList.builder();
      if (p_195478_ != null) {
         builder.add(p_195478_);
      }

      if (p_195477_ != 0) {
         builder.add(RarityFilter.onAverageOnceEvery(p_195477_));
      }

      builder.add(InSquarePlacement.spread());
      builder.add(PlacementUtils.HEIGHTMAP);
      builder.add(BiomeFilter.biome());
      return builder.build();
   }

   private static Builder<PlacementModifier> treePlacementBase(PlacementModifier p_195485_) {
      return ImmutableList.builder()
         .add(p_195485_)
         .add(InSquarePlacement.spread())
         .add(TREE_THRESHOLD)
         .add(PlacementUtils.HEIGHTMAP_OCEAN_FLOOR)
         .add(BiomeFilter.biome());
   }

   public static List<PlacementModifier> treePlacement(PlacementModifier p_195480_) {
      return treePlacementBase(p_195480_).build();
   }

   public static List<PlacementModifier> treePlacement(PlacementModifier p_195482_, Block p_195483_) {
      return treePlacementBase(p_195482_)
         .add(BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(p_195483_.defaultBlockState(), BlockPos.ZERO)))
         .build();
   }

   public static void bootstrap(BootstrapContext<PlacedFeature> p_333301_) {
      HolderGetter<ConfiguredFeature<?, ?>> holdergetter = p_333301_.lookup(Registries.CONFIGURED_FEATURE);
      Holder<ConfiguredFeature<?, ?>> holder = holdergetter.getOrThrow(VegetationFeatures.BAMBOO_NO_PODZOL);
      Holder<ConfiguredFeature<?, ?>> holder1 = holdergetter.getOrThrow(VegetationFeatures.BAMBOO_SOME_PODZOL);
      Holder<ConfiguredFeature<?, ?>> holder2 = holdergetter.getOrThrow(VegetationFeatures.VINES);
      Holder<ConfiguredFeature<?, ?>> holder3 = holdergetter.getOrThrow(VegetationFeatures.PATCH_SUNFLOWER);
      Holder<ConfiguredFeature<?, ?>> holder4 = holdergetter.getOrThrow(VegetationFeatures.PATCH_PUMPKIN);
      Holder<ConfiguredFeature<?, ?>> holder5 = holdergetter.getOrThrow(VegetationFeatures.PATCH_GRASS);
      Holder<ConfiguredFeature<?, ?>> holder6 = holdergetter.getOrThrow(VegetationFeatures.PATCH_GRASS_MEADOW);
      Holder<ConfiguredFeature<?, ?>> holder7 = holdergetter.getOrThrow(VegetationFeatures.PATCH_LEAF_LITTER);
      Holder<ConfiguredFeature<?, ?>> holder8 = holdergetter.getOrThrow(VegetationFeatures.PATCH_TAIGA_GRASS);
      Holder<ConfiguredFeature<?, ?>> holder9 = holdergetter.getOrThrow(VegetationFeatures.PATCH_GRASS_JUNGLE);
      Holder<ConfiguredFeature<?, ?>> holder10 = holdergetter.getOrThrow(VegetationFeatures.SINGLE_PIECE_OF_GRASS);
      Holder<ConfiguredFeature<?, ?>> holder11 = holdergetter.getOrThrow(VegetationFeatures.PATCH_DEAD_BUSH);
      Holder<ConfiguredFeature<?, ?>> holder12 = holdergetter.getOrThrow(VegetationFeatures.PATCH_DRY_GRASS);
      Holder<ConfiguredFeature<?, ?>> holder13 = holdergetter.getOrThrow(VegetationFeatures.PATCH_FIREFLY_BUSH);
      Holder<ConfiguredFeature<?, ?>> holder14 = holdergetter.getOrThrow(VegetationFeatures.PATCH_MELON);
      Holder<ConfiguredFeature<?, ?>> holder15 = holdergetter.getOrThrow(VegetationFeatures.PATCH_BERRY_BUSH);
      Holder<ConfiguredFeature<?, ?>> holder16 = holdergetter.getOrThrow(VegetationFeatures.PATCH_WATERLILY);
      Holder<ConfiguredFeature<?, ?>> holder17 = holdergetter.getOrThrow(VegetationFeatures.PATCH_TALL_GRASS);
      Holder<ConfiguredFeature<?, ?>> holder18 = holdergetter.getOrThrow(VegetationFeatures.PATCH_LARGE_FERN);
      Holder<ConfiguredFeature<?, ?>> holder19 = holdergetter.getOrThrow(VegetationFeatures.PATCH_BUSH);
      Holder<ConfiguredFeature<?, ?>> holder20 = holdergetter.getOrThrow(VegetationFeatures.PATCH_CACTUS);
      Holder<ConfiguredFeature<?, ?>> holder21 = holdergetter.getOrThrow(VegetationFeatures.PATCH_SUGAR_CANE);
      Holder<ConfiguredFeature<?, ?>> holder22 = holdergetter.getOrThrow(VegetationFeatures.PATCH_BROWN_MUSHROOM);
      Holder<ConfiguredFeature<?, ?>> holder23 = holdergetter.getOrThrow(VegetationFeatures.PATCH_RED_MUSHROOM);
      Holder<ConfiguredFeature<?, ?>> holder24 = holdergetter.getOrThrow(VegetationFeatures.FLOWER_DEFAULT);
      Holder<ConfiguredFeature<?, ?>> holder25 = holdergetter.getOrThrow(VegetationFeatures.FLOWER_FLOWER_FOREST);
      Holder<ConfiguredFeature<?, ?>> holder26 = holdergetter.getOrThrow(VegetationFeatures.FLOWER_SWAMP);
      Holder<ConfiguredFeature<?, ?>> holder27 = holdergetter.getOrThrow(VegetationFeatures.FLOWER_PLAIN);
      Holder<ConfiguredFeature<?, ?>> holder28 = holdergetter.getOrThrow(VegetationFeatures.FLOWER_MEADOW);
      Holder<ConfiguredFeature<?, ?>> holder29 = holdergetter.getOrThrow(VegetationFeatures.FLOWER_CHERRY);
      Holder<ConfiguredFeature<?, ?>> holder30 = holdergetter.getOrThrow(VegetationFeatures.FLOWER_PALE_GARDEN);
      Holder<ConfiguredFeature<?, ?>> holder31 = holdergetter.getOrThrow(VegetationFeatures.WILDFLOWERS_BIRCH_FOREST);
      Holder<ConfiguredFeature<?, ?>> holder32 = holdergetter.getOrThrow(VegetationFeatures.WILDFLOWERS_MEADOW);
      Holder<ConfiguredFeature<?, ?>> holder33 = holdergetter.getOrThrow(VegetationFeatures.TREES_PLAINS);
      Holder<ConfiguredFeature<?, ?>> holder34 = holdergetter.getOrThrow(VegetationFeatures.DARK_FOREST_VEGETATION);
      Holder<ConfiguredFeature<?, ?>> holder35 = holdergetter.getOrThrow(VegetationFeatures.PALE_GARDEN_VEGETATION);
      Holder<ConfiguredFeature<?, ?>> holder36 = holdergetter.getOrThrow(VegetationFeatures.FOREST_FLOWERS);
      Holder<ConfiguredFeature<?, ?>> holder37 = holdergetter.getOrThrow(VegetationFeatures.PALE_FOREST_FLOWERS);
      Holder<ConfiguredFeature<?, ?>> holder38 = holdergetter.getOrThrow(VegetationFeatures.PALE_MOSS_PATCH);
      Holder<ConfiguredFeature<?, ?>> holder39 = holdergetter.getOrThrow(VegetationFeatures.TREES_FLOWER_FOREST);
      Holder<ConfiguredFeature<?, ?>> holder40 = holdergetter.getOrThrow(VegetationFeatures.MEADOW_TREES);
      Holder<ConfiguredFeature<?, ?>> holder41 = holdergetter.getOrThrow(VegetationFeatures.TREES_TAIGA);
      Holder<ConfiguredFeature<?, ?>> holder42 = holdergetter.getOrThrow(VegetationFeatures.TREES_BADLANDS);
      Holder<ConfiguredFeature<?, ?>> holder43 = holdergetter.getOrThrow(VegetationFeatures.TREES_GROVE);
      Holder<ConfiguredFeature<?, ?>> holder44 = holdergetter.getOrThrow(VegetationFeatures.TREES_SNOWY);
      Holder<ConfiguredFeature<?, ?>> holder45 = holdergetter.getOrThrow(TreeFeatures.CHERRY_BEES_005);
      Holder<ConfiguredFeature<?, ?>> holder46 = holdergetter.getOrThrow(TreeFeatures.SWAMP_OAK);
      Holder<ConfiguredFeature<?, ?>> holder47 = holdergetter.getOrThrow(VegetationFeatures.TREES_SAVANNA);
      Holder<ConfiguredFeature<?, ?>> holder48 = holdergetter.getOrThrow(VegetationFeatures.BIRCH_TALL);
      Holder<ConfiguredFeature<?, ?>> holder49 = holdergetter.getOrThrow(VegetationFeatures.TREES_BIRCH);
      Holder<ConfiguredFeature<?, ?>> holder50 = holdergetter.getOrThrow(VegetationFeatures.TREES_WINDSWEPT_HILLS);
      Holder<ConfiguredFeature<?, ?>> holder51 = holdergetter.getOrThrow(VegetationFeatures.TREES_WATER);
      Holder<ConfiguredFeature<?, ?>> holder52 = holdergetter.getOrThrow(VegetationFeatures.TREES_BIRCH_AND_OAK_LEAF_LITTER);
      Holder<ConfiguredFeature<?, ?>> holder53 = holdergetter.getOrThrow(VegetationFeatures.TREES_SPARSE_JUNGLE);
      Holder<ConfiguredFeature<?, ?>> holder54 = holdergetter.getOrThrow(VegetationFeatures.TREES_OLD_GROWTH_SPRUCE_TAIGA);
      Holder<ConfiguredFeature<?, ?>> holder55 = holdergetter.getOrThrow(VegetationFeatures.TREES_OLD_GROWTH_PINE_TAIGA);
      Holder<ConfiguredFeature<?, ?>> holder56 = holdergetter.getOrThrow(VegetationFeatures.TREES_JUNGLE);
      Holder<ConfiguredFeature<?, ?>> holder57 = holdergetter.getOrThrow(VegetationFeatures.BAMBOO_VEGETATION);
      Holder<ConfiguredFeature<?, ?>> holder58 = holdergetter.getOrThrow(VegetationFeatures.MUSHROOM_ISLAND_VEGETATION);
      Holder<ConfiguredFeature<?, ?>> holder59 = holdergetter.getOrThrow(VegetationFeatures.MANGROVE_VEGETATION);
      PlacementUtils.register(
         p_333301_, BAMBOO_LIGHT, holder, RarityFilter.onAverageOnceEvery(4), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         BAMBOO,
         holder1,
         NoiseBasedCountPlacement.of(160, 80.0, 0.3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         VINES,
         holder2,
         CountPlacement.of(127),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(100)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, PATCH_SUNFLOWER, holder3, RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, PATCH_PUMPKIN, holder4, RarityFilter.onAverageOnceEvery(300), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_GRASS_PLAIN,
         holder5,
         NoiseThresholdCountPlacement.of(-0.8, 5, 10),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_GRASS_MEADOW,
         holder6,
         NoiseThresholdCountPlacement.of(-0.8, 5, 10),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(p_333301_, PATCH_GRASS_FOREST, holder5, worldSurfaceSquaredWithCount(2));
      PlacementUtils.register(p_333301_, PATCH_LEAF_LITTER, holder7, worldSurfaceSquaredWithCount(2));
      PlacementUtils.register(p_333301_, PATCH_GRASS_BADLANDS, holder5, InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
      PlacementUtils.register(p_333301_, PATCH_GRASS_SAVANNA, holder5, worldSurfaceSquaredWithCount(20));
      PlacementUtils.register(p_333301_, PATCH_GRASS_NORMAL, holder5, worldSurfaceSquaredWithCount(5));
      PlacementUtils.register(p_333301_, PATCH_GRASS_TAIGA_2, holder8, InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
      PlacementUtils.register(p_333301_, PATCH_GRASS_TAIGA, holder8, worldSurfaceSquaredWithCount(7));
      PlacementUtils.register(p_333301_, PATCH_GRASS_JUNGLE, holder9, worldSurfaceSquaredWithCount(25));
      PlacementUtils.register(p_333301_, GRASS_BONEMEAL, holder10, PlacementUtils.isEmpty());
      PlacementUtils.register(p_333301_, PATCH_DEAD_BUSH_2, holder11, worldSurfaceSquaredWithCount(2));
      PlacementUtils.register(p_333301_, PATCH_DEAD_BUSH, holder11, InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
      PlacementUtils.register(p_333301_, PATCH_DEAD_BUSH_BADLANDS, holder11, worldSurfaceSquaredWithCount(20));
      PlacementUtils.register(
         p_333301_,
         PATCH_DRY_GRASS_BADLANDS,
         holder12,
         RarityFilter.onAverageOnceEvery(6),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_DRY_GRASS_DESERT,
         holder12,
         RarityFilter.onAverageOnceEvery(3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, PATCH_MELON, holder14, RarityFilter.onAverageOnceEvery(6), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_MELON_SPARSE,
         holder14,
         RarityFilter.onAverageOnceEvery(64),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_BERRY_COMMON,
         holder15,
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_BERRY_RARE,
         holder15,
         RarityFilter.onAverageOnceEvery(384),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(p_333301_, PATCH_WATERLILY, holder16, worldSurfaceSquaredWithCount(4));
      PlacementUtils.register(
         p_333301_,
         PATCH_TALL_GRASS_2,
         holder17,
         NoiseThresholdCountPlacement.of(-0.8, 0, 7),
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, PATCH_TALL_GRASS, holder17, RarityFilter.onAverageOnceEvery(5), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, PATCH_LARGE_FERN, holder18, RarityFilter.onAverageOnceEvery(5), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, PATCH_BUSH, holder19, RarityFilter.onAverageOnceEvery(4), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_CACTUS_DESERT,
         holder20,
         RarityFilter.onAverageOnceEvery(6),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_CACTUS_DECORATED,
         holder20,
         RarityFilter.onAverageOnceEvery(13),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_SUGAR_CANE_SWAMP,
         holder21,
         RarityFilter.onAverageOnceEvery(3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(p_333301_, PATCH_SUGAR_CANE_DESERT, holder21, InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
      PlacementUtils.register(
         p_333301_,
         PATCH_SUGAR_CANE_BADLANDS,
         holder21,
         RarityFilter.onAverageOnceEvery(5),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, PATCH_SUGAR_CANE, holder21, RarityFilter.onAverageOnceEvery(6), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_FIREFLY_BUSH_NEAR_WATER,
         holder13,
         CountPlacement.of(2),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_NO_LEAVES,
         BiomeFilter.biome(),
         VegetationFeatures.nearWaterPredicate(Blocks.FIREFLY_BUSH)
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_FIREFLY_BUSH_NEAR_WATER_SWAMP,
         holder13,
         CountPlacement.of(3),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome(),
         VegetationFeatures.nearWaterPredicate(Blocks.FIREFLY_BUSH)
      );
      PlacementUtils.register(
         p_333301_,
         PATCH_FIREFLY_BUSH_SWAMP,
         holder13,
         RarityFilter.onAverageOnceEvery(8),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         BROWN_MUSHROOM_NETHER,
         holder22,
         RarityFilter.onAverageOnceEvery(2),
         InSquarePlacement.spread(),
         PlacementUtils.FULL_RANGE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         RED_MUSHROOM_NETHER,
         holder23,
         RarityFilter.onAverageOnceEvery(2),
         InSquarePlacement.spread(),
         PlacementUtils.FULL_RANGE,
         BiomeFilter.biome()
      );
      PlacementUtils.register(p_333301_, BROWN_MUSHROOM_NORMAL, holder22, getMushroomPlacement(256, null));
      PlacementUtils.register(p_333301_, RED_MUSHROOM_NORMAL, holder23, getMushroomPlacement(512, null));
      PlacementUtils.register(p_333301_, BROWN_MUSHROOM_TAIGA, holder22, getMushroomPlacement(4, null));
      PlacementUtils.register(p_333301_, RED_MUSHROOM_TAIGA, holder23, getMushroomPlacement(256, null));
      PlacementUtils.register(p_333301_, BROWN_MUSHROOM_OLD_GROWTH, holder22, getMushroomPlacement(4, CountPlacement.of(3)));
      PlacementUtils.register(p_333301_, RED_MUSHROOM_OLD_GROWTH, holder23, getMushroomPlacement(171, null));
      PlacementUtils.register(p_333301_, BROWN_MUSHROOM_SWAMP, holder22, getMushroomPlacement(0, CountPlacement.of(2)));
      PlacementUtils.register(p_333301_, RED_MUSHROOM_SWAMP, holder23, getMushroomPlacement(64, null));
      PlacementUtils.register(
         p_333301_, FLOWER_WARM, holder24, RarityFilter.onAverageOnceEvery(16), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, FLOWER_DEFAULT, holder24, RarityFilter.onAverageOnceEvery(32), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         FLOWER_FLOWER_FOREST,
         holder25,
         CountPlacement.of(3),
         RarityFilter.onAverageOnceEvery(2),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, FLOWER_SWAMP, holder26, RarityFilter.onAverageOnceEvery(32), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         FLOWER_PLAINS,
         holder27,
         NoiseThresholdCountPlacement.of(-0.8, 15, 4),
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         FLOWER_CHERRY,
         holder29,
         NoiseThresholdCountPlacement.of(-0.8, 5, 10),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(p_333301_, FLOWER_MEADOW, holder28, InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
      PlacementUtils.register(
         p_333301_,
         FLOWER_PALE_GARDEN,
         holder30,
         RarityFilter.onAverageOnceEvery(32),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         WILDFLOWERS_BIRCH_FOREST,
         holder31,
         CountPlacement.of(3),
         RarityFilter.onAverageOnceEvery(2),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         WILDFLOWERS_MEADOW,
         holder32,
         NoiseThresholdCountPlacement.of(-0.8, 5, 10),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         BiomeFilter.biome()
      );
      PlacementModifier placementmodifier = SurfaceWaterDepthFilter.forMaxDepth(0);
      PlacementUtils.register(
         p_333301_,
         TREES_PLAINS,
         holder33,
         PlacementUtils.countExtra(0, 0.05F, 1),
         InSquarePlacement.spread(),
         placementmodifier,
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(Blocks.OAK_SAPLING.defaultBlockState(), BlockPos.ZERO)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         DARK_FOREST_VEGETATION,
         holder34,
         CountPlacement.of(16),
         InSquarePlacement.spread(),
         placementmodifier,
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PALE_GARDEN_VEGETATION,
         holder35,
         CountPlacement.of(16),
         InSquarePlacement.spread(),
         placementmodifier,
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         FLOWER_FOREST_FLOWERS,
         holder36,
         RarityFilter.onAverageOnceEvery(7),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         CountPlacement.of(ClampedInt.of(UniformInt.of(-1, 3), 0, 3)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         FOREST_FLOWERS,
         holder36,
         RarityFilter.onAverageOnceEvery(7),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP,
         CountPlacement.of(ClampedInt.of(UniformInt.of(-3, 1), 0, 1)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_,
         PALE_GARDEN_FLOWERS,
         holder37,
         RarityFilter.onAverageOnceEvery(8),
         InSquarePlacement.spread(),
         PlacementUtils.HEIGHTMAP_NO_LEAVES,
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         p_333301_, PALE_MOSS_PATCH, holder38, CountPlacement.of(1), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_NO_LEAVES, BiomeFilter.biome()
      );
      PlacementUtils.register(p_333301_, TREES_FLOWER_FOREST, holder39, treePlacement(PlacementUtils.countExtra(6, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_MEADOW, holder40, treePlacement(RarityFilter.onAverageOnceEvery(100)));
      PlacementUtils.register(p_333301_, TREES_CHERRY, holder45, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1), Blocks.CHERRY_SAPLING));
      PlacementUtils.register(p_333301_, TREES_TAIGA, holder41, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_GROVE, holder43, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_BADLANDS, holder42, treePlacement(PlacementUtils.countExtra(5, 0.1F, 1), Blocks.OAK_SAPLING));
      PlacementUtils.register(p_333301_, TREES_SNOWY, holder44, treePlacement(PlacementUtils.countExtra(0, 0.1F, 1), Blocks.SPRUCE_SAPLING));
      PlacementUtils.register(
         p_333301_,
         TREES_SWAMP,
         holder46,
         PlacementUtils.countExtra(2, 0.1F, 1),
         InSquarePlacement.spread(),
         SurfaceWaterDepthFilter.forMaxDepth(2),
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BiomeFilter.biome(),
         BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(Blocks.OAK_SAPLING.defaultBlockState(), BlockPos.ZERO))
      );
      PlacementUtils.register(p_333301_, TREES_WINDSWEPT_SAVANNA, holder47, treePlacement(PlacementUtils.countExtra(2, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_SAVANNA, holder47, treePlacement(PlacementUtils.countExtra(1, 0.1F, 1)));
      PlacementUtils.register(p_333301_, BIRCH_TALL, holder48, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_BIRCH, holder49, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1), Blocks.BIRCH_SAPLING));
      PlacementUtils.register(p_333301_, TREES_WINDSWEPT_FOREST, holder50, treePlacement(PlacementUtils.countExtra(3, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_WINDSWEPT_HILLS, holder50, treePlacement(PlacementUtils.countExtra(0, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_WATER, holder51, treePlacement(PlacementUtils.countExtra(0, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_BIRCH_AND_OAK_LEAF_LITTER, holder52, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_SPARSE_JUNGLE, holder53, treePlacement(PlacementUtils.countExtra(2, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_OLD_GROWTH_SPRUCE_TAIGA, holder54, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_OLD_GROWTH_PINE_TAIGA, holder55, treePlacement(PlacementUtils.countExtra(10, 0.1F, 1)));
      PlacementUtils.register(p_333301_, TREES_JUNGLE, holder56, treePlacement(PlacementUtils.countExtra(50, 0.1F, 1)));
      PlacementUtils.register(p_333301_, BAMBOO_VEGETATION, holder57, treePlacement(PlacementUtils.countExtra(30, 0.1F, 1)));
      PlacementUtils.register(p_333301_, MUSHROOM_ISLAND_VEGETATION, holder58, InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
      PlacementUtils.register(
         p_333301_,
         TREES_MANGROVE,
         holder59,
         CountPlacement.of(25),
         InSquarePlacement.spread(),
         SurfaceWaterDepthFilter.forMaxDepth(5),
         PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
         BiomeFilter.biome()
      );
   }
}
