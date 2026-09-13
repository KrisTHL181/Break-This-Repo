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
import net.minecraft.world.level.levelgen.feature.configurations.CompositeFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomBooleanFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.DualNoiseProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseThresholdProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Fluids;

public class VegetationFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> BAMBOO_NO_PODZOL = FeatureUtils.createKey("bamboo_no_podzol");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BAMBOO_SOME_PODZOL = FeatureUtils.createKey("bamboo_some_podzol");
    public static final ResourceKey<ConfiguredFeature<?, ?>> VINES = FeatureUtils.createKey("vines");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BROWN_MUSHROOM = FeatureUtils.createKey("brown_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RED_MUSHROOM = FeatureUtils.createKey("red_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SUNFLOWER = FeatureUtils.createKey("sunflower");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PUMPKIN = FeatureUtils.createKey("pumpkin");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BERRY_BUSH = FeatureUtils.createKey("berry_bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TAIGA_GRASS = FeatureUtils.createKey("taiga_grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GRASS = FeatureUtils.createKey("grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GRASS_JUNGLE = FeatureUtils.createKey("grass_jungle");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEAD_BUSH = FeatureUtils.createKey("dead_bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DRY_GRASS = FeatureUtils.createKey("dry_grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MELON = FeatureUtils.createKey("melon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WATERLILY = FeatureUtils.createKey("waterlily");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TALL_GRASS = FeatureUtils.createKey("tall_grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LARGE_FERN = FeatureUtils.createKey("large_fern");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BUSH = FeatureUtils.createKey("bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LEAF_LITTER = FeatureUtils.createKey("leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FIREFLY_BUSH = FeatureUtils.createKey("firefly_bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CACTUS = FeatureUtils.createKey("cactus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SUGAR_CANE = FeatureUtils.createKey("sugar_cane");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_DEFAULT = FeatureUtils.createKey("flower_default");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_FLOWER_FOREST = FeatureUtils.createKey("flower_flower_forest");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_SWAMP = FeatureUtils.createKey("flower_swamp");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_PLAIN = FeatureUtils.createKey("flower_plain");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_MEADOW = FeatureUtils.createKey("flower_meadow");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_CHERRY = FeatureUtils.createKey("flower_cherry");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_PALE_GARDEN = FeatureUtils.createKey("flower_pale_garden");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDFLOWER = FeatureUtils.createKey("wildflower");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FOREST_FLOWERS = FeatureUtils.createKey("forest_flowers");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_FOREST_FLOWER = FeatureUtils.createKey("pale_forest_flower");
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

    public static void bootstrap(final BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        Holder<ConfiguredFeature<?, ?>> hugeBrownMushroom = configuredFeatures.getOrThrow(TreeFeatures.HUGE_BROWN_MUSHROOM);
        Holder<ConfiguredFeature<?, ?>> hugeRedMushroom = configuredFeatures.getOrThrow(TreeFeatures.HUGE_RED_MUSHROOM);
        Holder<ConfiguredFeature<?, ?>> fancyOakBees005 = configuredFeatures.getOrThrow(TreeFeatures.FANCY_OAK_BEES_005);
        Holder<ConfiguredFeature<?, ?>> oakBees005 = configuredFeatures.getOrThrow(TreeFeatures.OAK_BEES_005);
        Holder<ConfiguredFeature<?, ?>> grassJungle = configuredFeatures.getOrThrow(GRASS_JUNGLE);
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        Holder<PlacedFeature> paleOakChecked = placedFeatures.getOrThrow(TreePlacements.PALE_OAK_CHECKED);
        Holder<PlacedFeature> paleOakCreakingChecked = placedFeatures.getOrThrow(TreePlacements.PALE_OAK_CREAKING_CHECKED);
        Holder<PlacedFeature> fancyOakChecked = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_CHECKED);
        Holder<PlacedFeature> birchBees002 = placedFeatures.getOrThrow(TreePlacements.BIRCH_BEES_002);
        Holder<PlacedFeature> fancyOakBees002 = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_BEES_002);
        Holder<PlacedFeature> fancyOakBees = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_BEES);
        Holder<PlacedFeature> pineChecked = placedFeatures.getOrThrow(TreePlacements.PINE_CHECKED);
        Holder<PlacedFeature> spruceChecked = placedFeatures.getOrThrow(TreePlacements.SPRUCE_CHECKED);
        Holder<PlacedFeature> pineOnSnow = placedFeatures.getOrThrow(TreePlacements.PINE_ON_SNOW);
        Holder<PlacedFeature> acaciaChecked = placedFeatures.getOrThrow(TreePlacements.ACACIA_CHECKED);
        Holder<PlacedFeature> superBirchBees0002 = placedFeatures.getOrThrow(TreePlacements.SUPER_BIRCH_BEES_0002);
        Holder<PlacedFeature> birchBees0002Placed = placedFeatures.getOrThrow(TreePlacements.BIRCH_BEES_0002_PLACED);
        Holder<PlacedFeature> birchBees0002LeafLitter = placedFeatures.getOrThrow(TreePlacements.BIRCH_BEES_0002_LEAF_LITTER);
        Holder<PlacedFeature> fancyOakBees0002LeafLitter = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_BEES_0002_LEAF_LITTER);
        Holder<PlacedFeature> jungleBush = placedFeatures.getOrThrow(TreePlacements.JUNGLE_BUSH);
        Holder<PlacedFeature> megaSpruceChecked = placedFeatures.getOrThrow(TreePlacements.MEGA_SPRUCE_CHECKED);
        Holder<PlacedFeature> megaPineChecked = placedFeatures.getOrThrow(TreePlacements.MEGA_PINE_CHECKED);
        Holder<PlacedFeature> megaJungleTreeChecked = placedFeatures.getOrThrow(TreePlacements.MEGA_JUNGLE_TREE_CHECKED);
        Holder<PlacedFeature> tallMangroveChecked = placedFeatures.getOrThrow(TreePlacements.TALL_MANGROVE_CHECKED);
        Holder<PlacedFeature> oakChecked = placedFeatures.getOrThrow(TreePlacements.OAK_CHECKED);
        Holder<PlacedFeature> oakBees002 = placedFeatures.getOrThrow(TreePlacements.OAK_BEES_002);
        Holder<PlacedFeature> superBirchBees = placedFeatures.getOrThrow(TreePlacements.SUPER_BIRCH_BEES);
        Holder<PlacedFeature> spruceOnSnow = placedFeatures.getOrThrow(TreePlacements.SPRUCE_ON_SNOW);
        Holder<PlacedFeature> oakBees0002LeafLitter = placedFeatures.getOrThrow(TreePlacements.OAK_BEES_0002_LEAF_LITTER);
        Holder<PlacedFeature> jungleTreeChecked = placedFeatures.getOrThrow(TreePlacements.JUNGLE_TREE_CHECKED);
        Holder<PlacedFeature> mangroveChecked = placedFeatures.getOrThrow(TreePlacements.MANGROVE_CHECKED);
        Holder<PlacedFeature> oakLeafLitter = placedFeatures.getOrThrow(TreePlacements.OAK_LEAF_LITTER);
        Holder<PlacedFeature> darkOakLeafLitter = placedFeatures.getOrThrow(TreePlacements.DARK_OAK_LEAF_LITTER);
        Holder<PlacedFeature> birchLeafLitter = placedFeatures.getOrThrow(TreePlacements.BIRCH_LEAF_LITTER);
        Holder<PlacedFeature> fancyOakLeafLitter = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_LEAF_LITTER);
        Holder<PlacedFeature> fallenOak = placedFeatures.getOrThrow(TreePlacements.FALLEN_OAK_TREE);
        Holder<PlacedFeature> fallenBirch = placedFeatures.getOrThrow(TreePlacements.FALLEN_BIRCH_TREE);
        Holder<PlacedFeature> fallenSuperBirch = placedFeatures.getOrThrow(TreePlacements.FALLEN_SUPER_BIRCH_TREE);
        Holder<PlacedFeature> fallenJungle = placedFeatures.getOrThrow(TreePlacements.FALLEN_JUNGLE_TREE);
        Holder<PlacedFeature> fallenSpruce = placedFeatures.getOrThrow(TreePlacements.FALLEN_SPRUCE_TREE);
        FeatureUtils.register(context, BAMBOO_NO_PODZOL, Feature.BAMBOO, new ProbabilityFeatureConfiguration(0.0F));
        FeatureUtils.register(context, BAMBOO_SOME_PODZOL, Feature.BAMBOO, new ProbabilityFeatureConfiguration(0.2F));
        FeatureUtils.register(context, VINES, Feature.VINES);
        FeatureUtils.register(context, BROWN_MUSHROOM, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.BROWN_MUSHROOM)));
        FeatureUtils.register(context, RED_MUSHROOM, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.RED_MUSHROOM)));
        FeatureUtils.register(context, SUNFLOWER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.SUNFLOWER)));
        FeatureUtils.register(context, PUMPKIN, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.PUMPKIN)));
        FeatureUtils.register(
            context,
            BERRY_BUSH,
            Feature.SIMPLE_BLOCK,
            new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3)))
        );
        FeatureUtils.register(
            context,
            TAIGA_GRASS,
            Feature.SIMPLE_BLOCK,
            new SimpleBlockConfiguration(
                new WeightedStateProvider(
                    WeightedList.<BlockState>builder().add(Blocks.SHORT_GRASS.defaultBlockState(), 1).add(Blocks.FERN.defaultBlockState(), 4)
                )
            )
        );
        FeatureUtils.register(context, GRASS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.SHORT_GRASS)));
        FeatureUtils.register(context, LEAF_LITTER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(leafLitterPatchBuilder(1, 3))));
        FeatureUtils.register(
            context,
            GRASS_JUNGLE,
            Feature.SIMPLE_BLOCK,
            new SimpleBlockConfiguration(
                new WeightedStateProvider(
                    WeightedList.<BlockState>builder().add(Blocks.SHORT_GRASS.defaultBlockState(), 3).add(Blocks.FERN.defaultBlockState(), 1)
                )
            )
        );
        FeatureUtils.register(context, DEAD_BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.DEAD_BUSH)));
        FeatureUtils.register(
            context,
            DRY_GRASS,
            Feature.SIMPLE_BLOCK,
            new SimpleBlockConfiguration(
                new WeightedStateProvider(
                    WeightedList.<BlockState>builder().add(Blocks.SHORT_DRY_GRASS.defaultBlockState(), 1).add(Blocks.TALL_DRY_GRASS.defaultBlockState(), 1)
                )
            )
        );
        FeatureUtils.register(context, MELON, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MELON)));
        FeatureUtils.register(context, WATERLILY, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LILY_PAD)));
        FeatureUtils.register(context, TALL_GRASS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.TALL_GRASS)));
        FeatureUtils.register(context, LARGE_FERN, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LARGE_FERN)));
        FeatureUtils.register(context, BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.BUSH)));
        FeatureUtils.register(
            context,
            CACTUS,
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
            )
        );
        FeatureUtils.register(
            context,
            SUGAR_CANE,
            Feature.BLOCK_COLUMN,
            BlockColumnConfiguration.simple(BiasedToBottomInt.of(2, 4), BlockStateProvider.simple(Blocks.SUGAR_CANE))
        );
        FeatureUtils.register(context, FIREFLY_BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.FIREFLY_BUSH)));
        BlockStateProvider provider = new WeightedStateProvider(
            WeightedList.<BlockState>builder().add(Blocks.POPPY.defaultBlockState(), 2).add(Blocks.DANDELION.defaultBlockState(), 1)
        );
        FeatureUtils.register(context, FLOWER_DEFAULT, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(provider));
        FeatureUtils.register(
            context,
            FLOWER_FLOWER_FOREST,
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
        );
        FeatureUtils.register(context, FLOWER_SWAMP, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.BLUE_ORCHID)));
        FeatureUtils.register(
            context,
            FLOWER_PLAIN,
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
        );
        FeatureUtils.register(
            context,
            FLOWER_MEADOW,
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
        );
        FeatureUtils.register(
            context, FLOWER_CHERRY, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(flowerBedPatchBuilder(Blocks.PINK_PETALS)))
        );
        FeatureUtils.register(
            context, WILDFLOWER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(flowerBedPatchBuilder(Blocks.WILDFLOWERS)))
        );
        FeatureUtils.register(
            context, FLOWER_PALE_GARDEN, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.CLOSED_EYEBLOSSOM), true)
        );
        FeatureUtils.register(
            context,
            FOREST_FLOWERS,
            Feature.SIMPLE_RANDOM_SELECTOR,
            new CompositeFeatureConfiguration(
                HolderSet.direct(
                    PlacementUtils.inlinePlaced(
                        Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LILAC)),
                        CountPlacement.of(96),
                        RandomOffsetPlacement.ofTriangle(7, 3),
                        BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
                    ),
                    PlacementUtils.inlinePlaced(
                        Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.ROSE_BUSH)),
                        CountPlacement.of(96),
                        RandomOffsetPlacement.ofTriangle(7, 3),
                        BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
                    ),
                    PlacementUtils.inlinePlaced(
                        Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.PEONY)),
                        CountPlacement.of(96),
                        RandomOffsetPlacement.ofTriangle(7, 3),
                        BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
                    ),
                    PlacementUtils.inlinePlaced(
                        Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LILY_OF_THE_VALLEY)),
                        CountPlacement.of(96),
                        RandomOffsetPlacement.ofTriangle(7, 3),
                        BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)
                    )
                )
            )
        );
        FeatureUtils.register(
            context, PALE_FOREST_FLOWER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.CLOSED_EYEBLOSSOM), true)
        );
        FeatureUtils.register(
            context,
            DARK_FOREST_VEGETATION,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(PlacementUtils.inlinePlaced(hugeBrownMushroom), 0.025F),
                    new WeightedPlacedFeature(PlacementUtils.inlinePlaced(hugeRedMushroom), 0.05F),
                    new WeightedPlacedFeature(darkOakLeafLitter, 0.6666667F),
                    new WeightedPlacedFeature(fallenBirch, 0.0025F),
                    new WeightedPlacedFeature(birchLeafLitter, 0.2F),
                    new WeightedPlacedFeature(fallenOak, 0.0125F),
                    new WeightedPlacedFeature(fancyOakLeafLitter, 0.1F)
                ),
                oakLeafLitter
            )
        );
        FeatureUtils.register(
            context,
            PALE_GARDEN_VEGETATION,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(paleOakCreakingChecked, 0.1F), new WeightedPlacedFeature(paleOakChecked, 0.9F)), paleOakChecked
            )
        );
        FeatureUtils.register(
            context,
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
            context,
            PALE_MOSS_PATCH,
            Feature.VEGETATION_PATCH,
            new VegetationPatchConfiguration(
                blocks.getOrThrow(BlockTags.MOSS_REPLACEABLE),
                BlockStateProvider.simple(Blocks.PALE_MOSS_BLOCK),
                PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(PALE_MOSS_VEGETATION)),
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
            context,
            PALE_MOSS_PATCH_BONEMEAL,
            Feature.VEGETATION_PATCH,
            new VegetationPatchConfiguration(
                blocks.getOrThrow(BlockTags.MOSS_REPLACEABLE),
                BlockStateProvider.simple(Blocks.PALE_MOSS_BLOCK),
                PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(PALE_MOSS_VEGETATION)),
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
            context,
            TREES_FLOWER_FOREST,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(fallenBirch, 0.0025F),
                    new WeightedPlacedFeature(birchBees002, 0.2F),
                    new WeightedPlacedFeature(fancyOakBees002, 0.1F)
                ),
                oakBees002
            )
        );
        FeatureUtils.register(
            context,
            MEADOW_TREES,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fancyOakBees, 0.5F)), superBirchBees)
        );
        FeatureUtils.register(
            context,
            TREES_TAIGA,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(pineChecked, 0.33333334F), new WeightedPlacedFeature(fallenSpruce, 0.0125F)), spruceChecked
            )
        );
        FeatureUtils.register(
            context,
            TREES_BADLANDS,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenOak, 0.0125F)), oakLeafLitter)
        );
        FeatureUtils.register(
            context,
            TREES_GROVE,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(pineOnSnow, 0.33333334F)), spruceOnSnow)
        );
        FeatureUtils.register(
            context,
            TREES_SAVANNA,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(acaciaChecked, 0.8F), new WeightedPlacedFeature(fallenOak, 0.0125F)), oakChecked)
        );
        FeatureUtils.register(
            context,
            TREES_SNOWY,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenSpruce, 0.0125F)), spruceChecked)
        );
        FeatureUtils.register(
            context,
            TREES_BIRCH,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenBirch, 0.0125F)), birchBees0002Placed)
        );
        FeatureUtils.register(
            context,
            BIRCH_TALL,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(fallenSuperBirch, 0.00625F),
                    new WeightedPlacedFeature(superBirchBees0002, 0.5F),
                    new WeightedPlacedFeature(fallenBirch, 0.0125F)
                ),
                birchBees0002Placed
            )
        );
        FeatureUtils.register(
            context,
            TREES_WINDSWEPT_HILLS,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(fallenSpruce, 0.008325F),
                    new WeightedPlacedFeature(spruceChecked, 0.666F),
                    new WeightedPlacedFeature(fancyOakChecked, 0.1F),
                    new WeightedPlacedFeature(fallenOak, 0.0125F)
                ),
                oakChecked
            )
        );
        FeatureUtils.register(
            context,
            TREES_WATER,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fancyOakChecked, 0.1F)), oakChecked)
        );
        FeatureUtils.register(
            context,
            TREES_BIRCH_AND_OAK_LEAF_LITTER,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(fallenBirch, 0.0025F),
                    new WeightedPlacedFeature(birchBees0002LeafLitter, 0.2F),
                    new WeightedPlacedFeature(fancyOakBees0002LeafLitter, 0.1F),
                    new WeightedPlacedFeature(fallenOak, 0.0125F)
                ),
                oakBees0002LeafLitter
            )
        );
        FeatureUtils.register(
            context,
            TREES_PLAINS,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(fancyOakBees005), 0.33333334F), new WeightedPlacedFeature(fallenOak, 0.0125F)),
                PlacementUtils.inlinePlaced(oakBees005)
            )
        );
        FeatureUtils.register(
            context,
            TREES_SPARSE_JUNGLE,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(fancyOakChecked, 0.1F),
                    new WeightedPlacedFeature(jungleBush, 0.5F),
                    new WeightedPlacedFeature(fallenJungle, 0.0125F)
                ),
                jungleTreeChecked
            )
        );
        FeatureUtils.register(
            context,
            TREES_OLD_GROWTH_SPRUCE_TAIGA,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(megaSpruceChecked, 0.33333334F),
                    new WeightedPlacedFeature(pineChecked, 0.33333334F),
                    new WeightedPlacedFeature(fallenSpruce, 0.0125F)
                ),
                spruceChecked
            )
        );
        FeatureUtils.register(
            context,
            TREES_OLD_GROWTH_PINE_TAIGA,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(megaSpruceChecked, 0.025641026F),
                    new WeightedPlacedFeature(megaPineChecked, 0.30769232F),
                    new WeightedPlacedFeature(pineChecked, 0.33333334F),
                    new WeightedPlacedFeature(fallenSpruce, 0.0125F)
                ),
                spruceChecked
            )
        );
        FeatureUtils.register(
            context,
            TREES_JUNGLE,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(fancyOakChecked, 0.1F),
                    new WeightedPlacedFeature(jungleBush, 0.5F),
                    new WeightedPlacedFeature(megaJungleTreeChecked, 0.33333334F),
                    new WeightedPlacedFeature(fallenJungle, 0.0125F)
                ),
                jungleTreeChecked
            )
        );
        FeatureUtils.register(
            context,
            BAMBOO_VEGETATION,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(
                List.of(
                    new WeightedPlacedFeature(fancyOakChecked, 0.05F),
                    new WeightedPlacedFeature(jungleBush, 0.15F),
                    new WeightedPlacedFeature(megaJungleTreeChecked, 0.7F)
                ),
                PlacementUtils.inlinePlaced(
                    grassJungle,
                    CountPlacement.of(32),
                    RandomOffsetPlacement.ofTriangle(7, 3),
                    BlockPredicateFilter.forPredicate(
                        BlockPredicate.allOf(
                            BlockPredicate.ONLY_IN_AIR_PREDICATE,
                            BlockPredicate.not(BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(), Blocks.PODZOL))
                        )
                    )
                )
            )
        );
        FeatureUtils.register(
            context,
            MUSHROOM_ISLAND_VEGETATION,
            Feature.RANDOM_BOOLEAN_SELECTOR,
            new RandomBooleanFeatureConfiguration(PlacementUtils.inlinePlaced(hugeRedMushroom), PlacementUtils.inlinePlaced(hugeBrownMushroom))
        );
        FeatureUtils.register(
            context,
            MANGROVE_VEGETATION,
            Feature.RANDOM_SELECTOR,
            new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(tallMangroveChecked, 0.85F)), mangroveChecked)
        );
    }

    private static WeightedList.Builder<BlockState> flowerBedPatchBuilder(final Block flowerBedBlock) {
        return segmentedBlockPatchBuilder(flowerBedBlock, 1, 4, FlowerBedBlock.AMOUNT, FlowerBedBlock.FACING);
    }

    public static WeightedList.Builder<BlockState> leafLitterPatchBuilder(final int minState, final int maxState) {
        return segmentedBlockPatchBuilder(Blocks.LEAF_LITTER, minState, maxState, LeafLitterBlock.AMOUNT, LeafLitterBlock.FACING);
    }

    private static WeightedList.Builder<BlockState> segmentedBlockPatchBuilder(
        final Block block, final int minState, final int maxState, final IntegerProperty amountProperty, final EnumProperty<Direction> directionProperty
    ) {
        WeightedList.Builder<BlockState> segmentedBlockBuild = WeightedList.builder();

        for (int amount = minState; amount <= maxState; amount++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                segmentedBlockBuild.add(block.defaultBlockState().setValue(amountProperty, amount).setValue(directionProperty, direction), 1);
            }
        }

        return segmentedBlockBuild;
    }

    public static BlockPredicateFilter nearWaterPredicate(final Block block) {
        return BlockPredicateFilter.forPredicate(
            BlockPredicate.allOf(
                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.wouldSurvive(block.defaultBlockState(), BlockPos.ZERO),
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