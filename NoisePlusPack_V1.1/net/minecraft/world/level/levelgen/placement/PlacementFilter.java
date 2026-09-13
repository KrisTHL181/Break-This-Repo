package net.minecraft.world.level.levelgen.placement;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public abstract class PlacementFilter extends PlacementModifier {
   @Override
   public final Stream<BlockPos> getPositions(PlacementContext p_226386_, RandomSource p_226387_, BlockPos p_226388_) {
      return this.shouldPlace(p_226386_, p_226387_, p_226388_) ? Stream.of(p_226388_) : Stream.of();
   }

   protected abstract boolean shouldPlace(PlacementContext var1, RandomSource var2, BlockPos var3);
}
