package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

public interface PositionalRandomFactory {
   default RandomSource at(final BlockPos pos) {
      return this.at(pos.getX(), pos.getY(), pos.getZ());
   }

   default RandomSource fromHashOf(final Identifier name) {
      return this.fromHashOf(name.toString());
   }

   RandomSource fromHashOf(final String name);

   RandomSource fromSeed(final long seed);

   RandomSource at(final int x, final int y, final int z);

   @VisibleForTesting
   void parityConfigString(StringBuilder sb);
}
