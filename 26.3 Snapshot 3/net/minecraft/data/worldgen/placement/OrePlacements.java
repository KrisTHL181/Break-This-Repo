package net.minecraft.data.worldgen.placement;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

public class OrePlacements {
   public static final ResourceKey<PlacedFeature> ORE_MAGMA = PlacementUtils.createKey("ore_magma");
   public static final ResourceKey<PlacedFeature> ORE_SOUL_SAND = PlacementUtils.createKey("ore_soul_sand");
   public static final ResourceKey<PlacedFeature> ORE_GOLD_DELTAS = PlacementUtils.createKey("ore_gold_deltas");
   public static final ResourceKey<PlacedFeature> ORE_QUARTZ_DELTAS = PlacementUtils.createKey("ore_quartz_deltas");
   public static final ResourceKey<PlacedFeature> ORE_GOLD_NETHER = PlacementUtils.createKey("ore_gold_nether");
   public static final ResourceKey<PlacedFeature> ORE_QUARTZ_NETHER = PlacementUtils.createKey("ore_quartz_nether");
   public static final ResourceKey<PlacedFeature> ORE_GRAVEL_NETHER = PlacementUtils.createKey("ore_gravel_nether");
   public static final ResourceKey<PlacedFeature> ORE_BLACKSTONE = PlacementUtils.createKey("ore_blackstone");
   public static final ResourceKey<PlacedFeature> ORE_DIRT = PlacementUtils.createKey("ore_dirt");
   public static final ResourceKey<PlacedFeature> ORE_GRAVEL = PlacementUtils.createKey("ore_gravel");
   public static final ResourceKey<PlacedFeature> ORE_GRANITE_UPPER = PlacementUtils.createKey("ore_granite_upper");
   public static final ResourceKey<PlacedFeature> ORE_GRANITE_LOWER = PlacementUtils.createKey("ore_granite_lower");
   public static final ResourceKey<PlacedFeature> ORE_DIORITE_UPPER = PlacementUtils.createKey("ore_diorite_upper");
   public static final ResourceKey<PlacedFeature> ORE_DIORITE_LOWER = PlacementUtils.createKey("ore_diorite_lower");
   public static final ResourceKey<PlacedFeature> ORE_ANDESITE_UPPER = PlacementUtils.createKey("ore_andesite_upper");
   public static final ResourceKey<PlacedFeature> ORE_ANDESITE_LOWER = PlacementUtils.createKey("ore_andesite_lower");
   public static final ResourceKey<PlacedFeature> ORE_TUFF = PlacementUtils.createKey("ore_tuff");
   public static final ResourceKey<PlacedFeature> ORE_COAL_UPPER = PlacementUtils.createKey("ore_coal_upper");
   public static final ResourceKey<PlacedFeature> ORE_COAL_LOWER = PlacementUtils.createKey("ore_coal_lower");
   public static final ResourceKey<PlacedFeature> ORE_IRON_UPPER = PlacementUtils.createKey("ore_iron_upper");
   public static final ResourceKey<PlacedFeature> ORE_IRON_MIDDLE = PlacementUtils.createKey("ore_iron_middle");
   public static final ResourceKey<PlacedFeature> ORE_IRON_SMALL = PlacementUtils.createKey("ore_iron_small");
   public static final ResourceKey<PlacedFeature> ORE_GOLD_EXTRA = PlacementUtils.createKey("ore_gold_extra");
   public static final ResourceKey<PlacedFeature> ORE_GOLD = PlacementUtils.createKey("ore_gold");
   public static final ResourceKey<PlacedFeature> ORE_GOLD_LOWER = PlacementUtils.createKey("ore_gold_lower");
   public static final ResourceKey<PlacedFeature> ORE_REDSTONE = PlacementUtils.createKey("ore_redstone");
   public static final ResourceKey<PlacedFeature> ORE_REDSTONE_LOWER = PlacementUtils.createKey("ore_redstone_lower");
   public static final ResourceKey<PlacedFeature> ORE_DIAMOND = PlacementUtils.createKey("ore_diamond");
   public static final ResourceKey<PlacedFeature> ORE_DIAMOND_MEDIUM = PlacementUtils.createKey("ore_diamond_medium");
   public static final ResourceKey<PlacedFeature> ORE_DIAMOND_LARGE = PlacementUtils.createKey("ore_diamond_large");
   public static final ResourceKey<PlacedFeature> ORE_DIAMOND_BURIED = PlacementUtils.createKey("ore_diamond_buried");
   public static final ResourceKey<PlacedFeature> ORE_LAPIS = PlacementUtils.createKey("ore_lapis");
   public static final ResourceKey<PlacedFeature> ORE_LAPIS_BURIED = PlacementUtils.createKey("ore_lapis_buried");
   public static final ResourceKey<PlacedFeature> ORE_INFESTED = PlacementUtils.createKey("ore_infested");
   public static final ResourceKey<PlacedFeature> ORE_EMERALD = PlacementUtils.createKey("ore_emerald");
   public static final ResourceKey<PlacedFeature> ORE_ANCIENT_DEBRIS_LARGE = PlacementUtils.createKey("ore_ancient_debris_large");
   public static final ResourceKey<PlacedFeature> ORE_ANCIENT_DEBRIS_SMALL = PlacementUtils.createKey("ore_debris_small");
   public static final ResourceKey<PlacedFeature> ORE_COPPER = PlacementUtils.createKey("ore_copper");
   public static final ResourceKey<PlacedFeature> ORE_COPPER_LARGE = PlacementUtils.createKey("ore_copper_large");
   public static final ResourceKey<PlacedFeature> ORE_CLAY = PlacementUtils.createKey("ore_clay");

