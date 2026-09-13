package net.minecraft.data.worldgen.features;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomBooleanFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.DualNoiseProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseThresholdProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Fluids;

public class VegetationFeatures {
   public static final ResourceKey<ConfiguredFeature<?, ?>> BAMBOO_NO_PODZOL = FeatureUtils.createKey("bamboo_no_podzol");
   public static final ResourceKey<ConfiguredFeature<?, ?>> BAMBOO_SOME_PODZOL = FeatureUtils.createKey("bamboo_some_podzol");
   public static final ResourceKey<ConfiguredFeature<?, ?>> VINES = FeatureUtils.createKey("vines");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_BROWN_MUSHROOM = FeatureUtils.createKey("patch_brown_mushroom");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_RED_MUSHROOM = FeatureUtils.createKey("patch_red_mushroom");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_SUNFLOWER = FeatureUtils.createKey("patch_sunflower");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_PUMPKIN = FeatureUtils.createKey("patch_pumpkin");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_BERRY_BUSH = FeatureUtils.createKey("patch_berry_bush");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_TAIGA_GRASS = FeatureUtils.createKey("patch_taiga_grass");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_GRASS = FeatureUtils.createKey("patch_grass");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_GRASS_MEADOW = FeatureUtils.createKey("patch_grass_meadow");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_GRASS_JUNGLE = FeatureUtils.createKey("patch_grass_jungle");
   public static final ResourceKey<ConfiguredFeature<?, ?>> SINGLE_PIECE_OF_GRASS = FeatureUtils.createKey("single_piece_of_grass");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_DEAD_BUSH = FeatureUtils.createKey("patch_dead_bush");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_DRY_GRASS = FeatureUtils.createKey("patch_dry_grass");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_MELON = FeatureUtils.createKey("patch_melon");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_WATERLILY = FeatureUtils.createKey("patch_waterlily");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_TALL_GRASS = FeatureUtils.createKey("patch_tall_grass");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_LARGE_FERN = FeatureUtils.createKey("patch_large_fern");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_BUSH = FeatureUtils.createKey("patch_bush");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_LEAF_LITTER = FeatureUtils.createKey("patch_leaf_litter");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_FIREFLY_BUSH = FeatureUtils.createKey("patch_firefly_bush");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_CACTUS = FeatureUtils.createKey("patch_cactus");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_SUGAR_CANE = FeatureUtils.createKey("patch_sugar_cane");
   public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_DEFAULT = FeatureUtils.createKey("flower_default");
   public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_FLOWER_FOREST = FeatureUtils.createKey("flower_flower_forest");
   public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_SWAMP = FeatureUtils.createKey("flower_swamp");
   public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_PLAIN = FeatureUtils.createKey("flower_plain");
   public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_MEADOW = FeatureUtils.createKey("flower_meadow");
   public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_CHERRY = FeatureUtils.createKey("flower_cherry");
   public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_PALE_GARDEN = FeatureUtils.createKey("flower_pale_garden");
   public static final ResourceKey<ConfiguredFeature<?, ?>> WILDFLOWERS_BIRCH_FOREST = FeatureUtils.createKey("wildflowers_birch_forest");
   public static final ResourceKey<ConfiguredFeature<?, ?>> WILDFLOWERS_MEADOW = FeatureUtils.createKey("wildflowers_meadow");
   public static final ResourceKey<ConfiguredFeature<?, ?>> FOREST_FLOWERS = FeatureUtils.createKey("forest_flowers");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_FOREST_FLOWERS = FeatureUtils.createKey("pale_forest_flowers");
   public static final ResourceKey<ConfiguredFeature<?, ?>> DARK_FOREST_VEGETATION = FeatureUtils.createKey("dark_forest_vegetation");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_GARDEN_VEGETATION = FeatureUtils.createKey("pale_garden_vegetation");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_MOSS_VEGETATION = FeatureUtils.createKey("pale_moss_vegetation");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_MOSS_PATCH = FeatureUtils.createKey("pale_moss_patch");
   public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_MOSS_PATCH_BONEMEAL = FeatureUtils.createKey("pale_moss_patch_bonemeal");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_FLOWER_FOREST = FeatureUtils.createKey("trees_flower_forest");
   public static final ResourceKey<ConfiguredFeature<?, ?>> MEADOW_TREES = FeatureUtils.createKey("meadow_trees");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_TAIGA = FeatureUtils.createKey("trees_taiga");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_BADLANDS = FeatureUtils.createKey("trees_badlands");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_GROVE = FeatureUtils.createKey("trees_grove");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_SAVANNA = FeatureUtils.createKey("trees_savanna");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_SNOWY = FeatureUtils.createKey("trees_snowy");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_BIRCH = FeatureUtils.createKey("trees_birch");
   public static final ResourceKey<ConfiguredFeature<?, ?>> BIRCH_TALL = FeatureUtils.createKey("birch_tall");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_WINDSWEPT_HILLS = FeatureUtils.createKey("trees_windswept_hills");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_WATER = FeatureUtils.createKey("trees_water");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_BIRCH_AND_OAK_LEAF_LITTER = FeatureUtils.createKey("trees_birch_and_oak_leaf_litter");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_PLAINS = FeatureUtils.createKey("trees_plains");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_SPARSE_JUNGLE = FeatureUtils.createKey("trees_sparse_jungle");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_OLD_GROWTH_SPRUCE_TAIGA = FeatureUtils.createKey("trees_old_growth_spruce_taiga");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_OLD_GROWTH_PINE_TAIGA = FeatureUtils.createKey("trees_old_growth_pine_taiga");
   public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_JUNGLE = FeatureUtils.createKey("trees_jungle");
   public static final ResourceKey<ConfiguredFeature<?, ?>> BAMBOO_VEGETATION = FeatureUtils.createKey("bamboo_vegetation");
   public static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_ISLAND_VEGETATION = FeatureUtils.createKey("mushroom_island_vegetation");
   public static final ResourceKey<ConfiguredFeature<?, ?>> MANGROVE_VEGETATION = FeatureUtils.createKey("mangrove_vegetation");
   private static final float FALLEN_TREE_ONE_IN_CHANCE = 80.0F;

