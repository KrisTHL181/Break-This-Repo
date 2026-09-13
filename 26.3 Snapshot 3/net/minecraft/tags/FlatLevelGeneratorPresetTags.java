package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;

public class FlatLevelGeneratorPresetTags {
   public static final TagKey<FlatLevelGeneratorPreset> VISIBLE = create("visible");

   private FlatLevelGeneratorPresetTags() {
   }

   private static TagKey<FlatLevelGeneratorPreset> create(final String name) {
      return TagKey.create(Registries.FLAT_LEVEL_GENERATOR_PRESET, Identifier.withDefaultNamespace(name));
   }
}
