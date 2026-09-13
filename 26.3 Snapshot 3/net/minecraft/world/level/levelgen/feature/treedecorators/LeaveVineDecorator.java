package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class LeaveVineDecorator extends TreeDecorator {
   public static final MapCodec<LeaveVineDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(LeaveVineDecorator::new, d -> d.probability);
   private final float probability;

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.LEAVE_VINE;
   }

   public LeaveVineDecorator(final float probability) {
      this.probability = probability;
   }

   @Override
   public void place(final TreeDecorator.Context context) {
      RandomSource random = context.random();
      context.leaves().forEach(pos -> {
         if (random.nextFloat() < this.probability) {
            BlockPos west = pos.west();
            if (context.isAir(west)) {
               addHangingVine(west, VineBlock.EAST, context);
            }
         }

         if (random.nextFloat() < this.probability) {
            BlockPos east = pos.east();
            if (context.isAir(east)) {
               addHangingVine(east, VineBlock.WEST, context);
            }
         }

         if (random.nextFloat() < this.probability) {
            BlockPos north = pos.north();
            if (context.isAir(north)) {
               addHangingVine(north, VineBlock.SOUTH, context);
            }
         }

         if (random.nextFloat() < this.probability) {
            BlockPos south = pos.south();
            if (context.isAir(south)) {
               addHangingVine(south, VineBlock.NORTH, context);
            }
         }
      });
   }

   private static void addHangingVine(BlockPos pos, final BooleanProperty direction, final TreeDecorator.Context context) {
      context.placeVine(pos, direction);
      int maxDir = 4;

      for (BlockPos var4 = pos.below(); context.isAir(var4) && maxDir > 0; maxDir--) {
         context.placeVine(var4, direction);
         var4 = var4.below();
      }
   }
}
