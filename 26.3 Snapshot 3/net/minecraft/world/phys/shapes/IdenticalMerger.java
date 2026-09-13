package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public class IdenticalMerger implements IndexMerger {
   private final DoubleList coords;

   public IdenticalMerger(final DoubleList coords) {
      this.coords = coords;
   }

   @Override
   public boolean forMergedIndexes(final IndexMerger.IndexConsumer consumer) {
      int size = this.coords.size() - 1;

      for (int i = 0; i < size; i++) {
         if (!consumer.merge(i, i, i)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public int size() {
      return this.coords.size();
   }

   @Override
   public DoubleList getList() {
      return this.coords;
   }
}
