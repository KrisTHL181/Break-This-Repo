package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import org.jspecify.annotations.Nullable;

public class StructurePiecesBuilder implements StructurePieceAccessor {
   private final List<StructurePiece> pieces = Lists.newArrayList();

   @Override
   public void addPiece(final StructurePiece piece) {
      this.pieces.add(piece);
   }

   @Override
   public @Nullable StructurePiece findCollisionPiece(final BoundingBox box) {
      return StructurePiece.findCollisionPiece(this.pieces, box);
   }

   @Deprecated
   public void offsetPiecesVertically(final int dy) {
      for (StructurePiece piece : this.pieces) {
         piece.move(0, dy, 0);
      }
   }

   @Deprecated
   public int moveBelowSeaLevel(final int seaLevel, final int minY, final RandomSource random, final int offset) {
      int maxY = seaLevel - offset;
      BoundingBox boundingBox = this.getBoundingBox();
      int y1Pos = boundingBox.getYSpan() + minY + 1;
      if (y1Pos < maxY) {
         y1Pos += random.nextInt(maxY - y1Pos);
      }

      int dy = y1Pos - boundingBox.maxY();
      this.offsetPiecesVertically(dy);
      return dy;
   }

   public void moveInsideHeights(final RandomSource random, final int lowestAllowed, final int highestAllowed) {
      BoundingBox boundingBox = this.getBoundingBox();
      int heightSpan = highestAllowed - lowestAllowed + 1 - boundingBox.getYSpan();
      int y0Pos;
      if (heightSpan > 1) {
         y0Pos = lowestAllowed + random.nextInt(heightSpan);
      } else {
         y0Pos = lowestAllowed;
      }

      int dy = y0Pos - boundingBox.minY();
      this.offsetPiecesVertically(dy);
   }

   public PiecesContainer build() {
      return new PiecesContainer(this.pieces);
   }

   public void clear() {
      this.pieces.clear();
   }

   public boolean isEmpty() {
      return this.pieces.isEmpty();
   }

   public BoundingBox getBoundingBox() {
      return StructurePiece.createBoundingBox(this.pieces.stream());
   }
}
