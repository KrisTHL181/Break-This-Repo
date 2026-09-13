package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

public interface PositionalRandomFactory {
   default RandomSource at(BlockPos p_224543_) {
      return this.at(p_224543_.getX(), p_224543_.getY(), p_224543_.getZ());
   }

   default RandomSource fromHashOf(Identifier p_459607_) {
      return this.fromHashOf(p_459607_.toString());
   }

   RandomSource fromHashOf(String var1);

   RandomSource fromSeed(long var1);

   RandomSource at(int var1, int var2, int var3);

   @VisibleForTesting
   void parityConfigString(StringBuilder var1);
}
