package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

public final class FluidTags {
   public static final TagKey<Fluid> WATER = create("water");
   public static final TagKey<Fluid> LAVA = create("lava");
   public static final TagKey<Fluid> SUPPORTS_SUGAR_CANE_ADJACENTLY = create("supports_sugar_cane_adjacently");
   public static final TagKey<Fluid> SUPPORTS_LILY_PAD = create("supports_lily_pad");
   public static final TagKey<Fluid> SUPPORTS_FROGSPAWN = create("supports_frogspawn");
   public static final TagKey<Fluid> BUBBLE_COLUMN_CAN_OCCUPY = create("bubble_column_can_occupy");

   private FluidTags() {
   }

   private static TagKey<Fluid> create(final String name) {
      return TagKey.create(Registries.FLUID, Identifier.withDefaultNamespace(name));
   }
}
