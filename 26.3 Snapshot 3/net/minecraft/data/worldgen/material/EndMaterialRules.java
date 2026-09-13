package net.minecraft.data.worldgen.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

public class EndMaterialRules {
   public static final ResourceKey<SurfaceRules.RuleSource> END = createKey("end");

   private static ResourceKey<SurfaceRules.RuleSource> createKey(final String name) {
      return ResourceKey.create(Registries.MATERIAL_RULE, Identifier.withDefaultNamespace(name));
   }

   public static void bootstrap(final BootstrapContext<SurfaceRules.RuleSource> context) {
      context.register(END, SurfaceRules.state(Blocks.END_STONE.defaultBlockState()));
   }
}
