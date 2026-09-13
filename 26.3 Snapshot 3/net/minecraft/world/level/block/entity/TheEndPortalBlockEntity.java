package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class TheEndPortalBlockEntity extends BlockEntity {
   protected TheEndPortalBlockEntity(final BlockEntityType<?> type, final BlockPos worldPosition, final BlockState blockState) {
      super(type, worldPosition, blockState);
   }

   public TheEndPortalBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      this(BlockEntityTypes.END_PORTAL, worldPosition, blockState);
   }

   public boolean shouldRenderFace(final Direction direction) {
      return direction.getAxis() == Direction.Axis.Y;
   }
}
