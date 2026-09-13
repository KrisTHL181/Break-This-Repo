package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class NonOverlappingMerger extends AbstractDoubleList implements IndexMerger {
   private final DoubleList lower;
   private final DoubleList upper;
   private final boolean swap;

   protected NonOverlappingMerger(final DoubleList lower, final DoubleList upper, final boolean swap) {
      this.lower = lower;
      this.upper = upper;
      this.swap = swap;
   }

   @Override
   public int size() {
      return this.lower.size() + this.upper.size();
   }

   @Override
   public boolean forMergedIndexes(final IndexMerger.IndexConsumer consumer) {
      return this.swap
         ? this.forNonSwappedIndexes((firstIndex, secondIndex, resultIndex) -> consumer.merge(secondIndex, firstIndex, resultIndex))
         : this.forNonSwappedIndexes(consumer);
   }

   private boolean forNonSwappedIndexes(final IndexMerger.IndexConsumer consumer) {
      int lowerSize = this.lower.size();

      for (int i = 0; i < lowerSize; i++) {
         if (!consumer.merge(i, -1, i)) {
            return false;
         }
      }

      int upperSize = this.upper.size() - 1;

      for (int i = 0; i < upperSize; i++) {
         if (!consumer.merge(lowerSize - 1, i, lowerSize + i)) {
            return false;
         }
      }

      return true;
   }

   public double getDouble(final int index) {
      return index < this.lower.size() ? this.lower.getDouble(index) : this.upper.getDouble(index - this.lower.size());
   }

   @Override
   public DoubleList getList() {
      return this;
   }
}