   private static RandomPatchConfiguration grassPatch(BlockStateProvider p_195203_, int p_195204_) {
      return FeatureUtils.simpleRandomPatchConfiguration(p_195204_, PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(p_195203_)));
   }

   public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> p_335054_) {
      HolderGetter<ConfiguredFeature<?, ?>> holdergetter = p_335054_.lookup(Registries.CONFIGURED_FEATURE);
      Holder<ConfiguredFeature<?, ?>> holder = holdergetter.getOrThrow(TreeFeatures.HUGE_BROWN_MUSHROOM);
      Holder<ConfiguredFeature<?, ?>> holder1 = holdergetter.getOrThrow(TreeFeatures.HUGE_RED_MUSHROOM);
      Holder<ConfiguredFeature<?, ?>> holder2 = holdergetter.getOrThrow(TreeFeatures.FANCY_OAK_BEES_005);
      Holder<ConfiguredFeature<?, ?>> holder3 = holdergetter.getOrThrow(TreeFeatures.OAK_BEES_005);
      Holder<ConfiguredFeature<?, ?>> holder4 = holdergetter.getOrThrow(PATCH_GRASS_JUNGLE);
      HolderGetter<PlacedFeature> holdergetter1 = p_335054_.lookup(Registries.PLACED_FEATURE);
      Holder<PlacedFeature> holder5 = holdergetter1.getOrThrow(TreePlacements.PALE_OAK_CHECKED);
      Holder<PlacedFeature> holder6 = holdergetter1.getOrThrow(TreePlacements.PALE_OAK_CREAKING_CHECKED);
      Holder<PlacedFeature> holder7 = holdergetter1.getOrThrow(TreePlacements.FANCY_OAK_CHECKED);
      Holder<PlacedFeature> holder8 = holdergetter1.getOrThrow(TreePlacements.BIRCH_BEES_002);
      Holder<PlacedFeature> holder9 = holdergetter1.getOrThrow(TreePlacements.FANCY_OAK_BEES_002);
      Holder<PlacedFeature> holder10 = holdergetter1.getOrThrow(TreePlacements.FANCY_OAK_BEES);
      Holder<PlacedFeature> holder11 = holdergetter1.getOrThrow(TreePlacements.PINE_CHECKED);
      Holder<PlacedFeature> holder12 = holdergetter1.getOrThrow(TreePlacements.SPRUCE_CHECKED);
      Holder<PlacedFeature> holder13 = holdergetter1.getOrThrow(TreePlacements.PINE_ON_SNOW);
      Holder<PlacedFeature> holder14 = holdergetter1.getOrThrow(TreePlacements.ACACIA_CHECKED);
      Holder<PlacedFeature> holder15 = holdergetter1.getOrThrow(TreePlacements.SUPER_BIRCH_BEES_0002);
      Holder<PlacedFeature> holder16 = holdergetter1.getOrThrow(TreePlacements.BIRCH_BEES_0002_PLACED);
      Holder<PlacedFeature> holder17 = holdergetter1.getOrThrow(TreePlacements.BIRCH_BEES_0002_LEAF_LITTER);
      Holder<PlacedFeature> holder18 = holdergetter1.getOrThrow(TreePlacements.FANCY_OAK_BEES_0002_LEAF_LITTER);
      Holder<PlacedFeature> holder19 = holdergetter1.getOrThrow(TreePlacements.JUNGLE_BUSH);
      Holder<PlacedFeature> holder20 = holdergetter1.getOrThrow(TreePlacements.MEGA_SPRUCE_CHECKED);
      Holder<PlacedFeature> holder21 = holdergetter1.getOrThrow(TreePlacements.MEGA_PINE_CHECKED);
      Holder<PlacedFeature> holder22 = holdergetter1.getOrThrow(TreePlacements.MEGA_JUNGLE_TREE_CHECKED);
      Holder<PlacedFeature> holder23 = holdergetter1.getOrThrow(TreePlacements.TALL_MANGROVE_CHECKED);
      Holder<PlacedFeature> holder24 = holdergetter1.getOrThrow(TreePlacements.OAK_CHECKED);
      Holder<PlacedFeature> holder25 = holdergetter1.getOrThrow(TreePlacements.OAK_BEES_002);
      Holder<PlacedFeature> holder26 = holdergetter1.getOrThrow(TreePlacements.SUPER_BIRCH_BEES);
      Holder<PlacedFeature> holder27 = holdergetter1.getOrThrow(TreePlacements.SPRUCE_ON_SNOW);
      Holder<PlacedFeature> holder28 = holdergetter1.getOrThrow(TreePlacements.OAK_BEES_0002_LEAF_LITTER);
      Holder<PlacedFeature> holder29 = holdergetter1.getOrThrow(TreePlacements.JUNGLE_TREE_CHECKED);
      Holder<PlacedFeature> holder30 = holdergetter1.getOrThrow(TreePlacements.MANGROVE_CHECKED);
      Holder<PlacedFeature> holder31 = holdergetter1.getOrThrow(TreePlacements.OAK_LEAF_LITTER);
      Holder<PlacedFeature> holder32 = holdergetter1.getOrThrow(TreePlacements.DARK_OAK_LEAF_LITTER);
      Holder<PlacedFeature> holder33 = holdergetter1.getOrThrow(TreePlacements.BIRCH_LEAF_LITTER);
      Holder<PlacedFeature> holder34 = holdergetter1.getOrThrow(TreePlacements.FANCY_OAK_LEAF_LITTER);
      Holder<PlacedFeature> holder35 = holdergetter1.getOrThrow(TreePlacements.FALLEN_OAK_TREE);
      Holder<PlacedFeature> holder36 = holdergetter1.getOrThrow(TreePlacements.FALLEN_BIRCH_TREE);
      Holder<PlacedFeature> holder37 = holdergetter1.getOrThrow(TreePlacements.FALLEN_SUPER_BIRCH_TREE);
      Holder<PlacedFeature> holder38 = holdergetter1.getOrThrow(TreePlacements.FALLEN_JUNGLE_TREE);
      Holder<PlacedFeature> holder39 = holdergetter1.getOrThrow(TreePlacements.FALLEN_SPRUCE_TREE);
      FeatureUtils.register(p_335054_, BAMBOO_NO_PODZOL, Feature.BAMBOO, new ProbabilityFeatureConfiguration(0.0F));
      FeatureUtils.register(p_335054_, BAMBOO_SOME_PODZOL, Feature.BAMBOO, new ProbabilityFeatureConfiguration(0.2F));
      FeatureUtils.register(p_335054_, VINES, Feature.VINES);
      FeatureUtils.register(
         p_335054_,
         PATCH_BROWN_MUSHROOM,
         Feature.RANDOM_PATCH,
         FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.BROWN_MUSHROOM)))
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_RED_MUSHROOM,
         Feature.RANDOM_PATCH,
         FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.RED_MUSHROOM)))
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_SUNFLOWER,
         Feature.RANDOM_PATCH,
         FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.SUNFLOWER)))
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_PUMPKIN,
         Feature.RANDOM_PATCH,
         FeatureUtils.simplePatchConfiguration(
            Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.PUMPKIN)), List.of(Blocks.GRASS_BLOCK)
         )
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_BERRY_BUSH,
         Feature.RANDOM_PATCH,
         FeatureUtils.simplePatchConfiguration(
            Feature.SIMPLE_BLOCK,
            new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3))),
            List.of(Blocks.GRASS_BLOCK)
         )
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_TAIGA_GRASS,
         Feature.RANDOM_PATCH,
         grassPatch(
            new WeightedStateProvider(WeightedList.<BlockState>builder().add(Blocks.SHORT_GRASS.defaultBlockState(), 1).add(Blocks.FERN.defaultBlockState(), 4)),
            32
         )
      );
      FeatureUtils.register(p_335054_, PATCH_GRASS, Feature.RANDOM_PATCH, grassPatch(BlockStateProvider.simple(Blocks.SHORT_GRASS), 32));
      FeatureUtils.register(p_335054_, PATCH_GRASS_MEADOW, Feature.RANDOM_PATCH, grassPatch(BlockStateProvider.simple(Blocks.SHORT_GRASS), 16));
      FeatureUtils.register(
         p_335054_,
         PATCH_LEAF_LITTER,
         Feature.RANDOM_PATCH,
         FeatureUtils.simpleRandomPatchConfiguration(
            32,
            PlacementUtils.filtered(
               Feature.SIMPLE_BLOCK,
               new SimpleBlockConfiguration(new WeightedStateProvider(leafLitterPatchBuilder(1, 3))),
               BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(), Blocks.GRASS_BLOCK))
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_GRASS_JUNGLE,
         Feature.RANDOM_PATCH,
         new RandomPatchConfiguration(
            32,
            7,
            3,
            PlacementUtils.filtered(
               Feature.SIMPLE_BLOCK,
               new SimpleBlockConfiguration(
                  new WeightedStateProvider(
                     WeightedList.<BlockState>builder().add(Blocks.SHORT_GRASS.defaultBlockState(), 3).add(Blocks.FERN.defaultBlockState(), 1)
                  )
               ),
               BlockPredicate.allOf(
                  BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.not(BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(), Blocks.PODZOL))
               )
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         SINGLE_PIECE_OF_GRASS,
         Feature.SIMPLE_BLOCK,
         new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.SHORT_GRASS.defaultBlockState()))
      );
      FeatureUtils.register(p_335054_, PATCH_DEAD_BUSH, Feature.RANDOM_PATCH, grassPatch(BlockStateProvider.simple(Blocks.DEAD_BUSH), 4));
      FeatureUtils.register(
         p_335054_,
         PATCH_DRY_GRASS,
         Feature.RANDOM_PATCH,
         grassPatch(
            new WeightedStateProvider(
               WeightedList.<BlockState>builder().add(Blocks.SHORT_DRY_GRASS.defaultBlockState(), 1).add(Blocks.TALL_DRY_GRASS.defaultBlockState(), 1)
            ),
            64
         )
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_MELON,
         Feature.RANDOM_PATCH,
         new RandomPatchConfiguration(
            64,
            7,
            3,
            PlacementUtils.filtered(
               Feature.SIMPLE_BLOCK,
               new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MELON)),
               BlockPredicate.allOf(
                  BlockPredicate.replaceable(), BlockPredicate.noFluid(), BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(), Blocks.GRASS_BLOCK)
               )
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_WATERLILY,
         Feature.RANDOM_PATCH,
         new RandomPatchConfiguration(
            10, 7, 3, PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LILY_PAD)))
         )
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_TALL_GRASS,
         Feature.RANDOM_PATCH,
         FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.TALL_GRASS)))
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_LARGE_FERN,
         Feature.RANDOM_PATCH,
         FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LARGE_FERN)))
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_BUSH,
         Feature.RANDOM_PATCH,
         new RandomPatchConfiguration(
            24, 5, 3, PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.BUSH)))
         )
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_CACTUS,
         Feature.RANDOM_PATCH,
         FeatureUtils.simpleRandomPatchConfiguration(
            10,
            PlacementUtils.inlinePlaced(
               Feature.BLOCK_COLUMN,
               new BlockColumnConfiguration(
                  List.of(
                     BlockColumnConfiguration.layer(BiasedToBottomInt.of(1, 3), BlockStateProvider.simple(Blocks.CACTUS)),
                     BlockColumnConfiguration.layer(
                        new WeightedListInt(WeightedList.<IntProvider>builder().add(ConstantInt.of(0), 3).add(ConstantInt.of(1), 1).build()),
                        BlockStateProvider.simple(Blocks.CACTUS_FLOWER)
                     )
                  ),
                  Direction.UP,
                  BlockPredicate.ONLY_IN_AIR_PREDICATE,
                  false
               ),
               BlockPredicateFilter.forPredicate(
                  BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.wouldSurvive(Blocks.CACTUS.defaultBlockState(), BlockPos.ZERO))
               )
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_SUGAR_CANE,
         Feature.RANDOM_PATCH,
         new RandomPatchConfiguration(
            20,
            4,
            0,
            PlacementUtils.inlinePlaced(
               Feature.BLOCK_COLUMN,
               BlockColumnConfiguration.simple(BiasedToBottomInt.of(2, 4), BlockStateProvider.simple(Blocks.SUGAR_CANE)),
               nearWaterPredicate(Blocks.SUGAR_CANE)
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         PATCH_FIREFLY_BUSH,
         Feature.RANDOM_PATCH,
         new RandomPatchConfiguration(
            20, 4, 3, PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.FIREFLY_BUSH)))
         )
      );
      FeatureUtils.register(
         p_335054_,
         FLOWER_DEFAULT,
         Feature.FLOWER,
         grassPatch(
            new WeightedStateProvider(WeightedList.<BlockState>builder().add(Blocks.POPPY.defaultBlockState(), 2).add(Blocks.DANDELION.defaultBlockState(), 1)),
            64
         )
      );
      FeatureUtils.register(
         p_335054_,
         FLOWER_FLOWER_FOREST,
         Feature.FLOWER,
         new RandomPatchConfiguration(
            96,
            6,
            2,
            PlacementUtils.onlyWhenEmpty(
               Feature.SIMPLE_BLOCK,
               new SimpleBlockConfiguration(
                  new NoiseProvider(
                     2345L,
                     new NormalNoise.NoiseParameters(0, 1.0),
                     0.020833334F,
                     List.of(
                        Blocks.DANDELION.defaultBlockState(),
                        Blocks.POPPY.defaultBlockState(),
                        Blocks.ALLIUM.defaultBlockState(),
                        Blocks.AZURE_BLUET.defaultBlockState(),
                        Blocks.RED_TULIP.defaultBlockState(),
                        Blocks.ORANGE_TULIP.defaultBlockState(),
                        Blocks.WHITE_TULIP.defaultBlockState(),
                        Blocks.PINK_TULIP.defaultBlockState(),
                        Blocks.OXEYE_DAISY.defaultBlockState(),
                        Blocks.CORNFLOWER.defaultBlockState(),
                        Blocks.LILY_OF_THE_VALLEY.defaultBlockState()
                     )
                  )
               )
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         FLOWER_SWAMP,
         Feature.FLOWER,
         new RandomPatchConfiguration(
            64, 6, 2, PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.BLUE_ORCHID)))
         )
      );
      FeatureUtils.register(
         p_335054_,
         FLOWER_PLAIN,
         Feature.FLOWER,
         new RandomPatchConfiguration(
            64,
            6,
            2,
            PlacementUtils.onlyWhenEmpty(
               Feature.SIMPLE_BLOCK,
               new SimpleBlockConfiguration(
                  new NoiseThresholdProvider(
                     2345L,
                     new NormalNoise.NoiseParameters(0, 1.0),
                     0.005F,
                     -0.8F,
                     0.33333334F,
                     Blocks.DANDELION.defaultBlockState(),
                     List.of(
                        Blocks.ORANGE_TULIP.defaultBlockState(),
                        Blocks.RED_TULIP.defaultBlockState(),
                        Blocks.PINK_TULIP.defaultBlockState(),
                        Blocks.WHITE_TULIP.defaultBlockState()
                     ),
                     List.of(
                        Blocks.POPPY.defaultBlockState(),
                        Blocks.AZURE_BLUET.defaultBlockState(),
                        Blocks.OXEYE_DAISY.defaultBlockState(),
                        Blocks.CORNFLOWER.defaultBlockState()
                     )
                  )
               )
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         FLOWER_MEADOW,
         Feature.FLOWER,
         new RandomPatchConfiguration(
            96,
            6,
            2,
            PlacementUtils.onlyWhenEmpty(
               Feature.SIMPLE_BLOCK,
               new SimpleBlockConfiguration(
                  new DualNoiseProvider(
                     new InclusiveRange<>(1, 3),
                     new NormalNoise.NoiseParameters(-10, 1.0),
                     1.0F,
                     2345L,
                     new NormalNoise.NoiseParameters(-3, 1.0),
                     1.0F,
                     List.of(
                        Blocks.TALL_GRASS.defaultBlockState(),
                        Blocks.ALLIUM.defaultBlockState(),
                        Blocks.POPPY.defaultBlockState(),
                        Blocks.AZURE_BLUET.defaultBlockState(),
                        Blocks.DANDELION.defaultBlockState(),
                        Blocks.CORNFLOWER.defaultBlockState(),
                        Blocks.OXEYE_DAISY.defaultBlockState(),
                        Blocks.SHORT_GRASS.defaultBlockState()
                     )
                  )
               )
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         FLOWER_CHERRY,
         Feature.FLOWER,
         new RandomPatchConfiguration(
            96,
            6,
            2,
            PlacementUtils.onlyWhenEmpty(
               Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(flowerBedPatchBuilder(Blocks.PINK_PETALS)))
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         WILDFLOWERS_BIRCH_FOREST,
         Feature.FLOWER,
         new RandomPatchConfiguration(
            64,
            6,
            2,
            PlacementUtils.onlyWhenEmpty(
               Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(flowerBedPatchBuilder(Blocks.WILDFLOWERS)))
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         WILDFLOWERS_MEADOW,
         Feature.FLOWER,
         new RandomPatchConfiguration(
            8,
            6,
            2,
            PlacementUtils.onlyWhenEmpty(
               Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(flowerBedPatchBuilder(Blocks.WILDFLOWERS)))
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         FLOWER_PALE_GARDEN,
         Feature.FLOWER,
         new RandomPatchConfiguration(
            1,
            0,
            0,
            PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.CLOSED_EYEBLOSSOM), true))
         )
      );
      FeatureUtils.register(
         p_335054_,
         FOREST_FLOWERS,
         Feature.SIMPLE_RANDOM_SELECTOR,
         new SimpleRandomFeatureConfiguration(
            HolderSet.direct(
               PlacementUtils.inlinePlaced(
                  Feature.RANDOM_PATCH,
                  FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LILAC)))
               ),
               PlacementUtils.inlinePlaced(
                  Feature.RANDOM_PATCH,
                  FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.ROSE_BUSH)))
               ),
               PlacementUtils.inlinePlaced(
                  Feature.RANDOM_PATCH,
                  FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.PEONY)))
               ),
               PlacementUtils.inlinePlaced(
                  Feature.NO_BONEMEAL_FLOWER,
                  FeatureUtils.simplePatchConfiguration(
                     Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LILY_OF_THE_VALLEY))
                  )
               )
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         PALE_FOREST_FLOWERS,
         Feature.RANDOM_PATCH,
         FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.CLOSED_EYEBLOSSOM), true))
      );
      FeatureUtils.register(
         p_335054_,
         DARK_FOREST_VEGETATION,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(
               new WeightedPlacedFeature(PlacementUtils.inlinePlaced(holder), 0.025F),
               new WeightedPlacedFeature(PlacementUtils.inlinePlaced(holder1), 0.05F),
               new WeightedPlacedFeature(holder32, 0.6666667F),
               new WeightedPlacedFeature(holder36, 0.0025F),
               new WeightedPlacedFeature(holder33, 0.2F),
               new WeightedPlacedFeature(holder35, 0.0125F),
               new WeightedPlacedFeature(holder34, 0.1F)
            ),
            holder31
         )
      );
      FeatureUtils.register(
         p_335054_,
         PALE_GARDEN_VEGETATION,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder6, 0.1F), new WeightedPlacedFeature(holder5, 0.9F)), holder5)
      );
      FeatureUtils.register(
         p_335054_,
         PALE_MOSS_VEGETATION,
         Feature.SIMPLE_BLOCK,
         new SimpleBlockConfiguration(
            new WeightedStateProvider(
               WeightedList.<BlockState>builder()
                  .add(Blocks.PALE_MOSS_CARPET.defaultBlockState(), 25)
                  .add(Blocks.SHORT_GRASS.defaultBlockState(), 25)
                  .add(Blocks.TALL_GRASS.defaultBlockState(), 10)
            )
         )
      );
      FeatureUtils.register(
         p_335054_,
         PALE_MOSS_PATCH,
         Feature.VEGETATION_PATCH,
         new VegetationPatchConfiguration(
            BlockTags.MOSS_REPLACEABLE,
            BlockStateProvider.simple(Blocks.PALE_MOSS_BLOCK),
            PlacementUtils.inlinePlaced(holdergetter.getOrThrow(PALE_MOSS_VEGETATION)),
            CaveSurface.FLOOR,
            ConstantInt.of(1),
            0.0F,
            5,
            0.3F,
            UniformInt.of(2, 4),
            0.75F
         )
      );
      FeatureUtils.register(
         p_335054_,
         PALE_MOSS_PATCH_BONEMEAL,
         Feature.VEGETATION_PATCH,
         new VegetationPatchConfiguration(
            BlockTags.MOSS_REPLACEABLE,
            BlockStateProvider.simple(Blocks.PALE_MOSS_BLOCK),
            PlacementUtils.inlinePlaced(holdergetter.getOrThrow(PALE_MOSS_VEGETATION)),
            CaveSurface.FLOOR,
            ConstantInt.of(1),
            0.0F,
            5,
            0.6F,
            UniformInt.of(1, 2),
            0.75F
         )
      );
      FeatureUtils.register(
         p_335054_,
         TREES_FLOWER_FOREST,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(new WeightedPlacedFeature(holder36, 0.0025F), new WeightedPlacedFeature(holder8, 0.2F), new WeightedPlacedFeature(holder9, 0.1F)), holder25
         )
      );
      FeatureUtils.register(
         p_335054_, MEADOW_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder10, 0.5F)), holder26)
      );
      FeatureUtils.register(
         p_335054_,
         TREES_TAIGA,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder11, 0.33333334F), new WeightedPlacedFeature(holder39, 0.0125F)), holder12)
      );
      FeatureUtils.register(
         p_335054_, TREES_BADLANDS, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder35, 0.0125F)), holder31)
      );
      FeatureUtils.register(
         p_335054_, TREES_GROVE, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder13, 0.33333334F)), holder27)
      );
      FeatureUtils.register(
         p_335054_,
         TREES_SAVANNA,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder14, 0.8F), new WeightedPlacedFeature(holder35, 0.0125F)), holder24)
      );
      FeatureUtils.register(
         p_335054_, TREES_SNOWY, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder39, 0.0125F)), holder12)
      );
      FeatureUtils.register(
         p_335054_, TREES_BIRCH, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder36, 0.0125F)), holder16)
      );
      FeatureUtils.register(
         p_335054_,
         BIRCH_TALL,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(new WeightedPlacedFeature(holder37, 0.00625F), new WeightedPlacedFeature(holder15, 0.5F), new WeightedPlacedFeature(holder36, 0.0125F)),
            holder16
         )
      );
      FeatureUtils.register(
         p_335054_,
         TREES_WINDSWEPT_HILLS,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(
               new WeightedPlacedFeature(holder39, 0.008325F),
               new WeightedPlacedFeature(holder12, 0.666F),
               new WeightedPlacedFeature(holder7, 0.1F),
               new WeightedPlacedFeature(holder35, 0.0125F)
            ),
            holder24
         )
      );
      FeatureUtils.register(
         p_335054_, TREES_WATER, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder7, 0.1F)), holder24)
      );
      FeatureUtils.register(
         p_335054_,
         TREES_BIRCH_AND_OAK_LEAF_LITTER,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(
               new WeightedPlacedFeature(holder36, 0.0025F),
               new WeightedPlacedFeature(holder17, 0.2F),
               new WeightedPlacedFeature(holder18, 0.1F),
               new WeightedPlacedFeature(holder35, 0.0125F)
            ),
            holder28
         )
      );
      FeatureUtils.register(
         p_335054_,
         TREES_PLAINS,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(holder2), 0.33333334F), new WeightedPlacedFeature(holder35, 0.0125F)),
            PlacementUtils.inlinePlaced(holder3)
         )
      );
      FeatureUtils.register(
         p_335054_,
         TREES_SPARSE_JUNGLE,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(new WeightedPlacedFeature(holder7, 0.1F), new WeightedPlacedFeature(holder19, 0.5F), new WeightedPlacedFeature(holder38, 0.0125F)),
            holder29
         )
      );
      FeatureUtils.register(
         p_335054_,
         TREES_OLD_GROWTH_SPRUCE_TAIGA,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(
               new WeightedPlacedFeature(holder20, 0.33333334F), new WeightedPlacedFeature(holder11, 0.33333334F), new WeightedPlacedFeature(holder39, 0.0125F)
            ),
            holder12
         )
      );
      FeatureUtils.register(
         p_335054_,
         TREES_OLD_GROWTH_PINE_TAIGA,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(
               new WeightedPlacedFeature(holder20, 0.025641026F),
               new WeightedPlacedFeature(holder21, 0.30769232F),
               new WeightedPlacedFeature(holder11, 0.33333334F),
               new WeightedPlacedFeature(holder39, 0.0125F)
            ),
            holder12
         )
      );
      FeatureUtils.register(
         p_335054_,
         TREES_JUNGLE,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(
               new WeightedPlacedFeature(holder7, 0.1F),
               new WeightedPlacedFeature(holder19, 0.5F),
               new WeightedPlacedFeature(holder22, 0.33333334F),
               new WeightedPlacedFeature(holder38, 0.0125F)
            ),
            holder29
         )
      );
      FeatureUtils.register(
         p_335054_,
         BAMBOO_VEGETATION,
         Feature.RANDOM_SELECTOR,
         new RandomFeatureConfiguration(
            List.of(new WeightedPlacedFeature(holder7, 0.05F), new WeightedPlacedFeature(holder19, 0.15F), new WeightedPlacedFeature(holder22, 0.7F)),
            PlacementUtils.inlinePlaced(holder4)
         )
      );
      FeatureUtils.register(
         p_335054_,
         MUSHROOM_ISLAND_VEGETATION,
         Feature.RANDOM_BOOLEAN_SELECTOR,
         new RandomBooleanFeatureConfiguration(PlacementUtils.inlinePlaced(holder1), PlacementUtils.inlinePlaced(holder))
      );
      FeatureUtils.register(
         p_335054_, MANGROVE_VEGETATION, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(holder23, 0.85F)), holder30)
      );
   }

   private static WeightedList.Builder<BlockState> flowerBedPatchBuilder(Block p_394149_) {
      return segmentedBlockPatchBuilder(p_394149_, 1, 4, FlowerBedBlock.AMOUNT, FlowerBedBlock.FACING);
   }

   public static WeightedList.Builder<BlockState> leafLitterPatchBuilder(int p_396969_, int p_394395_) {
      return segmentedBlockPatchBuilder(Blocks.LEAF_LITTER, p_396969_, p_394395_, LeafLitterBlock.AMOUNT, LeafLitterBlock.FACING);
   }

   private static WeightedList.Builder<BlockState> segmentedBlockPatchBuilder(
      Block p_392074_, int p_395090_, int p_391445_, IntegerProperty p_392060_, EnumProperty<Direction> p_396039_
   ) {
      WeightedList.Builder<BlockState> builder = WeightedList.builder();

      for (int i = p_395090_; i <= p_391445_; i++) {
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            builder.add(p_392074_.defaultBlockState().setValue(p_392060_, i).setValue(p_396039_, direction), 1);
         }
      }

      return builder;
   }

   public static BlockPredicateFilter nearWaterPredicate(Block p_398034_) {
      return BlockPredicateFilter.forPredicate(
         BlockPredicate.allOf(
            BlockPredicate.ONLY_IN_AIR_PREDICATE,
            BlockPredicate.wouldSurvive(p_398034_.defaultBlockState(), BlockPos.ZERO),
            BlockPredicate.anyOf(
               BlockPredicate.matchesFluids(new BlockPos(1, -1, 0), Fluids.WATER, Fluids.FLOWING_WATER),
               BlockPredicate.matchesFluids(new BlockPos(-1, -1, 0), Fluids.WATER, Fluids.FLOWING_WATER),
               BlockPredicate.matchesFluids(new BlockPos(0, -1, 1), Fluids.WATER, Fluids.FLOWING_WATER),
               BlockPredicate.matchesFluids(new BlockPos(0, -1, -1), Fluids.WATER, Fluids.FLOWING_WATER)
            )
         )
      );
   }
}
