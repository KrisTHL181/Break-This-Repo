package net.minecraft.world.level.chunk;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

public class ChunkGenerators {
   public static MapCodec<? extends ChunkGenerator> bootstrap(Registry<MapCodec<? extends ChunkGenerator>> p_223243_) {
      Registry.register(p_223243_, "noise", NoiseBasedChunkGenerator.CODEC);
      Registry.register(p_223243_, "flat", FlatLevelSource.CODEC);
      return Registry.register(p_223243_, "debug", DebugLevelSource.CODEC);
   }
}
