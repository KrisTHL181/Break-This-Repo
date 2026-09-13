package net.minecraft.world.level.levelgen.structure;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

@FunctionalInterface
public interface PostPlacementProcessor {
   PostPlacementProcessor NONE = (level, structureManager, generator, random, chunkBB, chunkPos, pieces) -> {};

   void afterPlace(
      WorldGenLevel level,
      StructureManager structureManager,
      ChunkGenerator generator,
      RandomSource random,
      BoundingBox chunkBB,
      ChunkPos chunkPos,
      PiecesContainer pieces
   );
}
