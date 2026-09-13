package net.minecraft.data.worldgen.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public class VanillaMaterialRules {
   public static final ResourceKey<SurfaceRules.RuleSource> BEDROCK_FLOOR = createKey("bedrock_floor");
   public static final ResourceKey<SurfaceRules.RuleSource> BEDROCK_ROOF = createKey("bedrock_roof");
   private static final SurfaceRules.RuleSource BEDROCK = makeStateRule(Blocks.BEDROCK);

   private static ResourceKey<SurfaceRules.RuleSource> createKey(final String name) {
      return ResourceKey.create(Registries.MATERIAL_RULE, Identifier.withDefaultNamespace(name));
   }

   private static SurfaceRules.RuleSource makeStateRule(final Block block) {
      return SurfaceRules.state(block.defaultBlockState());
   }

   public static void bootstrap(final BootstrapContext<SurfaceRules.RuleSource> context) {
      context.register(
         BEDROCK_FLOOR, SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK)
      );
      context.register(
         BEDROCK_ROOF,
         SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.verticalGradient("bedrock_roof", VerticalAnchor.belowTop(5), VerticalAnchor.top())), BEDROCK)
      );
      OverworldMaterialRules.bootstrap(context);
      NetherMaterialRules.bootstrap(context);
      EndMaterialRules.bootstrap(context);
   }

   public static SurfaceRules.RuleSource air() {
      return SurfaceRules.state(Blocks.AIR.defaultBlockState());
   }
}
