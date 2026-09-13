package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class SpawnConditions {
   public static MapCodec<? extends SpawnCondition> bootstrap(final Registry<MapCodec<? extends SpawnCondition>> registry) {
      Registry.register(registry, "structure", StructureCheck.MAP_CODEC);
      Registry.register(registry, "moon_brightness", MoonBrightnessCheck.MAP_CODEC);
      return Registry.register(registry, "biome", BiomeCheck.MAP_CODEC);
   }
}
