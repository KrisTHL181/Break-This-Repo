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
   public void addPiece(StructurePiece p_192791_) {
      this.pieces.add(p_192791_);
   }

   @Override
   public @Nullable StructurePiece findCollisionPiece(BoundingBox p_192789_) {
      return StructurePiece.findCollisionPiece(this.pieces, p_192789_);
   }

   @Deprecated
   public void offsetPiecesVertically(int p_192782_) {
      for (StructurePiece structurepiece : this.pieces) {
         structurepiece.move(0, p_192782_, 0);
      }
   }

   @Deprecated
   public int moveBelowSeaLevel(int p_226966_, int p_226967_, RandomSource p_226968_, int p_226969_) {
      int i = p_226966_ - p_226969_;
      BoundingBox boundingbox = this.getBoundingBox();
      int j = boundingbox.getYSpan() + p_226967_ + 1;
      if (j < i) {
         j += p_226968_.nextInt(i - j);
      }

      int k = j - boundingbox.maxY();
      this.offsetPiecesVertically(k);
      return k;
   }

   public void moveInsideHeights(RandomSource p_226971_, int p_226972_, int p_226973_) {
      BoundingBox boundingbox = this.getBoundingBox();
      int i = p_226973_ - p_226972_ + 1 - boundingbox.getYSpan();
      int j;
      if (i > 1) {
         j = p_226972_ + p_226971_.nextInt(i);
      } else {
         j = p_226972_;
      }

      int k = j - boundingbox.minY();
      this.offsetPiecesVertically(k);
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
