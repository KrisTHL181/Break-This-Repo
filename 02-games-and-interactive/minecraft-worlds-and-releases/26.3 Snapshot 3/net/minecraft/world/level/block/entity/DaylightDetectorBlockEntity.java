package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DaylightDetectorBlockEntity extends BlockEntity {
   public DaylightDetectorBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.DAYLIGHT_DETECTOR, worldPosition, blockState);
   }
}
