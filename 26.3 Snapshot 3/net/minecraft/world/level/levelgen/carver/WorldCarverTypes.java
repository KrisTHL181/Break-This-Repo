package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public interface WorldCarverTypes {
   static MapCodec<? extends WorldCarver> bootstrap(final Registry<MapCodec<? extends WorldCarver>> registry) {
      Registry.register(registry, "cave", CaveWorldCarver.MAP_CODEC);
      return Registry.register(registry, "canyon", CanyonWorldCarver.MAP_CODEC);
   }
}
