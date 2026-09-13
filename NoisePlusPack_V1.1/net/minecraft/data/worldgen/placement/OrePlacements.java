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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
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

   private static List<PlacementModifier> orePlacement(PlacementModifier p_195347_, PlacementModifier p_195348_) {
      return List.of(p_195347_, InSquarePlacement.spread(), p_195348_, BiomeFilter.biome());
   }

   private static List<PlacementModifier> commonOrePlacement(int p_195344_, PlacementModifier p_195345_) {
      return orePlacement(CountPlacement.of(p_195344_), p_195345_);
   }

   private static List<PlacementModifier> rareOrePlacement(int p_195350_, PlacementModifier p_195351_) {
      return orePlacement(RarityFilter.onAverageOnceEvery(p_195350_), p_195351_);
   }

   public static void bootstrap(BootstrapContext<PlacedFeature> p_335493_) {
      HolderGetter<ConfiguredFeature<?, ?>> holdergetter = p_335493_.lookup(Registries.CONFIGURED_FEATURE);
      Holder<ConfiguredFeature<?, ?>> holder = holdergetter.getOrThrow(OreFeatures.ORE_MAGMA);
      Holder<ConfiguredFeature<?, ?>> holder1 = holdergetter.getOrThrow(OreFeatures.ORE_SOUL_SAND);
      Holder<ConfiguredFeature<?, ?>> holder2 = holdergetter.getOrThrow(OreFeatures.ORE_NETHER_GOLD);
      Holder<ConfiguredFeature<?, ?>> holder3 = holdergetter.getOrThrow(OreFeatures.ORE_QUARTZ);
      Holder<ConfiguredFeature<?, ?>> holder4 = holdergetter.getOrThrow(OreFeatures.ORE_GRAVEL_NETHER);
      Holder<ConfiguredFeature<?, ?>> holder5 = holdergetter.getOrThrow(OreFeatures.ORE_BLACKSTONE);
      Holder<ConfiguredFeature<?, ?>> holder6 = holdergetter.getOrThrow(OreFeatures.ORE_DIRT);
      Holder<ConfiguredFeature<?, ?>> holder7 = holdergetter.getOrThrow(OreFeatures.ORE_GRAVEL);
      Holder<ConfiguredFeature<?, ?>> holder8 = holdergetter.getOrThrow(OreFeatures.ORE_GRANITE);
      Holder<ConfiguredFeature<?, ?>> holder9 = holdergetter.getOrThrow(OreFeatures.ORE_DIORITE);
      Holder<ConfiguredFeature<?, ?>> holder10 = holdergetter.getOrThrow(OreFeatures.ORE_ANDESITE);
      Holder<ConfiguredFeature<?, ?>> holder11 = holdergetter.getOrThrow(OreFeatures.ORE_TUFF);
      Holder<ConfiguredFeature<?, ?>> holder12 = holdergetter.getOrThrow(OreFeatures.ORE_COAL);
      Holder<ConfiguredFeature<?, ?>> holder13 = holdergetter.getOrThrow(OreFeatures.ORE_COAL_BURIED);
      Holder<ConfiguredFeature<?, ?>> holder14 = holdergetter.getOrThrow(OreFeatures.ORE_IRON);
      Holder<ConfiguredFeature<?, ?>> holder15 = holdergetter.getOrThrow(OreFeatures.ORE_IRON_SMALL);
      Holder<ConfiguredFeature<?, ?>> holder16 = holdergetter.getOrThrow(OreFeatures.ORE_GOLD);
      Holder<ConfiguredFeature<?, ?>> holder17 = holdergetter.getOrThrow(OreFeatures.ORE_GOLD_BURIED);
      Holder<ConfiguredFeature<?, ?>> holder18 = holdergetter.getOrThrow(OreFeatures.ORE_REDSTONE);
      Holder<ConfiguredFeature<?, ?>> holder19 = holdergetter.getOrThrow(OreFeatures.ORE_DIAMOND_SMALL);
      Holder<ConfiguredFeature<?, ?>> holder20 = holdergetter.getOrThrow(OreFeatures.ORE_DIAMOND_MEDIUM);
      Holder<ConfiguredFeature<?, ?>> holder21 = holdergetter.getOrThrow(OreFeatures.ORE_DIAMOND_LARGE);
      Holder<ConfiguredFeature<?, ?>> holder22 = holdergetter.getOrThrow(OreFeatures.ORE_DIAMOND_BURIED);
      Holder<ConfiguredFeature<?, ?>> holder23 = holdergetter.getOrThrow(OreFeatures.ORE_LAPIS);
      Holder<ConfiguredFeature<?, ?>> holder24 = holdergetter.getOrThrow(OreFeatures.ORE_LAPIS_BURIED);
      Holder<ConfiguredFeature<?, ?>> holder25 = holdergetter.getOrThrow(OreFeatures.ORE_INFESTED);
      Holder<ConfiguredFeature<?, ?>> holder26 = holdergetter.getOrThrow(OreFeatures.ORE_EMERALD);
      Holder<ConfiguredFeature<?, ?>> holder27 = holdergetter.getOrThrow(OreFeatures.ORE_ANCIENT_DEBRIS_LARGE);
      Holder<ConfiguredFeature<?, ?>> holder28 = holdergetter.getOrThrow(OreFeatures.ORE_ANCIENT_DEBRIS_SMALL);
      Holder<ConfiguredFeature<?, ?>> holder29 = holdergetter.getOrThrow(OreFeatures.ORE_COPPPER_SMALL);
      Holder<ConfiguredFeature<?, ?>> holder30 = holdergetter.getOrThrow(OreFeatures.ORE_COPPER_LARGE);
      Holder<ConfiguredFeature<?, ?>> holder31 = holdergetter.getOrThrow(OreFeatures.ORE_CLAY);
      PlacementUtils.register(
         p_335493_, ORE_MAGMA, holder, commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.absolute(27), VerticalAnchor.absolute(36)))
      );
      PlacementUtils.register(
         p_335493_, ORE_SOUL_SAND, holder1, commonOrePlacement(12, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(31)))
      );
      PlacementUtils.register(p_335493_, ORE_GOLD_DELTAS, holder2, commonOrePlacement(20, PlacementUtils.RANGE_10_10));
      PlacementUtils.register(p_335493_, ORE_QUARTZ_DELTAS, holder3, commonOrePlacement(32, PlacementUtils.RANGE_10_10));
      PlacementUtils.register(p_335493_, ORE_GOLD_NETHER, holder2, commonOrePlacement(10, PlacementUtils.RANGE_10_10));
      PlacementUtils.register(p_335493_, ORE_QUARTZ_NETHER, holder3, commonOrePlacement(16, PlacementUtils.RANGE_10_10));
      PlacementUtils.register(
         p_335493_, ORE_GRAVEL_NETHER, holder4, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(5), VerticalAnchor.absolute(41)))
      );
      PlacementUtils.register(
         p_335493_, ORE_BLACKSTONE, holder5, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(5), VerticalAnchor.absolute(31)))
      );
      PlacementUtils.register(
         p_335493_, ORE_DIRT, holder6, commonOrePlacement(7, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(160)))
      );
      PlacementUtils.register(
         p_335493_, ORE_GRAVEL, holder7, commonOrePlacement(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top()))
      );
      PlacementUtils.register(
         p_335493_, ORE_GRANITE_UPPER, holder8, rareOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(128)))
      );
      PlacementUtils.register(
         p_335493_, ORE_GRANITE_LOWER, holder8, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)))
      );
      PlacementUtils.register(
         p_335493_, ORE_DIORITE_UPPER, holder9, rareOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(128)))
      );
      PlacementUtils.register(
         p_335493_, ORE_DIORITE_LOWER, holder9, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)))
      );
      PlacementUtils.register(
         p_335493_, ORE_ANDESITE_UPPER, holder10, rareOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(128)))
      );
      PlacementUtils.register(
         p_335493_, ORE_ANDESITE_LOWER, holder10, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)))
      );
      PlacementUtils.register(
         p_335493_, ORE_TUFF, holder11, commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(0)))
      );
      PlacementUtils.register(
         p_335493_, ORE_COAL_UPPER, holder12, commonOrePlacement(30, HeightRangePlacement.uniform(VerticalAnchor.absolute(136), VerticalAnchor.top()))
      );
      PlacementUtils.register(
         p_335493_, ORE_COAL_LOWER, holder13, commonOrePlacement(20, HeightRangePlacement.triangle(VerticalAnchor.absolute(0), VerticalAnchor.absolute(192)))
      );
      PlacementUtils.register(
         p_335493_, ORE_IRON_UPPER, holder14, commonOrePlacement(90, HeightRangePlacement.triangle(VerticalAnchor.absolute(80), VerticalAnchor.absolute(384)))
      );
      PlacementUtils.register(
         p_335493_, ORE_IRON_MIDDLE, holder14, commonOrePlacement(10, HeightRangePlacement.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56)))
      );
      PlacementUtils.register(
         p_335493_, ORE_IRON_SMALL, holder15, commonOrePlacement(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))
      );
      PlacementUtils.register(
         p_335493_, ORE_GOLD_EXTRA, holder16, commonOrePlacement(50, HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(256)))
      );
      PlacementUtils.register(
         p_335493_, ORE_GOLD, holder17, commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32)))
      );
      PlacementUtils.register(
         p_335493_,
         ORE_GOLD_LOWER,
         holder17,
         orePlacement(CountPlacement.of(UniformInt.of(0, 1)), HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-48)))
      );
      PlacementUtils.register(
         p_335493_, ORE_REDSTONE, holder18, commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(15)))
      );
      PlacementUtils.register(
         p_335493_,
         ORE_REDSTONE_LOWER,
         holder18,
         commonOrePlacement(8, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-32), VerticalAnchor.aboveBottom(32)))
      );
      PlacementUtils.register(
         p_335493_,
         ORE_DIAMOND,
         holder19,
         commonOrePlacement(7, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
      );
      PlacementUtils.register(
         p_335493_,
         ORE_DIAMOND_MEDIUM,
         holder20,
         commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-4)))
      );
      PlacementUtils.register(
         p_335493_,
         ORE_DIAMOND_LARGE,
         holder21,
         rareOrePlacement(9, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
      );
      PlacementUtils.register(
         p_335493_,
         ORE_DIAMOND_BURIED,
         holder22,
         commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
      );
      PlacementUtils.register(
         p_335493_, ORE_LAPIS, holder23, commonOrePlacement(2, HeightRangePlacement.triangle(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32)))
      );
      PlacementUtils.register(
         p_335493_, ORE_LAPIS_BURIED, holder24, commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))
      );
      PlacementUtils.register(
         p_335493_, ORE_INFESTED, holder25, commonOrePlacement(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(63)))
      );
      PlacementUtils.register(
         p_335493_, ORE_EMERALD, holder26, commonOrePlacement(100, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(480)))
      );
      PlacementUtils.register(
         p_335493_,
         ORE_ANCIENT_DEBRIS_LARGE,
         holder27,
         InSquarePlacement.spread(),
         HeightRangePlacement.triangle(VerticalAnchor.absolute(8), VerticalAnchor.absolute(24)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(p_335493_, ORE_ANCIENT_DEBRIS_SMALL, holder28, InSquarePlacement.spread(), PlacementUtils.RANGE_8_8, BiomeFilter.biome());
      PlacementUtils.register(
         p_335493_, ORE_COPPER, holder29, commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)))
      );
      PlacementUtils.register(
         p_335493_,
         ORE_COPPER_LARGE,
         holder30,
         commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)))
      );
      PlacementUtils.register(p_335493_, ORE_CLAY, holder31, commonOrePlacement(46, PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT));
   }
}
