package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class SpawnConditions {
   public static MapCodec<? extends SpawnCondition> bootstrap(Registry<MapCodec<? extends SpawnCondition>> p_396755_) {
      Registry.register(p_396755_, "structure", StructureCheck.MAP_CODEC);
      Registry.register(p_396755_, "moon_brightness", MoonBrightnessCheck.MAP_CODEC);
      return Registry.register(p_396755_, "biome", BiomeCheck.MAP_CODEC);
   }
}
