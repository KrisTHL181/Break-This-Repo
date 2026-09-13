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
   boolean isStateAtPosition(BlockPos var1, Predicate<BlockState> var2);

   boolean isFluidAtPosition(BlockPos var1, Predicate<FluidState> var2);

   <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos var1, BlockEntityType<T> var2);

   BlockPos getHeightmapPos(Heightmap.Types var1, BlockPos var2);
}
