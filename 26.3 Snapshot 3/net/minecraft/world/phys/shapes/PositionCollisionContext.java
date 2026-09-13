package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class PositionCollisionContext implements CollisionContext {
   private final double y;

   public PositionCollisionContext(final double y) {
      this.y = y;
   }

   @Override
   public boolean isDescending() {
      return false;
   }

   @Override
   public boolean isAbove(final VoxelShape shape, final BlockPos pos, final boolean defaultValue) {
      return this.y > pos.getY() + shape.max(Direction.Axis.Y) - 1.0E-5F;
   }

   @Override
   public boolean isHoldingItem(final Item item) {
      return false;
   }

   @Override
   public boolean alwaysCollideWithFluid() {
      return false;
   }

   @Override
   public boolean canStandOnFluid(final FluidState fluidStateAbove, final FluidState fluid) {
      return false;
   }

   @Override
   public VoxelShape getCollisionShape(final BlockState state, final CollisionGetter collisionGetter, final BlockPos pos) {
      return state.getCollisionShape(collisionGetter, pos, this);
   }
}
