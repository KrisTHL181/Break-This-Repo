package net.minecraft.data.worldgen.features;

import java.util.List;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.HeightMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

public class OreFeatures {
   public static final ResourceKey<Feature> ORE_MAGMA = FeatureUtils.createKey("ore_magma");
   public static final ResourceKey<Feature> ORE_SOUL_SAND = FeatureUtils.createKey("ore_soul_sand");
   public static final ResourceKey<Feature> ORE_NETHER_GOLD = FeatureUtils.createKey("ore_nether_gold");
   public static final ResourceKey<Feature> ORE_QUARTZ = FeatureUtils.createKey("ore_quartz");
   public static final ResourceKey<Feature> ORE_GRAVEL_NETHER = FeatureUtils.createKey("ore_gravel_nether");
   public static final ResourceKey<Feature> ORE_BLACKSTONE = FeatureUtils.createKey("ore_blackstone");
   public static final ResourceKey<Feature> ORE_DIRT = FeatureUtils.createKey("ore_dirt");
   public static final ResourceKey<Feature> ORE_GRAVEL = FeatureUtils.createKey("ore_gravel");
   public static final ResourceKey<Feature> ORE_GRANITE = FeatureUtils.createKey("ore_granite");
   public static final ResourceKey<Feature> ORE_DIORITE = FeatureUtils.createKey("ore_diorite");
   public static final ResourceKey<Feature> ORE_ANDESITE = FeatureUtils.createKey("ore_andesite");
   public static final ResourceKey<Feature> ORE_TUFF = FeatureUtils.createKey("ore_tuff");
   public static final ResourceKey<Feature> ORE_COAL = FeatureUtils.createKey("ore_coal");
   public static final ResourceKey<Feature> ORE_COAL_BURIED = FeatureUtils.createKey("ore_coal_buried");
   public static final ResourceKey<Feature> ORE_IRON = FeatureUtils.createKey("ore_iron");
   public static final ResourceKey<Feature> ORE_IRON_SMALL = FeatureUtils.createKey("ore_iron_small");
   public static final ResourceKey<Feature> ORE_GOLD = FeatureUtils.createKey("ore_gold");
   public static final ResourceKey<Feature> ORE_GOLD_BURIED = FeatureUtils.createKey("ore_gold_buried");
   public static final ResourceKey<Feature> ORE_REDSTONE = FeatureUtils.createKey("ore_redstone");
   public static final ResourceKey<Feature> ORE_DIAMOND_SMALL = FeatureUtils.createKey("ore_diamond_small");
   public static final ResourceKey<Feature> ORE_DIAMOND_MEDIUM = FeatureUtils.createKey("ore_diamond_medium");
   public static final ResourceKey<Feature> ORE_DIAMOND_LARGE = FeatureUtils.createKey("ore_diamond_large");
   public static final ResourceKey<Feature> ORE_DIAMOND_BURIED = FeatureUtils.createKey("ore_diamond_buried");
   public static final ResourceKey<Feature> ORE_LAPIS = FeatureUtils.createKey("ore_lapis");
   public static final ResourceKey<Feature> ORE_LAPIS_BURIED = FeatureUtils.createKey("ore_lapis_buried");
   public static final ResourceKey<Feature> ORE_INFESTED = FeatureUtils.createKey("ore_infested");
   public static final ResourceKey<Feature> ORE_EMERALD = FeatureUtils.createKey("ore_emerald");
   public static final ResourceKey<Feature> ORE_ANCIENT_DEBRIS_LARGE = FeatureUtils.createKey("ore_ancient_debris_large");
   public static final ResourceKey<Feature> ORE_ANCIENT_DEBRIS_SMALL = FeatureUtils.createKey("ore_ancient_debris_small");
   public static final ResourceKey<Feature> ORE_COPPPER_SMALL = FeatureUtils.createKey("ore_copper_small");
   public static final ResourceKey<Feature> ORE_COPPER_LARGE = FeatureUtils.createKey("ore_copper_large");
   public static final ResourceKey<Feature> ORE_CLAY = FeatureUtils.createKey("ore_clay");

