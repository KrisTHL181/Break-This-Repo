package net.minecraft.world.level.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class NetherVines {
   private static final double BONEMEAL_GROW_PROBABILITY_DECREASE_RATE = 0.826;
   public static final double GROW_PER_TICK_PROBABILITY = 0.1;

   public static boolean isValidGrowthState(final BlockState state) {
      return state.isAir();
   }

   public static int getBlocksToGrowWhenBonemealed(final RandomSource random) {
      double growProbabilty = 1.0;

      int count;
      for (count = 0; random.nextDouble() < growProbabilty; count++) {
         growProbabilty *= 0.826;
      }

      return count;
   }
}
