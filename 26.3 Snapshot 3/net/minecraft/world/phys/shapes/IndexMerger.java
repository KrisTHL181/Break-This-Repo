package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public interface IndexMerger {
   DoubleList getList();

   boolean forMergedIndexes(IndexMerger.IndexConsumer consumer);

   int size();

   interface IndexConsumer {
      boolean merge(int firstIndex, int secondIndex, int resultIndex);
   }
}
