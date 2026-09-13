package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;

public class ArrayVoxelShape extends VoxelShape {
   private final DoubleList xs;
   private final DoubleList ys;
   private final DoubleList zs;

   ArrayVoxelShape(final DiscreteVoxelShape shape, final double[] xs, final double[] ys, final double[] zs) {
      this(
         shape,
         DoubleArrayList.wrap(Arrays.copyOf(xs, shape.getXSize() + 1)),
         DoubleArrayList.wrap(Arrays.copyOf(ys, shape.getYSize() + 1)),
         DoubleArrayList.wrap(Arrays.copyOf(zs, shape.getZSize() + 1))
      );
   }

   ArrayVoxelShape(final DiscreteVoxelShape shape, final DoubleList xs, final DoubleList ys, final DoubleList zs) {
      super(shape);
      int xSize = shape.getXSize() + 1;
      int ySize = shape.getYSize() + 1;
      int zSize = shape.getZSize() + 1;
      if (xSize == xs.size() && ySize == ys.size() && zSize == zs.size()) {
         this.xs = xs;
         this.ys = ys;
         this.zs = zs;
      } else {
         throw (IllegalArgumentException)Util.pauseInIde(
            new IllegalArgumentException("Lengths of point arrays must be consistent with the size of the VoxelShape.")
         );
      }
   }

   @Override
   public DoubleList getCoords(final Direction.Axis axis) {
      return switch (axis) {
         case X -> this.xs;
         case Y -> this.ys;
         case Z -> this.zs;
      };
   }
}
