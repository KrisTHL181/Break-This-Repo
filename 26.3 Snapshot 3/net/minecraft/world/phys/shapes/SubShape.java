package net.minecraft.world.phys.shapes;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class SubShape extends DiscreteVoxelShape {
   private final DiscreteVoxelShape parent;
   private final int startX;
   private final int startY;
   private final int startZ;
   private final int endX;
   private final int endY;
   private final int endZ;

   SubShape(final DiscreteVoxelShape parent, final int startX, final int startY, final int startZ, final int endX, final int endY, final int endZ) {
      super(endX - startX, endY - startY, endZ - startZ);
      this.parent = parent;
      this.startX = startX;
      this.startY = startY;
      this.startZ = startZ;
      this.endX = endX;
      this.endY = endY;
      this.endZ = endZ;
   }

   @Override
   public boolean isFull(final int x, final int y, final int z) {
      return this.parent.isFull(this.startX + x, this.startY + y, this.startZ + z);
   }

   @Override
   public void fill(final int x, final int y, final int z) {
      this.parent.fill(this.startX + x, this.startY + y, this.startZ + z);
   }

   @Override
   public int firstFull(final Direction.Axis axis) {
      return this.clampToShape(axis, this.parent.firstFull(axis));
   }

   @Override
   public int lastFull(final Direction.Axis axis) {
      return this.clampToShape(axis, this.parent.lastFull(axis));
   }

   private int clampToShape(final Direction.Axis axis, final int parentResult) {
      int start = axis.choose(this.startX, this.startY, this.startZ);
      int end = axis.choose(this.endX, this.endY, this.endZ);
      return Mth.clamp(parentResult, start, end) - start;
   }
}
