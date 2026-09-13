package net.minecraft.data.worldgen.material;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public class OverworldMaterialRules {
   public static final ResourceKey<SurfaceRules.RuleSource> OVERWORLD = createKey("overworld");
   public static final ResourceKey<SurfaceRules.RuleSource> OVERWORLD_CAVES = createKey("overworld_caves");
   public static final ResourceKey<SurfaceRules.RuleSource> OVERWORLD_FLOATING_ISLANDS = createKey("overworld_floating_islands");
   private static final ResourceKey<SurfaceRules.RuleSource> SURFACE = createKey("overworld/surface");
   private static final ResourceKey<SurfaceRules.RuleSource> UNDERGROUND = createKey("overworld/underground");
   private static final ResourceKey<SurfaceRules.RuleSource> SULFUR_CAVE_BANDS = createKey("overworld/sulfur_cave_bands");
   private static final ResourceKey<SurfaceRules.RuleSource> BIOME_SURFACE = createKey("overworld/biome_surface");
   private static final ResourceKey<SurfaceRules.RuleSource> DAPPLED_FOREST_SURFACE = createKey("overworld/biome_surface/dappled_forest");
   private static final ResourceKey<SurfaceRules.RuleSource> DEFAULT_BIOME_SURFACE = createKey("overworld/biome_surface/default");
   private static final ResourceKey<SurfaceRules.RuleSource> DRIPSTONE_CAVES_SURFACE = createKey("overworld/biome_surface/dripstone_caves");
   private static final ResourceKey<SurfaceRules.RuleSource> FROZEN_PEAKS_SURFACE = createKey("overworld/biome_surface/frozen_peaks");
   private static final ResourceKey<SurfaceRules.RuleSource> GROVE_SURFACE = createKey("overworld/biome_surface/grove");
   private static final ResourceKey<SurfaceRules.RuleSource> ICE_SPIKES_SURFACE = createKey("overworld/biome_surface/ice_spikes");
   private static final ResourceKey<SurfaceRules.RuleSource> JAGGED_PEAKS_SURFACE = createKey("overworld/biome_surface/jagged_peaks");
   private static final ResourceKey<SurfaceRules.RuleSource> MANGROVE_SWAMP_SURFACE = createKey("overworld/biome_surface/mangrove_swamp");
   private static final ResourceKey<SurfaceRules.RuleSource> MUSHROOM_FIELDS_SURFACE = createKey("overworld/biome_surface/mushroom_fields");
   private static final ResourceKey<SurfaceRules.RuleSource> OLD_GROWTH_PINE_TAIGA_SURFACE = createKey("overworld/biome_surface/old_growth_pine_taiga");
   private static final ResourceKey<SurfaceRules.RuleSource> SNOWY_SLOPES_SURFACE = createKey("overworld/biome_surface/snowy_slopes");
   private static final ResourceKey<SurfaceRules.RuleSource> STONY_PEAKS_SURFACE = createKey("overworld/biome_surface/stony_peaks");
   private static final ResourceKey<SurfaceRules.RuleSource> STONY_SHORE_SURFACE = createKey("overworld/biome_surface/stony_shore");
   private static final ResourceKey<SurfaceRules.RuleSource> SULFUR_CAVES_SURFACE = createKey("overworld/biome_surface/sulfur_caves");
   private static final ResourceKey<SurfaceRules.RuleSource> WINDSWEPT_GRAVELLY_HILLS_SURFACE = createKey("overworld/biome_surface/windswept_gravelly_hills");
   private static final ResourceKey<SurfaceRules.RuleSource> WINDSWEPT_HILLS_SURFACE = createKey("overworld/biome_surface/windswept_hills");
   private static final ResourceKey<SurfaceRules.RuleSource> WINDSWEPT_SAVANNA_SURFACE = createKey("overworld/biome_surface/windswept_savanna");
   private static final ResourceKey<SurfaceRules.RuleSource> UNDER_BIOME_SURFACE = createKey("overworld/under_biome_surface");
   private static final ResourceKey<SurfaceRules.RuleSource> DEFAULT_UNDER_BIOME_SURFACE = createKey("overworld/under_biome_surface/default");
   private static final ResourceKey<SurfaceRules.RuleSource> FROZEN_PEAKS_UNDER_SURFACE = createKey("overworld/under_biome_surface/frozen_peaks");
   private static final ResourceKey<SurfaceRules.RuleSource> GROVE_UNDER_SURFACE = createKey("overworld/under_biome_surface/grove");
   private static final ResourceKey<SurfaceRules.RuleSource> JAGGED_PEAKS_UNDER_SURFACE = createKey("overworld/under_biome_surface/jagged_peaks");
   private static final ResourceKey<SurfaceRules.RuleSource> SNOWY_SLOPES_UNDER_SURFACE = createKey("overworld/under_biome_surface/snowy_slopes");
   private static final ResourceKey<SurfaceRules.RuleSource> WINDSWEPT_GRAVELLY_HILLS_UNDER_SURFACE = createKey(
      "overworld/under_biome_surface/windswept_gravelly_hills"
   );
   private static final ResourceKey<SurfaceRules.RuleSource> WINDSWEPT_SAVANNA_UNDER_SURFACE = createKey("overworld/under_biome_surface/windswept_savanna");
   private static final ResourceKey<SurfaceRules.RuleSource> SAND_OR_SANDSTONE_IF_CEILING = createKey("overworld/sand_or_sandstone_if_ceiling");
   private static final ResourceKey<SurfaceRules.RuleSource> GRAVEL_OR_STONE_IF_CEILING = createKey("overworld/gravel_or_stone_if_ceiling");
   private static final ResourceKey<SurfaceRules.RuleSource> POWDER_SNOW_SURFACE = createKey("overworld/powder_snow_surface");
   private static final ResourceKey<SurfaceRules.RuleSource> POWDER_SNOW_UNDER_SURFACE = createKey("overworld/powder_snow_under_surface");
   private static final SurfaceRules.RuleSource AIR = makeStateRule(Blocks.AIR);
   private static final SurfaceRules.RuleSource WHITE_TERRACOTTA = makeStateRule(Blocks.DYED_TERRACOTTA.white());
   private static final SurfaceRules.RuleSource ORANGE_TERRACOTTA = makeStateRule(Blocks.DYED_TERRACOTTA.orange());
   private static final SurfaceRules.RuleSource TERRACOTTA = makeStateRule(Blocks.TERRACOTTA);
   private static final SurfaceRules.RuleSource RED_SAND = makeStateRule(Blocks.RED_SAND);
   private static final SurfaceRules.RuleSource RED_SANDSTONE = makeStateRule(Blocks.RED_SANDSTONE);
   private static final SurfaceRules.RuleSource STONE = makeStateRule(Blocks.STONE);
   private static final SurfaceRules.RuleSource DEEPSLATE = makeStateRule(Blocks.DEEPSLATE);
   private static final SurfaceRules.RuleSource DIRT = makeStateRule(Blocks.DIRT);
   private static final SurfaceRules.RuleSource PODZOL = makeStateRule(Blocks.PODZOL);
   private static final SurfaceRules.RuleSource COARSE_DIRT = makeStateRule(Blocks.COARSE_DIRT);
   private static final SurfaceRules.RuleSource MYCELIUM = makeStateRule(Blocks.MYCELIUM);
   private static final SurfaceRules.RuleSource GRASS_BLOCK = makeStateRule(Blocks.GRASS_BLOCK);
   private static final SurfaceRules.RuleSource CALCITE = makeStateRule(Blocks.CALCITE);
   private static final SurfaceRules.RuleSource GRAVEL = makeStateRule(Blocks.GRAVEL);
   private static final SurfaceRules.RuleSource SAND = makeStateRule(Blocks.SAND);
   private static final SurfaceRules.RuleSource SANDSTONE = makeStateRule(Blocks.SANDSTONE);
   private static final SurfaceRules.RuleSource PACKED_ICE = makeStateRule(Blocks.PACKED_ICE);
   private static final SurfaceRules.RuleSource SNOW_BLOCK = makeStateRule(Blocks.SNOW_BLOCK);
   private static final SurfaceRules.RuleSource MUD = makeStateRule(Blocks.MUD);
   private static final SurfaceRules.RuleSource POWDER_SNOW = makeStateRule(Blocks.POWDER_SNOW);
   private static final SurfaceRules.RuleSource ICE = makeStateRule(Blocks.ICE);
   private static final SurfaceRules.RuleSource WATER = makeStateRule(Blocks.WATER);
   private static final SurfaceRules.RuleSource CINNABAR = makeStateRule(Blocks.CINNABAR);
   private static final SurfaceRules.RuleSource SULFUR = makeStateRule(Blocks.SULFUR);

   private static ResourceKey<SurfaceRules.RuleSource> createKey(final String name) {
      return ResourceKey.create(Registries.MATERIAL_RULE, Identifier.withDefaultNamespace(name));
   }

   private static SurfaceRules.RuleSource makeStateRule(final Block block) {
      return SurfaceRules.state(block.defaultBlockState());
   }

   public static void bootstrap(final BootstrapContext<SurfaceRules.RuleSource> context) {
      HolderGetter<SurfaceRules.RuleSource> rules = context.lookup(Registries.MATERIAL_RULE);
      HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
      SurfaceRules.RuleSource sulfurCaveBands = SurfaceRules.registerAndWrap(
         context,
         SULFUR_CAVE_BANDS,
         SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition3d(Noises.SULFUR_CAVE_GRADIENT, -0.4F, -0.1F), CINNABAR),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition3d(Noises.SULFUR_CAVE_GRADIENT, 0.0, 0.4F), SULFUR),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition3d(Noises.SULFUR_CAVE_GRADIENT, 0.4F), CINNABAR)
         )
      );
      SurfaceRules.RuleSource underground = SurfaceRules.registerAndWrap(
         context,
         UNDERGROUND,
         SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, Biomes.SULFUR_CAVES), sulfurCaveBands),
            SurfaceRules.ifTrue(SurfaceRules.verticalGradient("deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(8)), DEEPSLATE)
         )
      );
      SurfaceRules.RuleSource surface = registerSurface(context, sulfurCaveBands);
      context.register(OVERWORLD, createOverworldLike(rules, true, false, true, surface, underground));
      context.register(OVERWORLD_CAVES, createOverworldLike(rules, false, true, true, surface, underground));
      context.register(OVERWORLD_FLOATING_ISLANDS, createOverworldLike(rules, false, false, false, surface, underground));
   }

   private static SurfaceRules.RuleSource registerSurface(
      final BootstrapContext<SurfaceRules.RuleSource> context, final SurfaceRules.RuleSource sulfurCaveBands
   ) {
      HolderGetter<SurfaceRules.ConditionSource> conditions = context.lookup(Registries.MATERIAL_CONDITION);
      HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
      SurfaceRules.ConditionSource woodedBadlandsTop = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(97), 2);
      SurfaceRules.ConditionSource badlandsTop = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(256), 0);
      SurfaceRules.ConditionSource badlandsHeightCondition = SurfaceRules.yStartCheck(VerticalAnchor.absolute(63), -1);
      SurfaceRules.ConditionSource badlandsMid = SurfaceRules.yStartCheck(VerticalAnchor.absolute(74), 1);
      SurfaceRules.ConditionSource mangroveSwampPuddleLevel = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(60), 0);
      SurfaceRules.ConditionSource swampPuddleLevel = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(62), 0);
      SurfaceRules.ConditionSource aboveOverworldSeaLevel = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(63), 0);
      SurfaceRules.ConditionSource onFloor = SurfaceRules.getCondition(conditions, VanillaMaterialConditions.ON_FLOOR);
      SurfaceRules.ConditionSource onCeiling = SurfaceRules.getCondition(conditions, VanillaMaterialConditions.ON_CEILING);
      SurfaceRules.ConditionSource underFloor = SurfaceRules.getCondition(conditions, VanillaMaterialConditions.UNDER_FLOOR);
      SurfaceRules.ConditionSource notUnderwater = SurfaceRules.getCondition(conditions, VanillaMaterialConditions.NOT_UNDERWATER);
      SurfaceRules.ConditionSource notUnderDeepWater = SurfaceRules.getCondition(conditions, VanillaMaterialConditions.NOT_UNDER_DEEP_WATER);
      SurfaceRules.ConditionSource hole = SurfaceRules.hole();
      SurfaceRules.ConditionSource frozenOcean = SurfaceRules.isBiome(biomes, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN);
      SurfaceRules.ConditionSource steep = SurfaceRules.steep();
      SurfaceRules.RuleSource defaultBiomeSurface = SurfaceRules.registerAndWrap(
         context, DEFAULT_BIOME_SURFACE, SurfaceRules.sequence(SurfaceRules.ifTrue(notUnderwater, GRASS_BLOCK), DIRT)
      );
      SurfaceRules.RuleSource sandOrSandstoneIfCeiling = SurfaceRules.registerAndWrap(
         context, SAND_OR_SANDSTONE_IF_CEILING, SurfaceRules.sequence(SurfaceRules.ifTrue(onCeiling, SANDSTONE), SAND)
      );
      SurfaceRules.RuleSource gravelOrStoneIfCeiling = SurfaceRules.registerAndWrap(
         context, GRAVEL_OR_STONE_IF_CEILING, SurfaceRules.sequence(SurfaceRules.ifTrue(onCeiling, STONE), GRAVEL)
      );
      SurfaceRules.ConditionSource biomesWithSandAndSandstone = SurfaceRules.isBiome(biomes, Biomes.WARM_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH);
      SurfaceRules.ConditionSource biomesWithSandAndVeryDeepSandstone = SurfaceRules.isBiome(biomes, Biomes.DESERT);
      SurfaceRules.RuleSource commonSurfaceAndUnderRules = SurfaceRules.sequence(
         SurfaceRules.ifTrue(
            SurfaceRules.isBiome(biomes, Biomes.STONY_PEAKS),
            SurfaceRules.registerAndWrap(
               context,
               STONY_PEAKS_SURFACE,
               SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.CALCITE, -0.0125, 0.0125), CALCITE), STONE)
            )
         ),
         SurfaceRules.ifTrue(
            SurfaceRules.isBiome(biomes, Biomes.STONY_SHORE),
            SurfaceRules.registerAndWrap(
               context,
               STONY_SHORE_SURFACE,
               SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.GRAVEL, -0.05, 0.05), gravelOrStoneIfCeiling), STONE)
            )
         ),
         SurfaceRules.ifTrue(
            SurfaceRules.isBiome(biomes, Biomes.WINDSWEPT_HILLS),
            SurfaceRules.registerAndWrap(context, WINDSWEPT_HILLS_SURFACE, SurfaceRules.ifTrue(surfaceNoiseAbove(1.0), STONE))
         ),
         SurfaceRules.ifTrue(biomesWithSandAndSandstone, sandOrSandstoneIfCeiling),
         SurfaceRules.ifTrue(biomesWithSandAndVeryDeepSandstone, sandOrSandstoneIfCeiling),
         SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, Biomes.DRIPSTONE_CAVES), SurfaceRules.registerAndWrap(context, DRIPSTONE_CAVES_SURFACE, STONE)),
         SurfaceRules.ifTrue(
            SurfaceRules.isBiome(biomes, Biomes.SULFUR_CAVES),
            SurfaceRules.registerAndWrap(context, SULFUR_CAVES_SURFACE, SurfaceRules.sequence(sulfurCaveBands, STONE))
         ),
         SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, Biomes.MANGROVE_SWAMP), SurfaceRules.registerAndWrap(context, MANGROVE_SWAMP_SURFACE, MUD))
      );
      SurfaceRules.RuleSource powderSnowUnderRule = SurfaceRules.registerAndWrap(
         context,
         POWDER_SNOW_UNDER_SURFACE,
         SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.POWDER_SNOW, 0.45, 0.58), SurfaceRules.ifTrue(notUnderwater, POWDER_SNOW))
      );
      SurfaceRules.RuleSource powderSnowSurfaceRule = SurfaceRules.registerAndWrap(
         context,
         POWDER_SNOW_SURFACE,
         SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.POWDER_SNOW, 0.35, 0.6), SurfaceRules.ifTrue(notUnderwater, POWDER_SNOW))
      );
      SurfaceRules.RuleSource underBiomeSurfaceRule = SurfaceRules.registerAndWrap(
         context,
         UNDER_BIOME_SURFACE,
         SurfaceRules.sequence(
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.FROZEN_PEAKS),
               SurfaceRules.registerAndWrap(
                  context,
                  FROZEN_PEAKS_UNDER_SURFACE,
                  SurfaceRules.sequence(
                     SurfaceRules.ifTrue(steep, PACKED_ICE),
                     SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.PACKED_ICE, -0.5, 0.2), PACKED_ICE),
                     SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.ICE, -0.0625, 0.025), ICE),
                     SurfaceRules.ifTrue(notUnderwater, SNOW_BLOCK)
                  )
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.SNOWY_SLOPES),
               SurfaceRules.registerAndWrap(
                  context,
                  SNOWY_SLOPES_UNDER_SURFACE,
                  SurfaceRules.sequence(SurfaceRules.ifTrue(steep, STONE), powderSnowUnderRule, SurfaceRules.ifTrue(notUnderwater, SNOW_BLOCK))
               )
            ),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, Biomes.JAGGED_PEAKS), SurfaceRules.registerAndWrap(context, JAGGED_PEAKS_UNDER_SURFACE, STONE)),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.GROVE),
               SurfaceRules.registerAndWrap(context, GROVE_UNDER_SURFACE, SurfaceRules.sequence(powderSnowUnderRule, DIRT))
            ),
            commonSurfaceAndUnderRules,
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.WINDSWEPT_SAVANNA),
               SurfaceRules.registerAndWrap(context, WINDSWEPT_SAVANNA_UNDER_SURFACE, SurfaceRules.ifTrue(surfaceNoiseAbove(1.75), STONE))
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.WINDSWEPT_GRAVELLY_HILLS),
               SurfaceRules.registerAndWrap(
                  context,
                  WINDSWEPT_GRAVELLY_HILLS_UNDER_SURFACE,
                  SurfaceRules.sequence(
                     SurfaceRules.ifTrue(surfaceNoiseAbove(2.0), gravelOrStoneIfCeiling),
                     SurfaceRules.ifTrue(surfaceNoiseAbove(1.0), STONE),
                     SurfaceRules.ifTrue(surfaceNoiseAbove(-1.0), DIRT),
                     gravelOrStoneIfCeiling
                  )
               )
            ),
            SurfaceRules.registerAndWrap(context, DEFAULT_UNDER_BIOME_SURFACE, DIRT)
         )
      );
      SurfaceRules.RuleSource biomeSurfaceRule = SurfaceRules.registerAndWrap(
         context,
         BIOME_SURFACE,
         SurfaceRules.sequence(
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.FROZEN_PEAKS),
               SurfaceRules.registerAndWrap(
                  context,
                  FROZEN_PEAKS_SURFACE,
                  SurfaceRules.sequence(
                     SurfaceRules.ifTrue(steep, PACKED_ICE),
                     SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.PACKED_ICE, 0.0, 0.2), PACKED_ICE),
                     SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.ICE, 0.0, 0.025), ICE),
                     SurfaceRules.ifTrue(notUnderwater, SNOW_BLOCK)
                  )
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.SNOWY_SLOPES),
               SurfaceRules.registerAndWrap(
                  context,
                  SNOWY_SLOPES_SURFACE,
                  SurfaceRules.sequence(SurfaceRules.ifTrue(steep, STONE), powderSnowSurfaceRule, SurfaceRules.ifTrue(notUnderwater, SNOW_BLOCK))
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.JAGGED_PEAKS),
               SurfaceRules.registerAndWrap(
                  context, JAGGED_PEAKS_SURFACE, SurfaceRules.sequence(SurfaceRules.ifTrue(steep, STONE), SurfaceRules.ifTrue(notUnderwater, SNOW_BLOCK))
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.GROVE),
               SurfaceRules.registerAndWrap(
                  context, GROVE_SURFACE, SurfaceRules.sequence(powderSnowSurfaceRule, SurfaceRules.ifTrue(notUnderwater, SNOW_BLOCK))
               )
            ),
            commonSurfaceAndUnderRules,
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.WINDSWEPT_SAVANNA),
               SurfaceRules.registerAndWrap(
                  context,
                  WINDSWEPT_SAVANNA_SURFACE,
                  SurfaceRules.sequence(SurfaceRules.ifTrue(surfaceNoiseAbove(1.75), STONE), SurfaceRules.ifTrue(surfaceNoiseAbove(-0.5), COARSE_DIRT))
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.WINDSWEPT_GRAVELLY_HILLS),
               SurfaceRules.registerAndWrap(
                  context,
                  WINDSWEPT_GRAVELLY_HILLS_SURFACE,
                  SurfaceRules.sequence(
                     SurfaceRules.ifTrue(surfaceNoiseAbove(2.0), gravelOrStoneIfCeiling),
                     SurfaceRules.ifTrue(surfaceNoiseAbove(1.0), STONE),
                     SurfaceRules.ifTrue(surfaceNoiseAbove(-1.0), defaultBiomeSurface),
                     gravelOrStoneIfCeiling
                  )
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
               SurfaceRules.registerAndWrap(
                  context,
                  OLD_GROWTH_PINE_TAIGA_SURFACE,
                  SurfaceRules.sequence(SurfaceRules.ifTrue(surfaceNoiseAbove(1.75), COARSE_DIRT), SurfaceRules.ifTrue(surfaceNoiseAbove(-0.95), PODZOL))
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.ICE_SPIKES),
               SurfaceRules.registerAndWrap(context, ICE_SPIKES_SURFACE, SurfaceRules.ifTrue(notUnderwater, SNOW_BLOCK))
            ),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, Biomes.MUSHROOM_FIELDS), SurfaceRules.registerAndWrap(context, MUSHROOM_FIELDS_SURFACE, MYCELIUM)),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.DAPPLED_FOREST),
               SurfaceRules.registerAndWrap(
                  context, DAPPLED_FOREST_SURFACE, SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SMALL_PATCH, 1.2F), COARSE_DIRT)
               )
            ),
            defaultBiomeSurface
         )
      );
      SurfaceRules.ConditionSource clayBand1 = SurfaceRules.noiseCondition2d(Noises.SURFACE, -0.909, -0.5454);
      SurfaceRules.ConditionSource clayBand2 = SurfaceRules.noiseCondition2d(Noises.SURFACE, -0.1818, 0.1818);
      SurfaceRules.ConditionSource clayBand3 = SurfaceRules.noiseCondition2d(Noises.SURFACE, 0.5454, 0.909);
      return SurfaceRules.registerAndWrap(
         context,
         SURFACE,
         SurfaceRules.sequence(
            SurfaceRules.ifTrue(
               onFloor,
               SurfaceRules.sequence(
                  SurfaceRules.ifTrue(
                     SurfaceRules.isBiome(biomes, Biomes.WOODED_BADLANDS),
                     SurfaceRules.ifTrue(
                        woodedBadlandsTop,
                        SurfaceRules.sequence(
                           SurfaceRules.ifTrue(clayBand1, COARSE_DIRT),
                           SurfaceRules.ifTrue(clayBand2, COARSE_DIRT),
                           SurfaceRules.ifTrue(clayBand3, COARSE_DIRT),
                           defaultBiomeSurface
                        )
                     )
                  ),
                  SurfaceRules.ifTrue(
                     SurfaceRules.isBiome(biomes, Biomes.SWAMP),
                     SurfaceRules.ifTrue(
                        swampPuddleLevel,
                        SurfaceRules.ifTrue(
                           SurfaceRules.not(aboveOverworldSeaLevel), SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SWAMP, 0.0), WATER)
                        )
                     )
                  ),
                  SurfaceRules.ifTrue(
                     SurfaceRules.isBiome(biomes, Biomes.MANGROVE_SWAMP),
                     SurfaceRules.ifTrue(
                        mangroveSwampPuddleLevel,
                        SurfaceRules.ifTrue(
                           SurfaceRules.not(aboveOverworldSeaLevel), SurfaceRules.ifTrue(SurfaceRules.noiseCondition2d(Noises.SWAMP, 0.0), WATER)
                        )
                     )
                  )
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS),
               SurfaceRules.sequence(
                  SurfaceRules.ifTrue(
                     onFloor,
                     SurfaceRules.sequence(
                        SurfaceRules.ifTrue(badlandsTop, ORANGE_TERRACOTTA),
                        SurfaceRules.ifTrue(
                           badlandsMid,
                           SurfaceRules.sequence(
                              SurfaceRules.ifTrue(clayBand1, TERRACOTTA),
                              SurfaceRules.ifTrue(clayBand2, TERRACOTTA),
                              SurfaceRules.ifTrue(clayBand3, TERRACOTTA),
                              SurfaceRules.bandlands()
                           )
                        ),
                        SurfaceRules.ifTrue(notUnderwater, SurfaceRules.sequence(SurfaceRules.ifTrue(onCeiling, RED_SANDSTONE), RED_SAND)),
                        SurfaceRules.ifTrue(SurfaceRules.not(hole), ORANGE_TERRACOTTA),
                        SurfaceRules.ifTrue(notUnderDeepWater, WHITE_TERRACOTTA),
                        gravelOrStoneIfCeiling
                     )
                  ),
                  SurfaceRules.ifTrue(
                     badlandsHeightCondition,
                     SurfaceRules.sequence(
                        SurfaceRules.ifTrue(aboveOverworldSeaLevel, SurfaceRules.ifTrue(SurfaceRules.not(badlandsMid), ORANGE_TERRACOTTA)),
                        SurfaceRules.bandlands()
                     )
                  ),
                  SurfaceRules.ifTrue(underFloor, SurfaceRules.ifTrue(notUnderDeepWater, WHITE_TERRACOTTA))
               )
            ),
            SurfaceRules.ifTrue(
               onFloor,
               SurfaceRules.ifTrue(notUnderwater, SurfaceRules.sequence(SurfaceRules.ifTrue(frozenOcean, SurfaceRules.ifTrue(hole, AIR)), biomeSurfaceRule))
            ),
            SurfaceRules.ifTrue(
               notUnderDeepWater,
               SurfaceRules.sequence(
                  SurfaceRules.ifTrue(onFloor, SurfaceRules.ifTrue(frozenOcean, SurfaceRules.ifTrue(hole, WATER))),
                  SurfaceRules.ifTrue(underFloor, underBiomeSurfaceRule),
                  SurfaceRules.ifTrue(
                     biomesWithSandAndSandstone,
                     SurfaceRules.ifTrue(SurfaceRules.getCondition(conditions, VanillaMaterialConditions.DEEP_UNDER_FLOOR), SANDSTONE)
                  ),
                  SurfaceRules.ifTrue(
                     biomesWithSandAndVeryDeepSandstone,
                     SurfaceRules.ifTrue(SurfaceRules.getCondition(conditions, VanillaMaterialConditions.VERY_DEEP_UNDER_FLOOR), SANDSTONE)
                  )
               )
            ),
            SurfaceRules.ifTrue(
               onFloor,
               SurfaceRules.sequence(
                  SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS), STONE),
                  SurfaceRules.ifTrue(
                     SurfaceRules.isBiome(biomes, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN), sandOrSandstoneIfCeiling
                  ),
                  gravelOrStoneIfCeiling
               )
            )
         )
      );
   }

   private static SurfaceRules.RuleSource createOverworldLike(
      final HolderGetter<SurfaceRules.RuleSource> rules,
      final boolean doPreliminarySurfaceCheck,
      final boolean bedrockRoof,
      final boolean bedrockFloor,
      final SurfaceRules.RuleSource mainRuleCloseToSurface,
      final SurfaceRules.RuleSource underground
   ) {
      Builder<SurfaceRules.RuleSource> builder = ImmutableList.builder();
      if (bedrockRoof) {
         builder.add(SurfaceRules.getRule(rules, VanillaMaterialRules.BEDROCK_ROOF));
      }

      if (bedrockFloor) {
         builder.add(SurfaceRules.getRule(rules, VanillaMaterialRules.BEDROCK_FLOOR));
      }

      SurfaceRules.RuleSource ruleAbovePreliminarySurface = SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), mainRuleCloseToSurface);
      builder.add(doPreliminarySurfaceCheck ? ruleAbovePreliminarySurface : mainRuleCloseToSurface);
      builder.add(underground);
      return SurfaceRules.sequence((SurfaceRules.RuleSource[])builder.build().toArray(SurfaceRules.RuleSource[]::new));
   }

   private static SurfaceRules.ConditionSource surfaceNoiseAbove(final double threshold) {
      return SurfaceRules.noiseCondition2d(Noises.SURFACE, threshold / 8.25, Double.MAX_VALUE);
   }
}
