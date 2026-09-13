package net.minecraft.data.worldgen.features;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PotentSulfurBlock;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.BlockBlobFeature;
import net.minecraft.world.level.levelgen.feature.BlueIceFeature;
import net.minecraft.world.level.levelgen.feature.BonusChestFeature;
import net.minecraft.world.level.levelgen.feature.DesertWellFeature;
import net.minecraft.world.level.levelgen.feature.DiskFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.IcebergFeature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.SequenceFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.SnowAndFreezeFeature;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.TemplateFeature;
import net.minecraft.world.level.levelgen.feature.VoidStartPlatformFeature;
import net.minecraft.world.level.levelgen.feature.WeightedRandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluids;

public class MiscOverworldFeatures {
   public static final ResourceKey<Feature> ICE_SPIKE = FeatureUtils.createKey("ice_spike");
   public static final ResourceKey<Feature> ICE_PATCH = FeatureUtils.createKey("ice_patch");
   public static final ResourceKey<Feature> FOREST_ROCK = FeatureUtils.createKey("forest_rock");
   public static final ResourceKey<Feature> ICEBERG_PACKED = FeatureUtils.createKey("iceberg_packed");
   public static final ResourceKey<Feature> ICEBERG_BLUE = FeatureUtils.createKey("iceberg_blue");
   public static final ResourceKey<Feature> BLUE_ICE = FeatureUtils.createKey("blue_ice");
   public static final ResourceKey<Feature> LAKE_LAVA = FeatureUtils.createKey("lake_lava");
   public static final ResourceKey<Feature> SULFUR_POOL = FeatureUtils.createKey("sulfur_pool");
   public static final ResourceKey<Feature> SULFUR_SPRING = FeatureUtils.createKey("sulfur_spring");
   public static final ResourceKey<Feature> DISK_CLAY = FeatureUtils.createKey("disk_clay");
   public static final ResourceKey<Feature> DISK_GRAVEL = FeatureUtils.createKey("disk_gravel");
   public static final ResourceKey<Feature> DISK_SAND = FeatureUtils.createKey("disk_sand");
   public static final ResourceKey<Feature> FREEZE_TOP_LAYER = FeatureUtils.createKey("freeze_top_layer");
   public static final ResourceKey<Feature> DISK_GRASS = FeatureUtils.createKey("disk_grass");
   public static final ResourceKey<Feature> BONUS_CHEST = FeatureUtils.createKey("bonus_chest");
   public static final ResourceKey<Feature> VOID_START_PLATFORM = FeatureUtils.createKey("void_start_platform");
   public static final ResourceKey<Feature> DESERT_WELL = FeatureUtils.createKey("desert_well");
   public static final ResourceKey<Feature> SPRING_LAVA_OVERWORLD = FeatureUtils.createKey("spring_lava_overworld");
   public static final ResourceKey<Feature> SPRING_LAVA_FROZEN = FeatureUtils.createKey("spring_lava_frozen");
   public static final ResourceKey<Feature> SPRING_WATER = FeatureUtils.createKey("spring_water");

