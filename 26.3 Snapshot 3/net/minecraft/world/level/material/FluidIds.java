package net.minecraft.world.level.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class FluidIds {
   public static final ResourceKey<Fluid> EMPTY = create("empty");
   public static final ResourceKey<Fluid> FLOWING_WATER = create("flowing_water");
   public static final ResourceKey<Fluid> WATER = create("water");
   public static final ResourceKey<Fluid> FLOWING_LAVA = create("flowing_lava");
   public static final ResourceKey<Fluid> LAVA = create("lava");

   private static ResourceKey<Fluid> create(final String name) {
      return ResourceKey.create(Registries.FLUID, Identifier.withDefaultNamespace(name));
   }
}
