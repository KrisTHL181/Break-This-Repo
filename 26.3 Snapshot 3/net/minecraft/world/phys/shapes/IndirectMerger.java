package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;

public class IndirectMerger implements IndexMerger {
   private static final DoubleList EMPTY = DoubleLists.unmodifiable(DoubleArrayList.wrap(new double[]{0.0}));
   private final double[] result;
   private final int[] firstIndices;
   private final int[] secondIndices;
   private final int resultLength;

   public IndirectMerger(final DoubleList first, final DoubleList second, final boolean firstOnlyMatters, final boolean secondOnlyMatters) {
      double lastValue = Double.NaN;
      int firstSize = first.size();
      int secondSize = second.size();
      int capacity = firstSize + secondSize;
      this.result = new double[capacity];
      this.firstIndices = new int[capacity];
      this.secondIndices = new int[capacity];
      boolean canSkipFirst = !firstOnlyMatters;
      boolean canSkipSecond = !secondOnlyMatters;
      int resultIndex = 0;
      int firstIndex = 0;
      int secondIndex = 0;

      while (true) {
         boolean ranOutOfFirst = firstIndex >= firstSize;
         boolean ranOutOfSecond = secondIndex >= secondSize;
         if (ranOutOfFirst && ranOutOfSecond) {
            this.resultLength = Math.max(1, resultIndex);
            return;
         }

         boolean choseFirst = !ranOutOfFirst && (ranOutOfSecond || first.getDouble(firstIndex) < second.getDouble(secondIndex) + 1.0E-7);
         if (choseFirst) {
            firstIndex++;
            if (canSkipFirst && (secondIndex == 0 || ranOutOfSecond)) {
               continue;
            }
         } else {
            secondIndex++;
            if (canSkipSecond && (firstIndex == 0 || ranOutOfFirst)) {
               continue;
            }
         }

         int currentFirstIndex = firstIndex - 1;
         int currentSecondIndex = secondIndex - 1;
         double nextValue = choseFirst ? first.getDouble(currentFirstIndex) : second.getDouble(currentSecondIndex);
         if (!(lastValue >= nextValue - 1.0E-7)) {
            this.firstIndices[resultIndex] = currentFirstIndex;
            this.secondIndices[resultIndex] = currentSecondIndex;
            this.result[resultIndex] = nextValue;
            resultIndex++;
            lastValue = nextValue;
         } else {
            this.firstIndices[resultIndex - 1] = currentFirstIndex;
            this.secondIndices[resultIndex - 1] = currentSecondIndex;
         }
      }
   }

   @Override
   public boolean forMergedIndexes(final IndexMerger.IndexConsumer consumer) {
      int length = this.resultLength - 1;

      for (int i = 0; i < length; i++) {
         if (!consumer.merge(this.firstIndices[i], this.secondIndices[i], i)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public int size() {
      return this.resultLength;
   }

   @Override
   public DoubleList getList() {
      return (DoubleList)(this.resultLength <= 1 ? EMPTY : DoubleArrayList.wrap(this.result, this.resultLength));
   }
}
