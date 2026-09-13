package net.minecraft.world.level.levelgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public abstract class ScatteredFeaturePiece extends StructurePiece {
   protected final int width;
   protected final int height;
   protected final int depth;
   protected int heightPosition = -1;

   protected ScatteredFeaturePiece(
      final StructurePieceType type,
      final int west,
      final int floor,
      final int north,
      final int width,
      final int height,
      final int depth,
      final Direction direction
   ) {
      super(type, 0, StructurePiece.makeBoundingBox(west, floor, north, direction, width, height, depth));
      this.width = width;
      this.height = height;
      this.depth = depth;
      this.setOrientation(direction);
   }

   protected ScatteredFeaturePiece(final StructurePieceType type, final CompoundTag tag) {
      super(type, tag);
      this.width = tag.getIntOr("Width", 0);
      this.height = tag.getIntOr("Height", 0);
      this.depth = tag.getIntOr("Depth", 0);
      this.heightPosition = tag.getIntOr("HPos", 0);
   }

   @Override
   protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
      tag.putInt("Width", this.width);
      tag.putInt("Height", this.height);
      tag.putInt("Depth", this.depth);
      tag.putInt("HPos", this.heightPosition);
   }

   protected boolean updateAverageGroundHeight(final LevelAccessor level, final BoundingBox chunkBB, final int offset) {
      if (this.heightPosition >= 0) {
         return true;
      }

      int total = 0;
      int count = 0;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); z++) {
         for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); x++) {
            pos.set(x, 64, z);
            if (chunkBB.isInside(pos)) {
               total += level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos).getY();
               count++;
            }
         }
      }

      if (count == 0) {
         return false;
      }

      this.heightPosition = total / count;
      this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + offset, 0);
      return true;
   }

   protected boolean updateHeightPositionToLowestGroundHeight(final LevelAccessor level, final int offset) {
      if (this.heightPosition >= 0) {
         return true;
      }

      int lowestGroundHeight = level.getMaxY() + 1;
      boolean foundPositionWithinBoundingBox = false;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); z++) {
         for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); x++) {
            pos.set(x, 0, z);
            lowestGroundHeight = Math.min(lowestGroundHeight, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos).getY());
            foundPositionWithinBoundingBox = true;
         }
      }

      if (!foundPositionWithinBoundingBox) {
         return false;
      }

      this.heightPosition = lowestGroundHeight;
      this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + offset, 0);
      return true;
   }
}
