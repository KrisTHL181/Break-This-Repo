package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

public final class FluidTags {
   public static final TagKey<Fluid> WATER = create("water");
   public static final TagKey<Fluid> LAVA = create("lava");

   private FluidTags() {
   }

   private static TagKey<Fluid> create(String p_203851_) {
      return TagKey.create(Registries.FLUID, Identifier.withDefaultNamespace(p_203851_));
   }
}