   public static void bootstrap(final BootstrapContext<Feature> context) {
      context.register(
         ICE_SPIKE,
         new SpikeFeature(
            Blocks.PACKED_ICE.defaultBlockState(), BlockPredicate.matchesBlocks(Blocks.SNOW_BLOCK), BlockPredicate.matchesTag(BlockTags.ICE_SPIKE_REPLACEABLE)
         )
      );
      context.register(
         ICE_PATCH,
         new DiskFeature(
            BlockStateProvider.simple(Blocks.PACKED_ICE),
            BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MYCELIUM, Blocks.SNOW_BLOCK, Blocks.ICE),
            UniformInt.of(2, 3),
            1
         )
      );
      context.register(
         FOREST_ROCK, new BlockBlobFeature(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), BlockPredicate.matchesTag(BlockTags.FOREST_ROCK_CAN_PLACE_ON))
      );
      context.register(ICEBERG_PACKED, new IcebergFeature(Blocks.PACKED_ICE.defaultBlockState()));
      context.register(ICEBERG_BLUE, new IcebergFeature(Blocks.BLUE_ICE.defaultBlockState()));
      context.register(BLUE_ICE, new BlueIceFeature());
      context.register(
         LAKE_LAVA,
         new LakeFeature(
            BlockStateProvider.simple(Blocks.LAVA.defaultBlockState()),
            BlockStateProvider.simple(Blocks.STONE.defaultBlockState()),
            BlockPredicate.alwaysTrue(),
            BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.FEATURES_CANNOT_REPLACE)),
            BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE))
         )
      );
      context.register(
         SULFUR_POOL,
         new SequenceFeature(
            HolderSet.direct(
               PlacementUtils.inlinePlaced(
                  new LakeFeature(
                     BlockStateProvider.simple(Blocks.WATER.defaultBlockState()),
                     BlockStateProvider.simple(Blocks.SULFUR.defaultBlockState()),
                     BlockPredicate.not(BlockPredicate.matchesBlocks(Blocks.SULFUR_SPIKE)),
                     BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.FEATURES_CANNOT_REPLACE)),
                     BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE))
                  )
               ),
               PlacementUtils.inlinePlaced(
                  new SimpleBlockFeature(
                     BlockStateProvider.simple(Blocks.POTENT_SULFUR.defaultBlockState().setValue(PotentSulfurBlock.STATE, PotentSulfurState.WET))
                  ),
                  EnvironmentScanPlacement.scanningFor(
                     Direction.DOWN, BlockPredicate.allOf(BlockPredicate.solid(), BlockPredicate.matchesFluids(Direction.UP, Fluids.WATER)), 4
                  )
               )
            )
         )
      );
      BiFunction<Integer, Integer, Holder<PlacedFeature>> tuffCover = (count, spread) -> PlacementUtils.inlinePlaced(
         new SimpleBlockFeature(BlockStateProvider.simple(Blocks.TUFF)),
         CountPlacement.of(count),
         OffsetPlacement.ofTriangle(spread, 3),
         EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), 4),
         BlockPredicateFilter.forPredicate(BlockPredicate.solid())
      );
      Function<WeightedList<TemplateFeature.TemplateEntry>, Holder<PlacedFeature>> sulfurSprings = entries -> PlacementUtils.inlinePlaced(
         new TemplateFeature(entries), OffsetPlacement.vertical(ConstantInt.of(-7))
      );
      context.register(
         SULFUR_SPRING,
         new WeightedRandomSelectorFeature(
            WeightedList.of(
               new Weighted<>(
                  PlacementUtils.inlinePlaced(
                     new SequenceFeature(
                        HolderSet.direct(
                           tuffCover.apply(64, 7),
                           sulfurSprings.apply(
                              WeightedList.of(
                                 TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_small_1")),
                                 TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_small_2")),
                                 TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_small_3")),
                                 TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_small_4"))
                              )
                           )
                        )
                     )
                  ),
                  200
               ),
               new Weighted<>(
                  PlacementUtils.inlinePlaced(
                     new SequenceFeature(
                        HolderSet.direct(
                           tuffCover.apply(80, 8),
                           sulfurSprings.apply(
                              WeightedList.of(
                                 TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_medium_1")),
                                 TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_medium_2")),
                                 TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_medium_3"))
                              )
                           )
                        )
                     )
                  ),
                  90
               ),
               new Weighted<>(
                  PlacementUtils.inlinePlaced(
                     new SequenceFeature(
                        HolderSet.direct(
                           tuffCover.apply(96, 9),
                           sulfurSprings.apply(
                              WeightedList.of(
                                 TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_large_1")),
                                 TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_large_2"))
                              )
                           )
                        )
                     )
                  ),
                  20
               ),
               new Weighted<>(
                  PlacementUtils.inlinePlaced(
                     new SequenceFeature(
                        HolderSet.direct(
                           tuffCover.apply(128, 10),
                           sulfurSprings.apply(
                              WeightedList.of(TemplateFeature.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_extra_large_1")))
                           )
                        )
                     )
                  ),
                  5
               )
            )
         )
      );
      context.register(
         DISK_CLAY, new DiskFeature(BlockStateProvider.simple(Blocks.CLAY), BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.CLAY), UniformInt.of(2, 3), 1)
      );
      context.register(
         DISK_GRAVEL,
         new DiskFeature(BlockStateProvider.simple(Blocks.GRAVEL), BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.GRASS_BLOCK), UniformInt.of(2, 5), 2)
      );
      context.register(
         DISK_SAND,
         new DiskFeature(
            new RuleBasedStateProvider(
               BlockStateProvider.simple(Blocks.SAND),
               List.of(new RuleBasedStateProvider.Rule(BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.AIR), BlockStateProvider.simple(Blocks.SANDSTONE)))
            ),
            BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.GRASS_BLOCK),
            UniformInt.of(2, 6),
            2
         )
      );
      context.register(FREEZE_TOP_LAYER, new SnowAndFreezeFeature());
      context.register(
         DISK_GRASS,
         new DiskFeature(
            new RuleBasedStateProvider(
               BlockStateProvider.simple(Blocks.DIRT),
               List.of(
                  new RuleBasedStateProvider.Rule(
                     BlockPredicate.not(BlockPredicate.anyOf(BlockPredicate.solid(Direction.UP), BlockPredicate.matchesFluids(Direction.UP, Fluids.WATER))),
                     BlockStateProvider.simple(Blocks.GRASS_BLOCK)
                  )
               )
            ),
            BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.MUD),
            UniformInt.of(2, 6),
            2
         )
      );
      context.register(BONUS_CHEST, new BonusChestFeature());
      context.register(VOID_START_PLATFORM, new VoidStartPlatformFeature());
      context.register(DESERT_WELL, new DesertWellFeature());
      context.register(
         SPRING_LAVA_OVERWORLD,
         new SpringFeature(
            Fluids.LAVA.defaultFluidState(),
            true,
            4,
            1,
            HolderSet.direct(
               Block::builtInRegistryHolder,
               Blocks.STONE,
               Blocks.GRANITE,
               Blocks.DIORITE,
               Blocks.ANDESITE,
               Blocks.DEEPSLATE,
               Blocks.TUFF,
               Blocks.CALCITE,
               Blocks.DIRT
            )
         )
      );
      context.register(
         SPRING_LAVA_FROZEN,
         new SpringFeature(
            Fluids.LAVA.defaultFluidState(),
            true,
            4,
            1,
            HolderSet.direct(Block::builtInRegistryHolder, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.PACKED_ICE)
         )
      );
      context.register(
         SPRING_WATER,
         new SpringFeature(
            Fluids.WATER.defaultFluidState(),
            true,
            4,
            1,
            HolderSet.direct(
               Block::builtInRegistryHolder,
               Blocks.STONE,
               Blocks.GRANITE,
               Blocks.DIORITE,
               Blocks.ANDESITE,
               Blocks.DEEPSLATE,
               Blocks.TUFF,
               Blocks.CALCITE,
               Blocks.DIRT,
               Blocks.SNOW_BLOCK,
               Blocks.POWDER_SNOW,
               Blocks.PACKED_ICE
            )
         )
      );
   }
}
