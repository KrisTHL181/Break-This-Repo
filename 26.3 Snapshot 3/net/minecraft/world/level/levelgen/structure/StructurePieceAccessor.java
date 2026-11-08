package net.minecraft.world.level.levelgen.structure;

import org.jspecify.annotations.Nullable;

public interface StructurePieceAccessor {
   void addPiece(StructurePiece piece);

   @Nullable StructurePiece findCollisionPiece(BoundingBox box);
}
