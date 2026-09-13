package net.minecraft.data.worldgen.material;

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

public class NetherMaterialRules {
   public static final ResourceKey<SurfaceRules.RuleSource> NETHER = createKey("nether");
   private static final SurfaceRules.RuleSource LAVA = makeStateRule(Blocks.LAVA);
   private static final SurfaceRules.RuleSource NETHERRACK = makeStateRule(Blocks.NETHERRACK);
   private static final SurfaceRules.RuleSource SOUL_SAND = makeStateRule(Blocks.SOUL_SAND);
   private static final SurfaceRules.RuleSource SOUL_SOIL = makeStateRule(Blocks.SOUL_SOIL);
   private static final SurfaceRules.RuleSource BASALT = makeStateRule(Blocks.BASALT);
   private static final SurfaceRules.RuleSource BLACKSTONE = makeStateRule(Blocks.BLACKSTONE);
   private static final SurfaceRules.RuleSource WARPED_WART_BLOCK = makeStateRule(Blocks.WARPED_WART_BLOCK);
   private static final SurfaceRules.RuleSource WARPED_NYLIUM = makeStateRule(Blocks.WARPED_NYLIUM);
   private static final SurfaceRules.RuleSource NETHER_WART_BLOCK = makeStateRule(Blocks.NETHER_WART_BLOCK);
   private static final SurfaceRules.RuleSource CRIMSON_NYLIUM = makeStateRule(Blocks.CRIMSON_NYLIUM);
   private static final SurfaceRules.RuleSource GRAVEL = makeStateRule(Blocks.GRAVEL);

   private static ResourceKey<SurfaceRules.RuleSource> createKey(final String name) {
      return ResourceKey.create(Registries.MATERIAL_RULE, Identifier.withDefaultNamespace(name));
   }

   private static SurfaceRules.RuleSource makeStateRule(final Block block) {
      return SurfaceRules.state(block.defaultBlockState());
   }

