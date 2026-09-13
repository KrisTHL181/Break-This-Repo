package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public interface PlacementFilter extends PlacementModifier {
   @Override
   default void modify(final PlacementContext context, final RandomSource random, final BlockPos origin, final Consumer<BlockPos> output) {
      if (this.shouldPlace(context, random, origin)) {
         output.accept(origin);
      }
   }

   boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin);

   @Override
   MapCodec<? extends PlacementFilter> codec();
}
