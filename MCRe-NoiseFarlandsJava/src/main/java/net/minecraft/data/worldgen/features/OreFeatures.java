package net.minecraft.data.worldgen.features;

import java.util.List;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

public class OreFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_MAGMA = FeatureUtils.createKey("ore_magma");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SOUL_SAND = FeatureUtils.createKey("ore_soul_sand");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_NETHER_GOLD = FeatureUtils.createKey("ore_nether_gold");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_QUARTZ = FeatureUtils.createKey("ore_quartz");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GRAVEL_NETHER = FeatureUtils.createKey("ore_gravel_nether");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_BLACKSTONE = FeatureUtils.createKey("ore_blackstone");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIRT = FeatureUtils.createKey("ore_dirt");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GRAVEL = FeatureUtils.createKey("ore_gravel");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GRANITE = FeatureUtils.createKey("ore_granite");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIORITE = FeatureUtils.createKey("ore_diorite");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ANDESITE = FeatureUtils.createKey("ore_andesite");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TUFF = FeatureUtils.createKey("ore_tuff");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COAL = FeatureUtils.createKey("ore_coal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COAL_BURIED = FeatureUtils.createKey("ore_coal_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_IRON = FeatureUtils.createKey("ore_iron");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_IRON_SMALL = FeatureUtils.createKey("ore_iron_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GOLD = FeatureUtils.createKey("ore_gold");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GOLD_BURIED = FeatureUtils.createKey("ore_gold_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_REDSTONE = FeatureUtils.createKey("ore_redstone");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIAMOND_SMALL = FeatureUtils.createKey("ore_diamond_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIAMOND_MEDIUM = FeatureUtils.createKey("ore_diamond_medium");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIAMOND_LARGE = FeatureUtils.createKey("ore_diamond_large");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIAMOND_BURIED = FeatureUtils.createKey("ore_diamond_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_LAPIS = FeatureUtils.createKey("ore_lapis");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_LAPIS_BURIED = FeatureUtils.createKey("ore_lapis_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_INFESTED = FeatureUtils.createKey("ore_infested");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_EMERALD = FeatureUtils.createKey("ore_emerald");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ANCIENT_DEBRIS_LARGE = FeatureUtils.createKey("ore_ancient_debris_large");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ANCIENT_DEBRIS_SMALL = FeatureUtils.createKey("ore_ancient_debris_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPPER_SMALL = FeatureUtils.createKey("ore_copper_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPER_LARGE = FeatureUtils.createKey("ore_copper_large");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_CLAY = FeatureUtils.createKey("ore_clay");

    public static void bootstrap(final BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest naturalStone = new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD);
        RuleTest stoneOreReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateOreReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrack = new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest netherOreReplaceables = new TagMatchTest(BlockTags.BASE_STONE_NETHER);
        List<OreConfiguration.TargetBlockState> oreIronTargetList = List.of(
            OreConfiguration.target(stoneOreReplaceables, Blocks.IRON_ORE.defaultBlockState()),
            OreConfiguration.target(deepslateOreReplaceables, Blocks.DEEPSLATE_IRON_ORE.defaultBlockState())
        );
        List<OreConfiguration.TargetBlockState> oreGoldTargetList = List.of(
            OreConfiguration.target(stoneOreReplaceables, Blocks.GOLD_ORE.defaultBlockState()),
            OreConfiguration.target(deepslateOreReplaceables, Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState())
        );
        List<OreConfiguration.TargetBlockState> oreDiamondTargetList = List.of(
            OreConfiguration.target(stoneOreReplaceables, Blocks.DIAMOND_ORE.defaultBlockState()),
            OreConfiguration.target(deepslateOreReplaceables, Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState())
        );
        List<OreConfiguration.TargetBlockState> oreLapisTargetList = List.of(
            OreConfiguration.target(stoneOreReplaceables, Blocks.LAPIS_ORE.defaultBlockState()),
            OreConfiguration.target(deepslateOreReplaceables, Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState())
        );
        List<OreConfiguration.TargetBlockState> oreCopperTargetList = List.of(
            OreConfiguration.target(stoneOreReplaceables, Blocks.COPPER_ORE.defaultBlockState()),
            OreConfiguration.target(deepslateOreReplaceables, Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState())
        );
        List<OreConfiguration.TargetBlockState> oreCoalTargetList = List.of(
            OreConfiguration.target(stoneOreReplaceables, Blocks.COAL_ORE.defaultBlockState()),
            OreConfiguration.target(deepslateOreReplaceables, Blocks.DEEPSLATE_COAL_ORE.defaultBlockState())
        );
        FeatureUtils.register(context, ORE_MAGMA, Feature.ORE, new OreConfiguration(netherrack, Blocks.MAGMA_BLOCK.defaultBlockState(), 33));
        FeatureUtils.register(context, ORE_SOUL_SAND, Feature.ORE, new OreConfiguration(netherrack, Blocks.SOUL_SAND.defaultBlockState(), 12));
        FeatureUtils.register(context, ORE_NETHER_GOLD, Feature.ORE, new OreConfiguration(netherrack, Blocks.NETHER_GOLD_ORE.defaultBlockState(), 10));
        FeatureUtils.register(context, ORE_QUARTZ, Feature.ORE, new OreConfiguration(netherrack, Blocks.NETHER_QUARTZ_ORE.defaultBlockState(), 14));
        FeatureUtils.register(context, ORE_GRAVEL_NETHER, Feature.ORE, new OreConfiguration(netherrack, Blocks.GRAVEL.defaultBlockState(), 33));
        FeatureUtils.register(context, ORE_BLACKSTONE, Feature.ORE, new OreConfiguration(netherrack, Blocks.BLACKSTONE.defaultBlockState(), 33));
        FeatureUtils.register(context, ORE_DIRT, Feature.ORE, new OreConfiguration(naturalStone, Blocks.DIRT.defaultBlockState(), 33));
        FeatureUtils.register(context, ORE_GRAVEL, Feature.ORE, new OreConfiguration(naturalStone, Blocks.GRAVEL.defaultBlockState(), 33));
        FeatureUtils.register(context, ORE_GRANITE, Feature.ORE, new OreConfiguration(naturalStone, Blocks.GRANITE.defaultBlockState(), 64));
        FeatureUtils.register(context, ORE_DIORITE, Feature.ORE, new OreConfiguration(naturalStone, Blocks.DIORITE.defaultBlockState(), 64));
        FeatureUtils.register(context, ORE_ANDESITE, Feature.ORE, new OreConfiguration(naturalStone, Blocks.ANDESITE.defaultBlockState(), 64));
        FeatureUtils.register(context, ORE_TUFF, Feature.ORE, new OreConfiguration(naturalStone, Blocks.TUFF.defaultBlockState(), 64));
        FeatureUtils.register(context, ORE_COAL, Feature.ORE, new OreConfiguration(oreCoalTargetList, 17));
        FeatureUtils.register(context, ORE_COAL_BURIED, Feature.ORE, new OreConfiguration(oreCoalTargetList, 17, 0.5F));
        FeatureUtils.register(context, ORE_IRON, Feature.ORE, new OreConfiguration(oreIronTargetList, 9));
        FeatureUtils.register(context, ORE_IRON_SMALL, Feature.ORE, new OreConfiguration(oreIronTargetList, 4));
        FeatureUtils.register(context, ORE_GOLD, Feature.ORE, new OreConfiguration(oreGoldTargetList, 9));
        FeatureUtils.register(context, ORE_GOLD_BURIED, Feature.ORE, new OreConfiguration(oreGoldTargetList, 9, 0.5F));
        FeatureUtils.register(
            context,
            ORE_REDSTONE,
            Feature.ORE,
            new OreConfiguration(
                List.of(
                    OreConfiguration.target(stoneOreReplaceables, Blocks.REDSTONE_ORE.defaultBlockState()),
                    OreConfiguration.target(deepslateOreReplaceables, Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState())
                ),
                8
            )
        );
        FeatureUtils.register(context, ORE_DIAMOND_SMALL, Feature.ORE, new OreConfiguration(oreDiamondTargetList, 4, 0.5F));
        FeatureUtils.register(context, ORE_DIAMOND_LARGE, Feature.ORE, new OreConfiguration(oreDiamondTargetList, 12, 0.7F));
        FeatureUtils.register(context, ORE_DIAMOND_BURIED, Feature.ORE, new OreConfiguration(oreDiamondTargetList, 8, 1.0F));
        FeatureUtils.register(context, ORE_DIAMOND_MEDIUM, Feature.ORE, new OreConfiguration(oreDiamondTargetList, 8, 0.5F));
        FeatureUtils.register(context, ORE_LAPIS, Feature.ORE, new OreConfiguration(oreLapisTargetList, 7));
        FeatureUtils.register(context, ORE_LAPIS_BURIED, Feature.ORE, new OreConfiguration(oreLapisTargetList, 7, 1.0F));
        FeatureUtils.register(
            context,
            ORE_INFESTED,
            Feature.ORE,
            new OreConfiguration(
                List.of(
                    OreConfiguration.target(stoneOreReplaceables, Blocks.INFESTED_STONE.defaultBlockState()),
                    OreConfiguration.target(deepslateOreReplaceables, Blocks.INFESTED_DEEPSLATE.defaultBlockState())
                ),
                9
            )
        );
        FeatureUtils.register(
            context,
            ORE_EMERALD,
            Feature.ORE,
            new OreConfiguration(
                List.of(
                    OreConfiguration.target(stoneOreReplaceables, Blocks.EMERALD_ORE.defaultBlockState()),
                    OreConfiguration.target(deepslateOreReplaceables, Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState())
                ),
                3
            )
        );
        FeatureUtils.register(
            context,
            ORE_ANCIENT_DEBRIS_LARGE,
            Feature.SCATTERED_ORE,
            new OreConfiguration(netherOreReplaceables, Blocks.ANCIENT_DEBRIS.defaultBlockState(), 3, 1.0F)
        );
        FeatureUtils.register(
            context,
            ORE_ANCIENT_DEBRIS_SMALL,
            Feature.SCATTERED_ORE,
            new OreConfiguration(netherOreReplaceables, Blocks.ANCIENT_DEBRIS.defaultBlockState(), 2, 1.0F)
        );
        FeatureUtils.register(context, ORE_COPPPER_SMALL, Feature.ORE, new OreConfiguration(oreCopperTargetList, 10));
        FeatureUtils.register(context, ORE_COPPER_LARGE, Feature.ORE, new OreConfiguration(oreCopperTargetList, 20));
        FeatureUtils.register(context, ORE_CLAY, Feature.ORE, new OreConfiguration(naturalStone, Blocks.CLAY.defaultBlockState(), 33));
    }
}