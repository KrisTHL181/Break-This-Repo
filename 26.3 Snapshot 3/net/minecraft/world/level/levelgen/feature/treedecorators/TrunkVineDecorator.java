package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;

public class TrunkVineDecorator extends TreeDecorator {
   public static final MapCodec<TrunkVineDecorator> CODEC = MapCodec.unit(() -> TrunkVineDecorator.INSTANCE);
   public static final TrunkVineDecorator INSTANCE = new TrunkVineDecorator();

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.TRUNK_VINE;
   }

   @Override
   public void place(final TreeDecorator.Context context) {
      RandomSource random = context.random();
      context.logs().forEach(pos -> {
         if (random.nextInt(3) > 0) {
            BlockPos west = pos.west();
            if (context.isAir(west)) {
               context.placeVine(west, VineBlock.EAST);
            }
         }

         if (random.nextInt(3) > 0) {
            BlockPos east = pos.east();
            if (context.isAir(east)) {
               context.placeVine(east, VineBlock.WEST);
            }
         }

         if (random.nextInt(3) > 0) {
            BlockPos north = pos.north();
            if (context.isAir(north)) {
               context.placeVine(north, VineBlock.SOUTH);
            }
         }

         if (random.nextInt(3) > 0) {
            BlockPos south = pos.south();
            if (context.isAir(south)) {
               context.placeVine(south, VineBlock.NORTH);
            }
         }
      });
   }
}
