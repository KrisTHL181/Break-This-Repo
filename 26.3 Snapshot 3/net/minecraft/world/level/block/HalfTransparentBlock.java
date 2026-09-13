package net.minecraft.world.level.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class HalfTransparentBlock extends Block {
   protected HalfTransparentBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected boolean skipRendering(final BlockState state, final BlockState neighborState, final Direction direction) {
      return neighborState.is(this) ? true : super.skipRendering(state, neighborState, direction);
   }
}
