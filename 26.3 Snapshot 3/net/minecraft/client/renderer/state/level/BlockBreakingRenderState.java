package net.minecraft.client.renderer.state.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record BlockBreakingRenderState(BlockPos blockPos, BlockState blockState, int progress) {
}
