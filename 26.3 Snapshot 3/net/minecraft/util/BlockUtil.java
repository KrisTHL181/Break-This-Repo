package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntStack;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockUtil {
   public static BlockUtil.FoundRectangle getLargestRectangleAround(
      final BlockPos center, final Direction.Axis axis1, final int limit1, final Direction.Axis axis2, final int limit2, final Predicate<BlockPos> test
   ) {
      BlockPos.MutableBlockPos pos = center.mutable();
      Direction negativeDirection1 = Direction.get(Direction.AxisDirection.NEGATIVE, axis1);
      Direction positiveDirection1 = negativeDirection1.getOpposite();
      Direction negativeDirection2 = Direction.get(Direction.AxisDirection.NEGATIVE, axis2);
      Direction positiveDirection2 = negativeDirection2.getOpposite();
      int negativeDelta1 = getLimit(test, pos.set(center), negativeDirection1, limit1);
      int positiveDelta1 = getLimit(test, pos.set(center), positiveDirection1, limit1);
      int centerIndex1 = negativeDelta1;
      BlockUtil.IntBounds[] boundsByAxis1 = new BlockUtil.IntBounds[centerIndex1 + 1 + positiveDelta1];
      boundsByAxis1[centerIndex1] = new BlockUtil.IntBounds(
         getLimit(test, pos.set(center), negativeDirection2, limit2), getLimit(test, pos.set(center), positiveDirection2, limit2)
      );
      int centerIndex2 = boundsByAxis1[centerIndex1].min;

      for (int i = 1; i <= negativeDelta1; i++) {
         BlockUtil.IntBounds lastBounds = boundsByAxis1[centerIndex1 - (i - 1)];
         boundsByAxis1[centerIndex1 - i] = new BlockUtil.IntBounds(
            getLimit(test, pos.set(center).move(negativeDirection1, i), negativeDirection2, lastBounds.min),
            getLimit(test, pos.set(center).move(negativeDirection1, i), positiveDirection2, lastBounds.max)
         );
      }

      for (int i = 1; i <= positiveDelta1; i++) {
         BlockUtil.IntBounds lastBounds = boundsByAxis1[centerIndex1 + i - 1];
         boundsByAxis1[centerIndex1 + i] = new BlockUtil.IntBounds(
            getLimit(test, pos.set(center).move(positiveDirection1, i), negativeDirection2, lastBounds.min),
            getLimit(test, pos.set(center).move(positiveDirection1, i), positiveDirection2, lastBounds.max)
         );
      }

      int minAxis1 = 0;
      int minAxis2 = 0;
      int sizeAxis1 = 0;
      int sizeAxis2 = 0;
      int[] columns = new int[boundsByAxis1.length];

      for (int i2 = centerIndex2; i2 >= 0; i2--) {
         for (int i1 = 0; i1 < boundsByAxis1.length; i1++) {
            BlockUtil.IntBounds bounds2 = boundsByAxis1[i1];
            int min2 = centerIndex2 - bounds2.min;
            int max2 = centerIndex2 + bounds2.max;
            columns[i1] = i2 >= min2 && i2 <= max2 ? max2 + 1 - i2 : 0;
         }

         Pair<BlockUtil.IntBounds, Integer> rectangle = getMaxRectangleLocation(columns);
         BlockUtil.IntBounds boundsAxis1 = (BlockUtil.IntBounds)rectangle.getFirst();
         int newSizeAxis1 = 1 + boundsAxis1.max - boundsAxis1.min;
         int newSizeAxis2 = (Integer)rectangle.getSecond();
         if (newSizeAxis1 * newSizeAxis2 > sizeAxis1 * sizeAxis2) {
            minAxis1 = boundsAxis1.min;
            minAxis2 = i2;
            sizeAxis1 = newSizeAxis1;
            sizeAxis2 = newSizeAxis2;
         }
      }

      return new BlockUtil.FoundRectangle(center.relative(axis1, minAxis1 - centerIndex1).relative(axis2, minAxis2 - centerIndex2), sizeAxis1, sizeAxis2);
   }

   private static int getLimit(final Predicate<BlockPos> test, final BlockPos.MutableBlockPos pos, final Direction direction, final int limit) {
      int max = 0;

      while (max < limit && test.test(pos.move(direction))) {
         max++;
      }

      return max;
   }

   @VisibleForTesting
   static Pair<BlockUtil.IntBounds, Integer> getMaxRectangleLocation(final int[] columns) {
      int maxStart = 0;
      int maxEnd = 0;
      int maxHeight = 0;
      IntStack stack = new IntArrayList();
      stack.push(0);

      for (int column = 1; column <= columns.length; column++) {
         int height = column == columns.length ? 0 : columns[column];

         while (!stack.isEmpty()) {
            int stackHeight = columns[stack.topInt()];
            if (height >= stackHeight) {
               stack.push(column);
               break;
            }

            stack.popInt();
            int start = stack.isEmpty() ? 0 : stack.topInt() + 1;
            if (stackHeight * (column - start) > maxHeight * (maxEnd - maxStart)) {
               maxEnd = column;
               maxStart = start;
               maxHeight = stackHeight;
            }
         }

         if (stack.isEmpty()) {
            stack.push(column);
         }
      }

      return new Pair(new BlockUtil.IntBounds(maxStart, maxEnd - 1), maxHeight);
   }

   public static Optional<BlockPos> getTopConnectedBlock(
      final BlockGetter level, final BlockPos pos, final Block bodyBlock, final Direction growthDirection, final Block headBlock
   ) {
      BlockPos.MutableBlockPos forwardPos = pos.mutable();

      BlockState forwardState;
      do {
         forwardPos.move(growthDirection);
         forwardState = level.getBlockState(forwardPos);
      } while (forwardState.is(bodyBlock));

      return forwardState.is(headBlock) ? Optional.of(forwardPos) : Optional.empty();
   }

   public static class FoundRectangle {
      public final BlockPos minCorner;
      public final int axis1Size;
      public final int axis2Size;

      public FoundRectangle(final BlockPos minCorner, final int axis1Size, final int axis2Size) {
         this.minCorner = minCorner;
         this.axis1Size = axis1Size;
         this.axis2Size = axis2Size;
      }
   }

   public static class IntBounds {
      public final int min;
      public final int max;

      public IntBounds(final int min, final int max) {
         this.min = min;
         this.max = max;
      }

      @Override
      public String toString() {
         return "IntBounds{min=" + this.min + ", max=" + this.max + "}";
      }
   }
}