   private static List<PlacementModifier> orePlacement(final PlacementModifier frequencyModifier, final PlacementModifier heightRange) {
      return List.of(frequencyModifier, InSquarePlacement.spread(), heightRange, BiomeFilter.biome());
   }

   private static List<PlacementModifier> commonOrePlacement(final int count, final PlacementModifier heightRange) {
      return orePlacement(CountPlacement.of(count), heightRange);
   }

   private static List<PlacementModifier> rareOrePlacement(final int rarity, final PlacementModifier heightRange) {
      return orePlacement(RarityFilter.onAverageOnceEvery(rarity), heightRange);
   }

   public static void bootstrap(final BootstrapContext<PlacedFeature> context) {
      HolderGetter<Feature> configuredFeatures = context.lookup(Registries.FEATURE);
      Holder<Feature> oreMagma = configuredFeatures.getOrThrow(OreFeatures.ORE_MAGMA);
      Holder<Feature> oreSoulSand = configuredFeatures.getOrThrow(OreFeatures.ORE_SOUL_SAND);
      Holder<Feature> oreNetherGold = configuredFeatures.getOrThrow(OreFeatures.ORE_NETHER_GOLD);
      Holder<Feature> oreQuartz = configuredFeatures.getOrThrow(OreFeatures.ORE_QUARTZ);
      Holder<Feature> oreGravelNether = configuredFeatures.getOrThrow(OreFeatures.ORE_GRAVEL_NETHER);
      Holder<Feature> oreBlackstone = configuredFeatures.getOrThrow(OreFeatures.ORE_BLACKSTONE);
      Holder<Feature> oreDirt = configuredFeatures.getOrThrow(OreFeatures.ORE_DIRT);
      Holder<Feature> oreGravel = configuredFeatures.getOrThrow(OreFeatures.ORE_GRAVEL);
      Holder<Feature> oreGranite = configuredFeatures.getOrThrow(OreFeatures.ORE_GRANITE);
      Holder<Feature> oreDiorite = configuredFeatures.getOrThrow(OreFeatures.ORE_DIORITE);
      Holder<Feature> oreAndesite = configuredFeatures.getOrThrow(OreFeatures.ORE_ANDESITE);
      Holder<Feature> oreTuff = configuredFeatures.getOrThrow(OreFeatures.ORE_TUFF);
      Holder<Feature> oreCoal = configuredFeatures.getOrThrow(OreFeatures.ORE_COAL);
      Holder<Feature> oreCoalBuried = configuredFeatures.getOrThrow(OreFeatures.ORE_COAL_BURIED);
      Holder<Feature> oreIron = configuredFeatures.getOrThrow(OreFeatures.ORE_IRON);
      Holder<Feature> oreIronSmall = configuredFeatures.getOrThrow(OreFeatures.ORE_IRON_SMALL);
      Holder<Feature> oreGold = configuredFeatures.getOrThrow(OreFeatures.ORE_GOLD);
      Holder<Feature> oreGoldBuried = configuredFeatures.getOrThrow(OreFeatures.ORE_GOLD_BURIED);
      Holder<Feature> oreRedstone = configuredFeatures.getOrThrow(OreFeatures.ORE_REDSTONE);
      Holder<Feature> oreDiamondSmall = configuredFeatures.getOrThrow(OreFeatures.ORE_DIAMOND_SMALL);
      Holder<Feature> oreDiamondMedium = configuredFeatures.getOrThrow(OreFeatures.ORE_DIAMOND_MEDIUM);
      Holder<Feature> oreDiamondLarge = configuredFeatures.getOrThrow(OreFeatures.ORE_DIAMOND_LARGE);
      Holder<Feature> oreDiamondBuried = configuredFeatures.getOrThrow(OreFeatures.ORE_DIAMOND_BURIED);
      Holder<Feature> oreLapis = configuredFeatures.getOrThrow(OreFeatures.ORE_LAPIS);
      Holder<Feature> oreLapisBuried = configuredFeatures.getOrThrow(OreFeatures.ORE_LAPIS_BURIED);
      Holder<Feature> oreInfested = configuredFeatures.getOrThrow(OreFeatures.ORE_INFESTED);
      Holder<Feature> oreEmerald = configuredFeatures.getOrThrow(OreFeatures.ORE_EMERALD);
      Holder<Feature> oreAncientDebrisLarge = configuredFeatures.getOrThrow(OreFeatures.ORE_ANCIENT_DEBRIS_LARGE);
      Holder<Feature> oreAncientDebrisSmall = configuredFeatures.getOrThrow(OreFeatures.ORE_ANCIENT_DEBRIS_SMALL);
      Holder<Feature> oreCoppperSmall = configuredFeatures.getOrThrow(OreFeatures.ORE_COPPPER_SMALL);
      Holder<Feature> oreCopperLarge = configuredFeatures.getOrThrow(OreFeatures.ORE_COPPER_LARGE);
      Holder<Feature> oreClay = configuredFeatures.getOrThrow(OreFeatures.ORE_CLAY);
      PlacementUtils.register(
         context, ORE_MAGMA, oreMagma, commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.absolute(27), VerticalAnchor.absolute(36)))
      );
      PlacementUtils.register(
         context, ORE_SOUL_SAND, oreSoulSand, commonOrePlacement(12, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(31)))
      );
      PlacementUtils.register(context, ORE_GOLD_DELTAS, oreNetherGold, commonOrePlacement(20, PlacementUtils.RANGE_10_10));
      PlacementUtils.register(context, ORE_QUARTZ_DELTAS, oreQuartz, commonOrePlacement(32, PlacementUtils.RANGE_10_10));
      PlacementUtils.register(context, ORE_GOLD_NETHER, oreNetherGold, commonOrePlacement(10, PlacementUtils.RANGE_10_10));
      PlacementUtils.register(context, ORE_QUARTZ_NETHER, oreQuartz, commonOrePlacement(16, PlacementUtils.RANGE_10_10));
      PlacementUtils.register(
         context,
         ORE_GRAVEL_NETHER,
         oreGravelNether,
         commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(5), VerticalAnchor.absolute(41)))
      );
      PlacementUtils.register(
         context, ORE_BLACKSTONE, oreBlackstone, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(5), VerticalAnchor.absolute(31)))
      );
      PlacementUtils.register(
         context, ORE_DIRT, oreDirt, commonOrePlacement(7, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(160)))
      );
      PlacementUtils.register(
         context, ORE_GRAVEL, oreGravel, commonOrePlacement(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top()))
      );
      PlacementUtils.register(
         context, ORE_GRANITE_UPPER, oreGranite, rareOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(128)))
      );
      PlacementUtils.register(
         context, ORE_GRANITE_LOWER, oreGranite, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)))
      );
      PlacementUtils.register(
         context, ORE_DIORITE_UPPER, oreDiorite, rareOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(128)))
      );
      PlacementUtils.register(
         context, ORE_DIORITE_LOWER, oreDiorite, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)))
      );
      PlacementUtils.register(
         context, ORE_ANDESITE_UPPER, oreAndesite, rareOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(128)))
      );
      PlacementUtils.register(
         context, ORE_ANDESITE_LOWER, oreAndesite, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)))
      );
      PlacementUtils.register(
         context, ORE_TUFF, oreTuff, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(0)))
      );
      PlacementUtils.register(
         context, ORE_COAL_UPPER, oreCoal, commonOrePlacement(30, HeightRangePlacement.uniform(VerticalAnchor.absolute(136), VerticalAnchor.top()))
      );
      PlacementUtils.register(
         context,
         ORE_COAL_LOWER,
         oreCoalBuried,
         commonOrePlacement(20, HeightRangePlacement.triangle(VerticalAnchor.absolute(0), VerticalAnchor.absolute(192)))
      );
      PlacementUtils.register(
         context, ORE_IRON_UPPER, oreIron, commonOrePlacement(90, HeightRangePlacement.triangle(VerticalAnchor.absolute(80), VerticalAnchor.absolute(384)))
      );
      PlacementUtils.register(
         context, ORE_IRON_MIDDLE, oreIron, commonOrePlacement(10, HeightRangePlacement.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56)))
      );
      PlacementUtils.register(
         context, ORE_IRON_SMALL, oreIronSmall, commonOrePlacement(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))
      );
      PlacementUtils.register(
         context, ORE_GOLD_EXTRA, oreGold, commonOrePlacement(50, HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(256)))
      );
      PlacementUtils.register(
         context, ORE_GOLD, oreGoldBuried, commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32)))
      );
      PlacementUtils.register(
         context,
         ORE_GOLD_LOWER,
         oreGoldBuried,
         orePlacement(CountPlacement.of(UniformInt.of(0, 1)), HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-48)))
      );
      PlacementUtils.register(
         context, ORE_REDSTONE, oreRedstone, commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(15)))
      );
      PlacementUtils.register(
         context,
         ORE_REDSTONE_LOWER,
         oreRedstone,
         commonOrePlacement(8, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-32), VerticalAnchor.aboveBottom(32)))
      );
      PlacementUtils.register(
         context,
         ORE_DIAMOND,
         oreDiamondSmall,
         commonOrePlacement(7, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
      );
      PlacementUtils.register(
         context,
         ORE_DIAMOND_MEDIUM,
         oreDiamondMedium,
         commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-4)))
      );
      PlacementUtils.register(
         context,
         ORE_DIAMOND_LARGE,
         oreDiamondLarge,
         rareOrePlacement(9, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
      );
      PlacementUtils.register(
         context,
         ORE_DIAMOND_BURIED,
         oreDiamondBuried,
         commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
      );
      PlacementUtils.register(
         context, ORE_LAPIS, oreLapis, commonOrePlacement(2, HeightRangePlacement.triangle(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32)))
      );
      PlacementUtils.register(
         context, ORE_LAPIS_BURIED, oreLapisBuried, commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))
      );
      PlacementUtils.register(
         context, ORE_INFESTED, oreInfested, commonOrePlacement(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(63)))
      );
      PlacementUtils.register(
         context, ORE_EMERALD, oreEmerald, commonOrePlacement(100, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(480)))
      );
      PlacementUtils.register(
         context,
         ORE_ANCIENT_DEBRIS_LARGE,
         oreAncientDebrisLarge,
         InSquarePlacement.spread(),
         HeightRangePlacement.triangle(VerticalAnchor.absolute(8), VerticalAnchor.absolute(24)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(
         context, ORE_ANCIENT_DEBRIS_SMALL, oreAncientDebrisSmall, InSquarePlacement.spread(), PlacementUtils.RANGE_8_8, BiomeFilter.biome()
      );
      PlacementUtils.register(
         context,
         ORE_COPPER,
         oreCoppperSmall,
         commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)))
      );
      PlacementUtils.register(
         context,
         ORE_COPPER_LARGE,
         oreCopperLarge,
         commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)))
      );
      PlacementUtils.register(context, ORE_CLAY, oreClay, commonOrePlacement(46, PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT));
   }
}
