package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface BonemealableBlock {
   boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state);

   boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state);

   void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state);

   static boolean hasSpreadableNeighbourPos(final LevelReader level, final BlockPos pos, final BlockState blockToPlace) {
      return getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.stream().toList(), level, pos, blockToPlace).isPresent();
   }

   static Optional<BlockPos> findSpreadableNeighbourPos(final Level level, final BlockPos pos, final BlockState blockToPlace) {
      return getSpreadableNeighbourPos(Direction.Plane.HORIZONTAL.shuffledCopy(level.getRandom()), level, pos, blockToPlace);
   }

   private static Optional<BlockPos> getSpreadableNeighbourPos(
      final List<Direction> directions, final LevelReader level, final BlockPos pos, final BlockState blockToPlace
   ) {
      for (Direction direction : directions) {
         BlockPos neighbourPos = pos.relative(direction);
         if (level.isEmptyBlock(neighbourPos) && blockToPlace.canSurvive(level, neighbourPos)) {
            return Optional.of(neighbourPos);
         }
      }

      return Optional.empty();
   }

   default BlockPos getParticlePos(final BlockPos blockPos) {
      return switch (this.getType()) {
         case NEIGHBOR_SPREADER -> blockPos.above();
         case GROWER -> blockPos;
      };
   }

   default BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.GROWER;
   }

   enum Type {
      NEIGHBOR_SPREADER,
      GROWER;
   }
}
