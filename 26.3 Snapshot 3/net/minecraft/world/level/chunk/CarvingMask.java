package net.minecraft.world.level.chunk;

import java.util.BitSet;

public class CarvingMask implements CarverOutput {
   private final int minY;
   private final int maxY;
   private final int height;
   private final BitSet mask;

   public CarvingMask(final int minY, final int maxY) {
      this.minY = minY;
      this.maxY = maxY;
      this.height = maxY - minY + 1;
      this.mask = new BitSet(256 * this.height);
   }

   private int getIndex(final int x, final int y, final int z) {
      return y - this.minY + (z + (x << 4)) * this.height;
   }

   @Override
   public int minY() {
      return this.minY;
   }

   @Override
   public int maxY() {
      return this.maxY;
   }

   @Override
   public void carve(final int x, final int y, final int z) {
      this.mask.set(this.getIndex(x, y, z));
   }

   public void visit(final CarvingMask.Visitor visitor) {
      int startIndex = this.mask.nextSetBit(0);

      while (startIndex != -1) {
         int endIndex = this.mask.nextClearBit(startIndex) - 1;
         this.visitSegment(visitor, startIndex, endIndex);
         startIndex = this.mask.nextSetBit(endIndex + 1);
      }
   }

   private void visitSegment(final CarvingMask.Visitor visitor, final int startIndex, final int endIndex) {
      int startColumn = startIndex / this.height;
      int endColumn = endIndex / this.height;

      for (int column = startColumn; column <= endColumn; column++) {
         int columnX = column >> 4 & 15;
         int columnZ = column & 15;
         int columnBaseIndex = column * this.height;
         int bottomY = Math.max(startIndex - columnBaseIndex, 0) + this.minY;
         int topY = Math.min(endIndex - columnBaseIndex, this.height - 1) + this.minY;
         visitor.visitColumn(columnX, columnZ, bottomY, topY);
      }
   }

   public boolean isEmpty() {
      return this.mask.isEmpty();
   }

   @FunctionalInterface
   public interface Filter {
      boolean test(int x, int y, int z);
   }

   @FunctionalInterface
   public interface Visitor {
      void visitColumn(int x, int z, int bottomY, int topY);
   }
}