   public static void bootstrap(final BootstrapContext<SurfaceRules.RuleSource> context) {
      HolderGetter<SurfaceRules.RuleSource> rules = context.lookup(Registries.MATERIAL_RULE);
      HolderGetter<SurfaceRules.ConditionSource> conditions = context.lookup(Registries.MATERIAL_CONDITION);
      HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
      SurfaceRules.ConditionSource onFloor = SurfaceRules.getCondition(conditions, VanillaMaterialConditions.ON_FLOOR);
      SurfaceRules.ConditionSource underCeiling = SurfaceRules.getCondition(conditions, VanillaMaterialConditions.UNDER_CEILING);
      SurfaceRules.ConditionSource underFloor = SurfaceRules.getCondition(conditions, VanillaMaterialConditions.UNDER_FLOOR);
      SurfaceRules.ConditionSource aboveNetherLavaLevel = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(31), 0);
      SurfaceRules.ConditionSource aboveNetherLavaSurface = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(32), 0);
      SurfaceRules.ConditionSource netherBandAroundLavaLevelBottom = SurfaceRules.yStartCheck(VerticalAnchor.absolute(30), 0);
      SurfaceRules.ConditionSource netherBandAroundLavaLevelTop = SurfaceRules.not(SurfaceRules.yStartCheck(VerticalAnchor.absolute(35), 0));
      SurfaceRules.ConditionSource closeToCeiling = SurfaceRules.yBlockCheck(VerticalAnchor.belowTop(5), 0);
      SurfaceRules.ConditionSource hole = SurfaceRules.hole();
      SurfaceRules.ConditionSource soulSandLayer = SurfaceRules.noiseCondition2d(Noises.SOUL_SAND_LAYER, -0.012);
      SurfaceRules.ConditionSource gravelLayer = SurfaceRules.noiseCondition2d(Noises.GRAVEL_LAYER, -0.012);
      SurfaceRules.ConditionSource patch = SurfaceRules.noiseCondition2d(Noises.PATCH, -0.012);
      SurfaceRules.ConditionSource netherrack = SurfaceRules.noiseCondition2d(Noises.NETHERRACK, 0.54);
      SurfaceRules.ConditionSource netherWart = SurfaceRules.noiseCondition2d(Noises.NETHER_WART, 1.17);
      SurfaceRules.ConditionSource netherStateSelector = SurfaceRules.noiseCondition2d(Noises.NETHER_STATE_SELECTOR, 0.0);
      SurfaceRules.RuleSource gravelPatch = SurfaceRules.ifTrue(
         patch, SurfaceRules.ifTrue(netherBandAroundLavaLevelBottom, SurfaceRules.ifTrue(netherBandAroundLavaLevelTop, GRAVEL))
      );
      context.register(
         NETHER,
         SurfaceRules.sequence(
            SurfaceRules.getRule(rules, VanillaMaterialRules.BEDROCK_FLOOR),
            SurfaceRules.getRule(rules, VanillaMaterialRules.BEDROCK_ROOF),
            SurfaceRules.ifTrue(closeToCeiling, NETHERRACK),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.BASALT_DELTAS),
               SurfaceRules.sequence(
                  SurfaceRules.ifTrue(underCeiling, BASALT),
                  SurfaceRules.ifTrue(underFloor, SurfaceRules.sequence(gravelPatch, SurfaceRules.ifTrue(netherStateSelector, BASALT), BLACKSTONE))
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.SOUL_SAND_VALLEY),
               SurfaceRules.sequence(
                  SurfaceRules.ifTrue(underCeiling, SurfaceRules.sequence(SurfaceRules.ifTrue(netherStateSelector, SOUL_SAND), SOUL_SOIL)),
                  SurfaceRules.ifTrue(underFloor, SurfaceRules.sequence(gravelPatch, SurfaceRules.ifTrue(netherStateSelector, SOUL_SAND), SOUL_SOIL))
               )
            ),
            SurfaceRules.ifTrue(
               onFloor,
               SurfaceRules.sequence(
                  SurfaceRules.ifTrue(SurfaceRules.not(aboveNetherLavaSurface), SurfaceRules.ifTrue(hole, LAVA)),
                  SurfaceRules.ifTrue(
                     SurfaceRules.isBiome(biomes, Biomes.WARPED_FOREST),
                     SurfaceRules.ifTrue(
                        SurfaceRules.not(netherrack),
                        SurfaceRules.ifTrue(aboveNetherLavaLevel, SurfaceRules.sequence(SurfaceRules.ifTrue(netherWart, WARPED_WART_BLOCK), WARPED_NYLIUM))
                     )
                  ),
                  SurfaceRules.ifTrue(
                     SurfaceRules.isBiome(biomes, Biomes.CRIMSON_FOREST),
                     SurfaceRules.ifTrue(
                        SurfaceRules.not(netherrack),
                        SurfaceRules.ifTrue(aboveNetherLavaLevel, SurfaceRules.sequence(SurfaceRules.ifTrue(netherWart, NETHER_WART_BLOCK), CRIMSON_NYLIUM))
                     )
                  )
               )
            ),
            SurfaceRules.ifTrue(
               SurfaceRules.isBiome(biomes, Biomes.NETHER_WASTES),
               SurfaceRules.sequence(
                  SurfaceRules.ifTrue(
                     underFloor,
                     SurfaceRules.ifTrue(
                        soulSandLayer,
                        SurfaceRules.sequence(
                           SurfaceRules.ifTrue(
                              SurfaceRules.not(hole),
                              SurfaceRules.ifTrue(netherBandAroundLavaLevelBottom, SurfaceRules.ifTrue(netherBandAroundLavaLevelTop, SOUL_SAND))
                           ),
                           NETHERRACK
                        )
                     )
                  ),
                  SurfaceRules.ifTrue(
                     onFloor,
                     SurfaceRules.ifTrue(
                        aboveNetherLavaLevel,
                        SurfaceRules.ifTrue(
                           netherBandAroundLavaLevelTop,
                           SurfaceRules.ifTrue(
                              gravelLayer,
                              SurfaceRules.sequence(SurfaceRules.ifTrue(aboveNetherLavaSurface, GRAVEL), SurfaceRules.ifTrue(SurfaceRules.not(hole), GRAVEL))
                           )
                        )
                     )
                  )
               )
            ),
            NETHERRACK
         )
      );
   }
}
