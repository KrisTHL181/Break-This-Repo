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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockBlobConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.CompositeFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TemplateFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WeightedRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.material.Fluids;

public class MiscOverworldFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> ICE_SPIKE = FeatureUtils.createKey("ice_spike");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ICE_PATCH = FeatureUtils.createKey("ice_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FOREST_ROCK = FeatureUtils.createKey("forest_rock");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ICEBERG_PACKED = FeatureUtils.createKey("iceberg_packed");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ICEBERG_BLUE = FeatureUtils.createKey("iceberg_blue");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BLUE_ICE = FeatureUtils.createKey("blue_ice");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LAKE_LAVA = FeatureUtils.createKey("lake_lava");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SULFUR_POOL = FeatureUtils.createKey("sulfur_pool");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SULFUR_SPRING = FeatureUtils.createKey("sulfur_spring");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_CLAY = FeatureUtils.createKey("disk_clay");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_GRAVEL = FeatureUtils.createKey("disk_gravel");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_SAND = FeatureUtils.createKey("disk_sand");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FREEZE_TOP_LAYER = FeatureUtils.createKey("freeze_top_layer");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_GRASS = FeatureUtils.createKey("disk_grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BONUS_CHEST = FeatureUtils.createKey("bonus_chest");
    public static final ResourceKey<ConfiguredFeature<?, ?>> VOID_START_PLATFORM = FeatureUtils.createKey("void_start_platform");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DESERT_WELL = FeatureUtils.createKey("desert_well");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPRING_LAVA_OVERWORLD = FeatureUtils.createKey("spring_lava_overworld");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPRING_LAVA_FROZEN = FeatureUtils.createKey("spring_lava_frozen");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPRING_WATER = FeatureUtils.createKey("spring_water");

    public static void bootstrap(final BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(
            context,
            ICE_SPIKE,
            Feature.SPIKE,
            new SpikeConfiguration(
                Blocks.PACKED_ICE.defaultBlockState(),
                BlockPredicate.matchesBlocks(Blocks.SNOW_BLOCK),
                BlockPredicate.matchesTag(BlockTags.ICE_SPIKE_REPLACEABLE)
            )
        );
        FeatureUtils.register(
            context,
            ICE_PATCH,
            Feature.DISK,
            new DiskConfiguration(
                BlockStateProvider.simple(Blocks.PACKED_ICE),
                BlockPredicate.matchesBlocks(
                    List.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MYCELIUM, Blocks.SNOW_BLOCK, Blocks.ICE)
                ),
                UniformInt.of(2, 3),
                1
            )
        );
        FeatureUtils.register(
            context,
            FOREST_ROCK,
            Feature.BLOCK_BLOB,
            new BlockBlobConfiguration(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), BlockPredicate.matchesTag(BlockTags.FOREST_ROCK_CAN_PLACE_ON))
        );
        FeatureUtils.register(context, ICEBERG_PACKED, Feature.ICEBERG, new BlockStateConfiguration(Blocks.PACKED_ICE.defaultBlockState()));
        FeatureUtils.register(context, ICEBERG_BLUE, Feature.ICEBERG, new BlockStateConfiguration(Blocks.BLUE_ICE.defaultBlockState()));
        FeatureUtils.register(context, BLUE_ICE, Feature.BLUE_ICE);
        FeatureUtils.register(
            context,
            LAKE_LAVA,
            Feature.LAKE,
            new LakeFeature.Configuration(
                BlockStateProvider.simple(Blocks.LAVA.defaultBlockState()),
                BlockStateProvider.simple(Blocks.STONE.defaultBlockState()),
                BlockPredicate.alwaysTrue(),
                BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.FEATURES_CANNOT_REPLACE)),
                BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE))
            )
        );
        FeatureUtils.register(
            context,
            SULFUR_POOL,
            Feature.SEQUENCE,
            new CompositeFeatureConfiguration(
                HolderSet.direct(
                    PlacementUtils.inlinePlaced(
                        Feature.LAKE,
                        new LakeFeature.Configuration(
                            BlockStateProvider.simple(Blocks.WATER.defaultBlockState()),
                            BlockStateProvider.simple(Blocks.SULFUR.defaultBlockState()),
                            BlockPredicate.not(BlockPredicate.matchesBlocks(Blocks.SULFUR_SPIKE)),
                            BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.FEATURES_CANNOT_REPLACE)),
                            BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE))
                        )
                    ),
                    PlacementUtils.inlinePlaced(
                        Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(
                            BlockStateProvider.simple(Blocks.POTENT_SULFUR.defaultBlockState().setValue(PotentSulfurBlock.STATE, PotentSulfurState.WET))
                        ),
                        EnvironmentScanPlacement.scanningFor(
                            Direction.DOWN,
                            BlockPredicate.allOf(BlockPredicate.solid(), BlockPredicate.matchesFluids(Direction.UP.getUnitVec3i(), Fluids.WATER)),
                            4
                        )
                    )
                )
            )
        );
        BiFunction<Integer, Integer, Holder<PlacedFeature>> tuffCover = (count, spread) -> PlacementUtils.inlinePlaced(
            Feature.SIMPLE_BLOCK,
            new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.TUFF)),
            CountPlacement.of(count),
            RandomOffsetPlacement.ofTriangle(spread, 3),
            EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), 4),
            BlockPredicateFilter.forPredicate(BlockPredicate.solid())
        );
        Function<WeightedList<TemplateFeatureConfiguration.TemplateEntry>, Holder<PlacedFeature>> sulfurSprings = entries -> PlacementUtils.inlinePlaced(
            Feature.TEMPLATE, new TemplateFeatureConfiguration(entries), RandomOffsetPlacement.vertical(ConstantInt.of(-7))
        );
        FeatureUtils.register(
            context,
            SULFUR_SPRING,
            Feature.WEIGHTED_RANDOM_SELECTOR,
            new WeightedRandomFeatureConfiguration(
                WeightedList.of(
                    new Weighted<>(
                        PlacementUtils.inlinePlaced(
                            Feature.SEQUENCE,
                            new CompositeFeatureConfiguration(
                                HolderSet.direct(
                                    tuffCover.apply(64, 7),
                                    sulfurSprings.apply(
                                        WeightedList.of(
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_small_1")),
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_small_2")),
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_small_3")),
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_small_4"))
                                        )
                                    )
                                )
                            )
                        ),
                        200
                    ),
                    new Weighted<>(
                        PlacementUtils.inlinePlaced(
                            Feature.SEQUENCE,
                            new CompositeFeatureConfiguration(
                                HolderSet.direct(
                                    tuffCover.apply(80, 8),
                                    sulfurSprings.apply(
                                        WeightedList.of(
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_medium_1")),
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_medium_2")),
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_medium_3"))
                                        )
                                    )
                                )
                            )
                        ),
                        90
                    ),
                    new Weighted<>(
                        PlacementUtils.inlinePlaced(
                            Feature.SEQUENCE,
                            new CompositeFeatureConfiguration(
                                HolderSet.direct(
                                    tuffCover.apply(96, 9),
                                    sulfurSprings.apply(
                                        WeightedList.of(
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_large_1")),
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_large_2"))
                                        )
                                    )
                                )
                            )
                        ),
                        20
                    ),
                    new Weighted<>(
                        PlacementUtils.inlinePlaced(
                            Feature.SEQUENCE,
                            new CompositeFeatureConfiguration(
                                HolderSet.direct(
                                    tuffCover.apply(128, 10),
                                    sulfurSprings.apply(
                                        WeightedList.of(
                                            TemplateFeatureConfiguration.TemplateEntry.of(Identifier.withDefaultNamespace("spring/sulfur_spring_extra_large_1"))
                                        )
                                    )
                                )
                            )
                        ),
                        5
                    )
                )
            )
        );
        FeatureUtils.register(
            context,
            DISK_CLAY,
            Feature.DISK,
            new DiskConfiguration(
                BlockStateProvider.simple(Blocks.CLAY), BlockPredicate.matchesBlocks(List.of(Blocks.DIRT, Blocks.CLAY)), UniformInt.of(2, 3), 1
            )
        );
        FeatureUtils.register(
            context,
            DISK_GRAVEL,
            Feature.DISK,
            new DiskConfiguration(
                BlockStateProvider.simple(Blocks.GRAVEL), BlockPredicate.matchesBlocks(List.of(Blocks.DIRT, Blocks.GRASS_BLOCK)), UniformInt.of(2, 5), 2
            )
        );
        FeatureUtils.register(
            context,
            DISK_SAND,
            Feature.DISK,
            new DiskConfiguration(
                new RuleBasedStateProvider(
                    BlockStateProvider.simple(Blocks.SAND),
                    List.of(
                        new RuleBasedStateProvider.Rule(
                            BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(), Blocks.AIR), BlockStateProvider.simple(Blocks.SANDSTONE)
                        )
                    )
                ),
                BlockPredicate.matchesBlocks(List.of(Blocks.DIRT, Blocks.GRASS_BLOCK)),
                UniformInt.of(2, 6),
                2
            )
        );
        FeatureUtils.register(context, FREEZE_TOP_LAYER, Feature.FREEZE_TOP_LAYER);
        FeatureUtils.register(
            context,
            DISK_GRASS,
            Feature.DISK,
            new DiskConfiguration(
                new RuleBasedStateProvider(
                    BlockStateProvider.simple(Blocks.DIRT),
                    List.of(
                        new RuleBasedStateProvider.Rule(
                            BlockPredicate.not(
                                BlockPredicate.anyOf(
                                    BlockPredicate.solid(Direction.UP.getUnitVec3i()), BlockPredicate.matchesFluids(Direction.UP.getUnitVec3i(), Fluids.WATER)
                                )
                            ),
                            BlockStateProvider.simple(Blocks.GRASS_BLOCK)
                        )
                    )
                ),
                BlockPredicate.matchesBlocks(List.of(Blocks.DIRT, Blocks.MUD)),
                UniformInt.of(2, 6),
                2
            )
        );
        FeatureUtils.register(context, BONUS_CHEST, Feature.BONUS_CHEST);
        FeatureUtils.register(context, VOID_START_PLATFORM, Feature.VOID_START_PLATFORM);
        FeatureUtils.register(context, DESERT_WELL, Feature.DESERT_WELL);
        FeatureUtils.register(
            context,
            SPRING_LAVA_OVERWORLD,
            Feature.SPRING,
            new SpringConfiguration(
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
        FeatureUtils.register(
            context,
            SPRING_LAVA_FROZEN,
            Feature.SPRING,
            new SpringConfiguration(
                Fluids.LAVA.defaultFluidState(),
                true,
                4,
                1,
                HolderSet.direct(Block::builtInRegistryHolder, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.PACKED_ICE)
            )
        );
        FeatureUtils.register(
            context,
            SPRING_WATER,
            Feature.SPRING,
            new SpringConfiguration(
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