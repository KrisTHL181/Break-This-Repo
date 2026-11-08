package net.minecraft.world.level.levelgen.structure;

import org.jspecify.annotations.Nullable;

public interface StructurePieceAccessor {
   void addPiece(StructurePiece var1);

   @Nullable StructurePiece findCollisionPiece(BoundingBox var1);
}
