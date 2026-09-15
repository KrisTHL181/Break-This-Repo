package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Continuation;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface BlockStateConsumer {
   Continuation apply(BlockPos pos, BlockState state);
}
