package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public interface ChangeOverTimeBlock<T extends Enum<T>> {
   int SCAN_DISTANCE = 4;

   Optional<BlockState> getNext(BlockState state);

   float getChanceModifier();

   default void changeOverTime(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      float eachBlockOncePerDayChance = 0.05688889F;
      if (random.nextFloat() < 0.05688889F) {
         this.getNextState(state, level, pos, random).ifPresent(weatheredState -> level.setBlockAndUpdate(pos, weatheredState));
      }
   }

   T getAge();

   default Optional<BlockState> getNextState(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      int ownAge = this.getAge().ordinal();
      int sameAgeCount = 0;
      int olderCount = 0;

      for (BlockPos blockPos : BlockPos.withinManhattan(pos, 4, 4, 4)) {
         int manhattanDistance = blockPos.distManhattan(pos);
         if (manhattanDistance > 4) {
            break;
         }

         if (!blockPos.equals(pos) && level.getBlockState(blockPos).getBlock() instanceof ChangeOverTimeBlock<?> neighborBlock) {
            Enum<?> neighborAge = neighborBlock.getAge();
            if (this.getAge().getClass() == neighborAge.getClass()) {
               int foundAge = neighborAge.ordinal();
               if (foundAge < ownAge) {
                  return Optional.empty();
               }

               if (foundAge > ownAge) {
                  olderCount++;
               } else {
                  sameAgeCount++;
               }
            }
         }
      }

      float chance = (float)(olderCount + 1) / (olderCount + sameAgeCount + 1);
      float actualChance = chance * chance * this.getChanceModifier();
      return random.nextFloat() < actualChance ? this.getNext(state) : Optional.empty();
   }
}
