package net.minecraft.world.clock;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface WorldClocks {
   ResourceKey<WorldClock> OVERWORLD = key("overworld");
   ResourceKey<WorldClock> THE_END = key("the_end");

   static void bootstrap(final BootstrapContext<WorldClock> context) {
      context.register(OVERWORLD, new WorldClock());
      context.register(THE_END, new WorldClock());
   }

   private static ResourceKey<WorldClock> key(final String id) {
      return ResourceKey.create(Registries.WORLD_CLOCK, Identifier.withDefaultNamespace(id));
   }
}
