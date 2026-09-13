package net.minecraft.world.level;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

public interface LevelSimulatedReader {
   boolean isStateAtPosition(final BlockPos pos, final Predicate<BlockState> predicate);

   boolean isFluidAtPosition(final BlockPos pos, final Predicate<FluidState> predicate);

   <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type);

   BlockPos getHeightmapPos(final Heightmap.Types type, final BlockPos pos);
}
