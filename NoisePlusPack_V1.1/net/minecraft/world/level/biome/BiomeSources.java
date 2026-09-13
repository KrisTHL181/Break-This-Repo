package net.minecraft.world.level.biome;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class BiomeSources {
   public static MapCodec<? extends BiomeSource> bootstrap(Registry<MapCodec<? extends BiomeSource>> p_220587_) {
      Registry.register(p_220587_, "fixed", FixedBiomeSource.CODEC);
      Registry.register(p_220587_, "multi_noise", MultiNoiseBiomeSource.CODEC);
      Registry.register(p_220587_, "checkerboard", CheckerboardColumnBiomeSource.CODEC);
      return Registry.register(p_220587_, "the_end", TheEndBiomeSource.CODEC);
   }
}
