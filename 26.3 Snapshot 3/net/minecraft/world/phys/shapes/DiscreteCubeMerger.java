package net.minecraft.world.phys.shapes;

import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public final class DiscreteCubeMerger implements IndexMerger {
   private final CubePointRange result;
   private final int firstDiv;
   private final int secondDiv;

   DiscreteCubeMerger(final int firstSize, final int secondSize) {
      this.result = new CubePointRange((int)Shapes.lcm(firstSize, secondSize));
      int gcd = IntMath.gcd(firstSize, secondSize);
      this.firstDiv = firstSize / gcd;
      this.secondDiv = secondSize / gcd;
   }

   @Override
   public boolean forMergedIndexes(final IndexMerger.IndexConsumer consumer) {
      int size = this.result.size() - 1;

      for (int i = 0; i < size; i++) {
         if (!consumer.merge(i / this.secondDiv, i / this.firstDiv, i)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public int size() {
      return this.result.size();
   }

   @Override
   public DoubleList getList() {
      return this.result;
   }
}
