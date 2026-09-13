package net.minecraft.data.worldgen.features;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ClampedInt;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.MultifaceSpreadeableBlock;
import net.minecraft.world.level.block.SmallDripleafBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.BlockColumnFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FossilFeature;
import net.minecraft.world.level.levelgen.feature.GeodeFeature;
import net.minecraft.world.level.levelgen.feature.LargeDripstoneFeature;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import net.minecraft.world.level.levelgen.feature.MultifaceGrowthFeature;
import net.minecraft.world.level.levelgen.feature.RandomBooleanSelectorFeature;
import net.minecraft.world.level.levelgen.feature.RootSystemFeature;
import net.minecraft.world.level.levelgen.feature.SculkPatchFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.SimpleRandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.SpeleothemClusterFeature;
import net.minecraft.world.level.levelgen.feature.SpeleothemFeature;
import net.minecraft.world.level.levelgen.feature.UnderwaterMagmaFeature;
import net.minecraft.world.level.levelgen.feature.VegetationPatchFeature;
import net.minecraft.world.level.levelgen.feature.WaterloggedVegetationPatchFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class CaveFeatures {
   public static final ResourceKey<Feature> MONSTER_ROOM = FeatureUtils.createKey("monster_room");
   public static final ResourceKey<Feature> FOSSIL_COAL = FeatureUtils.createKey("fossil_coal");
   public static final ResourceKey<Feature> FOSSIL_DIAMONDS = FeatureUtils.createKey("fossil_diamonds");
   public static final ResourceKey<Feature> DRIPSTONE_CLUSTER = FeatureUtils.createKey("dripstone_cluster");
   public static final ResourceKey<Feature> SULFUR_SPIKE_CLUSTER = FeatureUtils.createKey("sulfur_spike_cluster");
   public static final ResourceKey<Feature> LARGE_DRIPSTONE = FeatureUtils.createKey("large_dripstone");
   public static final ResourceKey<Feature> POINTED_DRIPSTONE = FeatureUtils.createKey("pointed_dripstone");
   public static final ResourceKey<Feature> SULFUR_SPIKE = FeatureUtils.createKey("sulfur_spike");
   public static final ResourceKey<Feature> UNDERWATER_MAGMA = FeatureUtils.createKey("underwater_magma");
   public static final ResourceKey<Feature> GLOW_LICHEN = FeatureUtils.createKey("glow_lichen");
   public static final ResourceKey<Feature> ROOTED_AZALEA_TREE = FeatureUtils.createKey("rooted_azalea_tree");
   public static final ResourceKey<Feature> ROOTED_SULFUR_SPRING = FeatureUtils.createKey("rooted_sulfur_spring");
   public static final ResourceKey<Feature> CAVE_VINE = FeatureUtils.createKey("cave_vine");
   public static final ResourceKey<Feature> CAVE_VINE_IN_MOSS = FeatureUtils.createKey("cave_vine_in_moss");
   public static final ResourceKey<Feature> MOSS_VEGETATION = FeatureUtils.createKey("moss_vegetation");
   public static final ResourceKey<Feature> MOSS_PATCH = FeatureUtils.createKey("moss_patch");
   public static final ResourceKey<Feature> MOSS_PATCH_BONEMEAL = FeatureUtils.createKey("moss_patch_bonemeal");
   public static final ResourceKey<Feature> DRIPLEAF = FeatureUtils.createKey("dripleaf");
   public static final ResourceKey<Feature> CLAY_WITH_DRIPLEAVES = FeatureUtils.createKey("clay_with_dripleaves");
   public static final ResourceKey<Feature> CLAY_POOL_WITH_DRIPLEAVES = FeatureUtils.createKey("clay_pool_with_dripleaves");
   public static final ResourceKey<Feature> LUSH_CAVES_CLAY = FeatureUtils.createKey("lush_caves_clay");
   public static final ResourceKey<Feature> MOSS_PATCH_CEILING = FeatureUtils.createKey("moss_patch_ceiling");
   public static final ResourceKey<Feature> SPORE_BLOSSOM = FeatureUtils.createKey("spore_blossom");
   public static final ResourceKey<Feature> AMETHYST_GEODE = FeatureUtils.createKey("amethyst_geode");
   public static final ResourceKey<Feature> SCULK_PATCH_DEEP_DARK = FeatureUtils.createKey("sculk_patch_deep_dark");
   public static final ResourceKey<Feature> SCULK_PATCH_ANCIENT_CITY = FeatureUtils.createKey("sculk_patch_ancient_city");
   public static final ResourceKey<Feature> SCULK_VEIN = FeatureUtils.createKey("sculk_vein");

   private static Holder<PlacedFeature> makeDripleaf(final Direction direction) {
      return PlacementUtils.inlinePlaced(
         new BlockColumnFeature(
            List.of(
               BlockColumnFeature.layer(
                  new WeightedListInt(WeightedList.<IntProvider>builder().add(UniformInt.of(0, 4), 2).add(ConstantInt.of(0), 1).build()),
                  BlockStateProvider.simple(Blocks.BIG_DRIPLEAF_STEM.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction))
               ),
               BlockColumnFeature.layer(
                  ConstantInt.of(1),
                  BlockStateProvider.simple(Blocks.BIG_DRIPLEAF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction))
               )
            ),
            Direction.UP,
            BlockPredicate.ONLY_IN_AIR_OR_WATER_PREDICATE,
            true
         )
      );
   }

   private static Holder<PlacedFeature> makeSmallDripleaf() {
      return PlacementUtils.inlinePlaced(
         new SimpleBlockFeature(
            new WeightedStateProvider(
               WeightedList.<BlockState>builder()
                  .add(Blocks.SMALL_DRIPLEAF.defaultBlockState().setValue(SmallDripleafBlock.FACING, Direction.EAST), 1)
                  .add(Blocks.SMALL_DRIPLEAF.defaultBlockState().setValue(SmallDripleafBlock.FACING, Direction.WEST), 1)
                  .add(Blocks.SMALL_DRIPLEAF.defaultBlockState().setValue(SmallDripleafBlock.FACING, Direction.NORTH), 1)
                  .add(Blocks.SMALL_DRIPLEAF.defaultBlockState().setValue(SmallDripleafBlock.FACING, Direction.SOUTH), 1)
            )
         )
      );
   }

   public static void bootstrap(final BootstrapContext<Feature> context) {
      HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
      HolderGetter<Feature> configuredFeatures = context.lookup(Registries.FEATURE);
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      context.register(MONSTER_ROOM, new MonsterRoomFeature());
      List<Identifier> fossilStructures = List.of(
         Identifier.withDefaultNamespace("fossil/spine_1"),
         Identifier.withDefaultNamespace("fossil/spine_2"),
         Identifier.withDefaultNamespace("fossil/spine_3"),
         Identifier.withDefaultNamespace("fossil/spine_4"),
         Identifier.withDefaultNamespace("fossil/skull_1"),
         Identifier.withDefaultNamespace("fossil/skull_2"),
         Identifier.withDefaultNamespace("fossil/skull_3"),
         Identifier.withDefaultNamespace("fossil/skull_4")
      );
      List<Identifier> fossilCoalStructures = List.of(
         Identifier.withDefaultNamespace("fossil/spine_1_coal"),
         Identifier.withDefaultNamespace("fossil/spine_2_coal"),
         Identifier.withDefaultNamespace("fossil/spine_3_coal"),
         Identifier.withDefaultNamespace("fossil/spine_4_coal"),
         Identifier.withDefaultNamespace("fossil/skull_1_coal"),
         Identifier.withDefaultNamespace("fossil/skull_2_coal"),
         Identifier.withDefaultNamespace("fossil/skull_3_coal"),
         Identifier.withDefaultNamespace("fossil/skull_4_coal")
      );
      Holder<StructureProcessorList> fossilRot = processorLists.getOrThrow(ProcessorLists.FOSSIL_ROT);
      context.register(
         FOSSIL_COAL, new FossilFeature(fossilStructures, fossilCoalStructures, fossilRot, processorLists.getOrThrow(ProcessorLists.FOSSIL_COAL), 4)
      );
      context.register(
         FOSSIL_DIAMONDS, new FossilFeature(fossilStructures, fossilCoalStructures, fossilRot, processorLists.getOrThrow(ProcessorLists.FOSSIL_DIAMONDS), 4)
      );
      context.register(
         DRIPSTONE_CLUSTER,
         new SpeleothemClusterFeature(
            Blocks.DRIPSTONE_BLOCK.defaultBlockState(),
            Blocks.POINTED_DRIPSTONE.defaultBlockState(),
            blocks.getOrThrow(BlockTags.DRIPSTONE_REPLACEABLE),
            12,
            UniformInt.of(3, 6),
            UniformInt.of(2, 8),
            1,
            3,
            UniformInt.of(2, 4),
            UniformFloat.of(0.3F, 0.7F),
            ClampedNormalFloat.of(0.1F, 0.3F, 0.1F, 0.9F),
            0.1F,
            3,
            8
         )
      );
      context.register(
         LARGE_DRIPSTONE,
         new LargeDripstoneFeature(
            blocks.getOrThrow(BlockTags.DRIPSTONE_REPLACEABLE),
            30,
            ClampedInt.of(UniformInt.of(3, 19), 3, 16),
            UniformFloat.of(0.4F, 2.0F),
            0.33F,
            UniformFloat.of(0.3F, 0.9F),
            UniformFloat.of(0.4F, 1.0F),
            UniformFloat.of(0.0F, 0.3F),
            4,
            0.6F
         )
      );
      context.register(
         POINTED_DRIPSTONE,
         new SimpleRandomSelectorFeature(
            HolderSet.direct(
               PlacementUtils.inlinePlaced(
                  new SpeleothemFeature(
                     Blocks.DRIPSTONE_BLOCK.defaultBlockState(),
                     Blocks.POINTED_DRIPSTONE.defaultBlockState(),
                     blocks.getOrThrow(BlockTags.DRIPSTONE_REPLACEABLE),
                     0.2F,
                     0.7F,
                     0.5F,
                     0.5F
                  ),
                  EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_OR_WATER_PREDICATE, 12),
                  OffsetPlacement.vertical(ConstantInt.of(1))
               ),
               PlacementUtils.inlinePlaced(
                  new SpeleothemFeature(
                     Blocks.DRIPSTONE_BLOCK.defaultBlockState(),
                     Blocks.POINTED_DRIPSTONE.defaultBlockState(),
                     blocks.getOrThrow(BlockTags.DRIPSTONE_REPLACEABLE),
                     0.2F,
                     0.7F,
                     0.5F,
                     0.5F
                  ),
                  EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_OR_WATER_PREDICATE, 12),
                  OffsetPlacement.vertical(ConstantInt.of(-1))
               )
            )
         )
      );
      context.register(
         SULFUR_SPIKE_CLUSTER,
         new SpeleothemClusterFeature(
            Blocks.SULFUR.defaultBlockState(),
            Blocks.SULFUR_SPIKE.defaultBlockState(),
            blocks.getOrThrow(BlockTags.SULFUR_SPIKE_REPLACEABLE),
            12,
            UniformInt.of(1, 4),
            UniformInt.of(2, 8),
            1,
            3,
            UniformInt.of(2, 4),
            UniformFloat.of(0.3F, 0.7F),
            ConstantFloat.ZERO,
            0.1F,
            3,
            8
         )
      );
      context.register(
         SULFUR_SPIKE,
         new SimpleRandomSelectorFeature(
            HolderSet.direct(
               PlacementUtils.inlinePlaced(
                  new SpeleothemFeature(
                     Blocks.SULFUR.defaultBlockState(),
                     Blocks.SULFUR_SPIKE.defaultBlockState(),
                     blocks.getOrThrow(BlockTags.SULFUR_SPIKE_REPLACEABLE),
                     0.2F,
                     0.7F,
                     0.5F,
                     0.5F
                  ),
                  EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_OR_WATER_PREDICATE, 12),
                  OffsetPlacement.vertical(ConstantInt.of(1))
               ),
               PlacementUtils.inlinePlaced(
                  new SpeleothemFeature(
                     Blocks.SULFUR.defaultBlockState(),
                     Blocks.SULFUR_SPIKE.defaultBlockState(),
                     blocks.getOrThrow(BlockTags.SULFUR_SPIKE_REPLACEABLE),
                     0.2F,
                     0.7F,
                     0.5F,
                     0.5F
                  ),
                  EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_OR_WATER_PREDICATE, 12),
                  OffsetPlacement.vertical(ConstantInt.of(-1))
               )
            )
         )
      );
      context.register(UNDERWATER_MAGMA, new UnderwaterMagmaFeature(5, 1, 0.5F));
      MultifaceSpreadeableBlock glowLichenBlock = (MultifaceSpreadeableBlock)Blocks.GLOW_LICHEN;
      context.register(
         GLOW_LICHEN,
         new MultifaceGrowthFeature(
            glowLichenBlock,
            20,
            false,
            true,
            true,
            0.5F,
            HolderSet.direct(
               Block::builtInRegistryHolder,
               Blocks.STONE,
               Blocks.ANDESITE,
               Blocks.DIORITE,
               Blocks.GRANITE,
               Blocks.DRIPSTONE_BLOCK,
               Blocks.CALCITE,
               Blocks.TUFF,
               Blocks.DEEPSLATE,
               Blocks.SULFUR,
               Blocks.CINNABAR
            )
         )
      );
      context.register(
         ROOTED_AZALEA_TREE,
         new RootSystemFeature(
            PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(TreeFeatures.AZALEA_TREE)),
            3,
            0,
            0,
            3,
            blocks.getOrThrow(BlockTags.AZALEA_ROOT_REPLACEABLE),
            BlockStateProvider.simple(Blocks.ROOTED_DIRT),
            20,
            100,
            3,
            2,
            BlockStateProvider.simple(Blocks.HANGING_ROOTS),
            20,
            2,
            BlockPredicate.allOf(
               BlockPredicate.anyOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesTag(BlockTags.REPLACEABLE_BY_TREES)),
               BlockPredicate.matchesTag(Direction.DOWN, BlockTags.AZALEA_GROWS_ON)
            )
         )
      );
      context.register(
         ROOTED_SULFUR_SPRING,
         new RootSystemFeature(
            PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(MiscOverworldFeatures.SULFUR_SPRING)),
            5,
            8,
            2,
            3,
            blocks.getOrThrow(BlockTags.AZALEA_ROOT_REPLACEABLE),
            BlockStateProvider.simple(Blocks.SULFUR),
            20,
            184,
            1,
            1,
            BlockStateProvider.simple(Blocks.SULFUR),
            1,
            1,
            BlockPredicate.ONLY_IN_AIR_PREDICATE
         )
      );
      WeightedStateProvider caveVinesBodyProvider = new WeightedStateProvider(
         WeightedList.<BlockState>builder()
            .add(Blocks.CAVE_VINES_PLANT.defaultBlockState(), 4)
            .add(Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVines.BERRIES, true), 1)
      );
      RandomizedIntStateProvider caveVinesHeadProvider = new RandomizedIntStateProvider(
         new WeightedStateProvider(
            WeightedList.<BlockState>builder()
               .add(Blocks.CAVE_VINES.defaultBlockState(), 4)
               .add(Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, true), 1)
         ),
         CaveVinesBlock.AGE,
         UniformInt.of(23, 25)
      );
      context.register(
         CAVE_VINE,
         new BlockColumnFeature(
            List.of(
               BlockColumnFeature.layer(
                  new WeightedListInt(
                     WeightedList.<IntProvider>builder().add(UniformInt.of(0, 19), 2).add(UniformInt.of(0, 2), 3).add(UniformInt.of(0, 6), 10).build()
                  ),
                  caveVinesBodyProvider
               ),
               BlockColumnFeature.layer(ConstantInt.of(1), caveVinesHeadProvider)
            ),
            Direction.DOWN,
            BlockPredicate.ONLY_IN_AIR_PREDICATE,
            true
         )
      );
      context.register(
         CAVE_VINE_IN_MOSS,
         new BlockColumnFeature(
            List.of(
               BlockColumnFeature.layer(
                  new WeightedListInt(WeightedList.<IntProvider>builder().add(UniformInt.of(0, 3), 5).add(UniformInt.of(1, 7), 1).build()),
                  caveVinesBodyProvider
               ),
               BlockColumnFeature.layer(ConstantInt.of(1), caveVinesHeadProvider)
            ),
            Direction.DOWN,
            BlockPredicate.ONLY_IN_AIR_PREDICATE,
            true
         )
      );
      context.register(
         MOSS_VEGETATION,
         new SimpleBlockFeature(
            new WeightedStateProvider(
               WeightedList.<BlockState>builder()
                  .add(Blocks.FLOWERING_AZALEA.defaultBlockState(), 4)
                  .add(Blocks.AZALEA.defaultBlockState(), 7)
                  .add(Blocks.MOSS_CARPET.defaultBlockState(), 25)
                  .add(Blocks.SHORT_GRASS.defaultBlockState(), 50)
                  .add(Blocks.TALL_GRASS.defaultBlockState(), 10)
            )
         )
      );
      context.register(
         MOSS_PATCH,
         new VegetationPatchFeature(
            blocks.getOrThrow(BlockTags.MOSS_REPLACEABLE),
            BlockStateProvider.simple(Blocks.MOSS_BLOCK),
            PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(MOSS_VEGETATION)),
            CaveSurface.FLOOR,
            ConstantInt.of(1),
            0.0F,
            5,
            0.8F,
            UniformInt.of(4, 7),
            0.3F
         )
      );
      context.register(
         MOSS_PATCH_BONEMEAL,
         new VegetationPatchFeature(
            blocks.getOrThrow(BlockTags.MOSS_REPLACEABLE),
            BlockStateProvider.simple(Blocks.MOSS_BLOCK),
            PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(MOSS_VEGETATION)),
            CaveSurface.FLOOR,
            ConstantInt.of(1),
            0.0F,
            5,
            0.6F,
            UniformInt.of(1, 2),
            0.75F
         )
      );
      context.register(
         DRIPLEAF,
         new SimpleRandomSelectorFeature(
            HolderSet.direct(
               makeSmallDripleaf(), makeDripleaf(Direction.EAST), makeDripleaf(Direction.WEST), makeDripleaf(Direction.SOUTH), makeDripleaf(Direction.NORTH)
            )
         )
      );
      context.register(
         CLAY_WITH_DRIPLEAVES,
         new VegetationPatchFeature(
            blocks.getOrThrow(BlockTags.LUSH_GROUND_REPLACEABLE),
            BlockStateProvider.simple(Blocks.CLAY),
            PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(DRIPLEAF)),
            CaveSurface.FLOOR,
            ConstantInt.of(3),
            0.8F,
            2,
            0.05F,
            UniformInt.of(4, 7),
            0.7F
         )
      );
      context.register(
         CLAY_POOL_WITH_DRIPLEAVES,
         new WaterloggedVegetationPatchFeature(
            blocks.getOrThrow(BlockTags.LUSH_GROUND_REPLACEABLE),
            BlockStateProvider.simple(Blocks.CLAY),
            PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(DRIPLEAF)),
            CaveSurface.FLOOR,
            ConstantInt.of(3),
            0.8F,
            5,
            0.1F,
            UniformInt.of(4, 7),
            0.7F
         )
      );
      context.register(
         LUSH_CAVES_CLAY,
         new RandomBooleanSelectorFeature(
            PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(CLAY_WITH_DRIPLEAVES)),
            PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(CLAY_POOL_WITH_DRIPLEAVES))
         )
      );
      context.register(
         MOSS_PATCH_CEILING,
         new VegetationPatchFeature(
            blocks.getOrThrow(BlockTags.MOSS_REPLACEABLE),
            BlockStateProvider.simple(Blocks.MOSS_BLOCK),
            PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(CAVE_VINE_IN_MOSS)),
            CaveSurface.CEILING,
            UniformInt.of(1, 2),
            0.0F,
            5,
            0.08F,
            UniformInt.of(4, 7),
            0.3F
         )
      );
      context.register(SPORE_BLOSSOM, new SimpleBlockFeature(BlockStateProvider.simple(Blocks.SPORE_BLOSSOM)));
      context.register(
         AMETHYST_GEODE,
         new GeodeFeature(
            new GeodeBlockSettings(
               BlockStateProvider.simple(Blocks.AIR),
               BlockStateProvider.simple(Blocks.AMETHYST_BLOCK),
               BlockStateProvider.simple(Blocks.BUDDING_AMETHYST),
               BlockStateProvider.simple(Blocks.CALCITE),
               BlockStateProvider.simple(Blocks.SMOOTH_BASALT),
               List.of(
                  Blocks.SMALL_AMETHYST_BUD.defaultBlockState(),
                  Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState(),
                  Blocks.LARGE_AMETHYST_BUD.defaultBlockState(),
                  Blocks.AMETHYST_CLUSTER.defaultBlockState()
               ),
               blocks.getOrThrow(BlockTags.FEATURES_CANNOT_REPLACE),
               blocks.getOrThrow(BlockTags.GEODE_INVALID_BLOCKS)
            ),
            new GeodeLayerSettings(1.7, 2.2, 3.2, 4.2),
            new GeodeCrackSettings(0.95, 2.0, 2),
            0.35,
            0.083,
            true,
            UniformInt.of(4, 6),
            UniformInt.of(3, 4),
            UniformInt.of(1, 2),
            -16,
            16,
            0.05,
            1
         )
      );
      context.register(SCULK_PATCH_DEEP_DARK, new SculkPatchFeature(10, 32, 64, 0, 1, ConstantInt.of(0), 0.5F));
      context.register(SCULK_PATCH_ANCIENT_CITY, new SculkPatchFeature(10, 32, 64, 0, 1, UniformInt.of(1, 3), 0.5F));
      MultifaceSpreadeableBlock sculkVeinBlock = (MultifaceSpreadeableBlock)Blocks.SCULK_VEIN;
      context.register(
         SCULK_VEIN,
         new MultifaceGrowthFeature(
            sculkVeinBlock,
            20,
            true,
            true,
            true,
            1.0F,
            HolderSet.direct(
               Block::builtInRegistryHolder,
               Blocks.STONE,
               Blocks.ANDESITE,
               Blocks.DIORITE,
               Blocks.GRANITE,
               Blocks.DRIPSTONE_BLOCK,
               Blocks.CALCITE,
               Blocks.TUFF,
               Blocks.DEEPSLATE
            )
         )
      );
   }
}