   public static void bootstrap(final BootstrapContext<Feature> context) {
      RuleTest naturalStone = new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD);
      RuleTest stoneOreReplaceables = RuleTest.either(
         new TagMatchTest(BlockTags.HEIGHT_SPECIFIC_ORE_REPLACEABLES), HeightMatchTest.min(0), new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES)
      );
      RuleTest deepslateOreReplaceables = RuleTest.either(
         new TagMatchTest(BlockTags.HEIGHT_SPECIFIC_ORE_REPLACEABLES), HeightMatchTest.max(8), new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
      );
      RuleTest netherrack = new BlockMatchTest(Blocks.NETHERRACK);
      RuleTest netherOreReplaceables = new TagMatchTest(BlockTags.BASE_STONE_NETHER);
      List<BlockReplacement> oreIronTargetList = List.of(
         BlockReplacement.replace(stoneOreReplaceables, Blocks.IRON_ORE.defaultBlockState()),
         BlockReplacement.replace(deepslateOreReplaceables, Blocks.DEEPSLATE_IRON_ORE.defaultBlockState())
      );
      List<BlockReplacement> oreGoldTargetList = List.of(
         BlockReplacement.replace(stoneOreReplaceables, Blocks.GOLD_ORE.defaultBlockState()),
         BlockReplacement.replace(deepslateOreReplaceables, Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState())
      );
      List<BlockReplacement> oreDiamondTargetList = List.of(
         BlockReplacement.replace(stoneOreReplaceables, Blocks.DIAMOND_ORE.defaultBlockState()),
         BlockReplacement.replace(deepslateOreReplaceables, Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState())
      );
      List<BlockReplacement> oreLapisTargetList = List.of(
         BlockReplacement.replace(stoneOreReplaceables, Blocks.LAPIS_ORE.defaultBlockState()),
         BlockReplacement.replace(deepslateOreReplaceables, Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState())
      );
      List<BlockReplacement> oreCopperTargetList = List.of(
         BlockReplacement.replace(stoneOreReplaceables, Blocks.COPPER_ORE.defaultBlockState()),
         BlockReplacement.replace(deepslateOreReplaceables, Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState())
      );
      List<BlockReplacement> oreCoalTargetList = List.of(
         BlockReplacement.replace(stoneOreReplaceables, Blocks.COAL_ORE.defaultBlockState()),
         BlockReplacement.replace(deepslateOreReplaceables, Blocks.DEEPSLATE_COAL_ORE.defaultBlockState())
      );
      context.register(ORE_MAGMA, new OreFeature(netherrack, Blocks.MAGMA_BLOCK.defaultBlockState(), 33));
      context.register(ORE_SOUL_SAND, new OreFeature(netherrack, Blocks.SOUL_SAND.defaultBlockState(), 12));
      context.register(ORE_NETHER_GOLD, new OreFeature(netherrack, Blocks.NETHER_GOLD_ORE.defaultBlockState(), 10));
      context.register(ORE_QUARTZ, new OreFeature(netherrack, Blocks.NETHER_QUARTZ_ORE.defaultBlockState(), 14));
      context.register(ORE_GRAVEL_NETHER, new OreFeature(netherrack, Blocks.GRAVEL.defaultBlockState(), 33));
      context.register(ORE_BLACKSTONE, new OreFeature(netherrack, Blocks.BLACKSTONE.defaultBlockState(), 33));
      context.register(ORE_DIRT, new OreFeature(naturalStone, Blocks.DIRT.defaultBlockState(), 33));
      context.register(ORE_GRAVEL, new OreFeature(naturalStone, Blocks.GRAVEL.defaultBlockState(), 33));
      context.register(ORE_GRANITE, new OreFeature(naturalStone, Blocks.GRANITE.defaultBlockState(), 64));
      context.register(ORE_DIORITE, new OreFeature(naturalStone, Blocks.DIORITE.defaultBlockState(), 64));
      context.register(ORE_ANDESITE, new OreFeature(naturalStone, Blocks.ANDESITE.defaultBlockState(), 64));
      context.register(ORE_TUFF, new OreFeature(naturalStone, Blocks.TUFF.defaultBlockState(), 64));
      context.register(ORE_COAL, new OreFeature(oreCoalTargetList, 17));
      context.register(ORE_COAL_BURIED, new OreFeature(oreCoalTargetList, 17, 0.5F));
      context.register(ORE_IRON, new OreFeature(oreIronTargetList, 9));
      context.register(ORE_IRON_SMALL, new OreFeature(oreIronTargetList, 4));
      context.register(ORE_GOLD, new OreFeature(oreGoldTargetList, 9));
      context.register(ORE_GOLD_BURIED, new OreFeature(oreGoldTargetList, 9, 0.5F));
      context.register(
         ORE_REDSTONE,
         new OreFeature(
            List.of(
               BlockReplacement.replace(stoneOreReplaceables, Blocks.REDSTONE_ORE.defaultBlockState()),
               BlockReplacement.replace(deepslateOreReplaceables, Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState())
            ),
            8
         )
      );
      context.register(ORE_DIAMOND_SMALL, new OreFeature(oreDiamondTargetList, 4, 0.5F));
      context.register(ORE_DIAMOND_LARGE, new OreFeature(oreDiamondTargetList, 12, 0.7F));
      context.register(ORE_DIAMOND_BURIED, new OreFeature(oreDiamondTargetList, 8, 1.0F));
      context.register(ORE_DIAMOND_MEDIUM, new OreFeature(oreDiamondTargetList, 8, 0.5F));
      context.register(ORE_LAPIS, new OreFeature(oreLapisTargetList, 7));
      context.register(ORE_LAPIS_BURIED, new OreFeature(oreLapisTargetList, 7, 1.0F));
      context.register(
         ORE_INFESTED,
         new OreFeature(
            List.of(
               BlockReplacement.replace(stoneOreReplaceables, Blocks.INFESTED_STONE.defaultBlockState()),
               BlockReplacement.replace(deepslateOreReplaceables, Blocks.INFESTED_DEEPSLATE.defaultBlockState())
            ),
            9
         )
      );
      context.register(
         ORE_EMERALD,
         new OreFeature(
            List.of(
               BlockReplacement.replace(stoneOreReplaceables, Blocks.EMERALD_ORE.defaultBlockState()),
               BlockReplacement.replace(deepslateOreReplaceables, Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState())
            ),
            3
         )
      );
      context.register(ORE_ANCIENT_DEBRIS_LARGE, new ScatteredOreFeature(netherOreReplaceables, Blocks.ANCIENT_DEBRIS.defaultBlockState(), 3, 1.0F));
      context.register(ORE_ANCIENT_DEBRIS_SMALL, new ScatteredOreFeature(netherOreReplaceables, Blocks.ANCIENT_DEBRIS.defaultBlockState(), 2, 1.0F));
      context.register(ORE_COPPPER_SMALL, new OreFeature(oreCopperTargetList, 10));
      context.register(ORE_COPPER_LARGE, new OreFeature(oreCopperTargetList, 20));
      context.register(ORE_CLAY, new OreFeature(naturalStone, Blocks.CLAY.defaultBlockState(), 33));
   }
}
