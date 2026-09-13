package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;

public class CocoaDecorator extends TreeDecorator {
   public static final MapCodec<CocoaDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(CocoaDecorator::new, d -> d.probability);
   private final float probability;

   public CocoaDecorator(final float probability) {
      this.probability = probability;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.COCOA;
   }

   @Override
   public void place(final TreeDecorator.Context context) {
      RandomSource random = context.random();
      if (!(random.nextFloat() >= this.probability)) {
         List<BlockPos> logs = context.logs();
         if (!logs.isEmpty()) {
            int treeY = logs.getFirst().getY();
            logs.stream()
               .filter(pos -> pos.getY() - treeY <= 2)
               .forEach(
                  pos -> {
                     for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (random.nextFloat() <= 0.25F) {
                           Direction opposite = direction.getOpposite();
                           BlockPos cocoaPos = pos.offset(opposite.getStepX(), 0, opposite.getStepZ());
                           if (context.isAir(cocoaPos)) {
                              context.setBlock(
                                 cocoaPos, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE, random.nextInt(3)).setValue(CocoaBlock.FACING, direction)
                              );
                           }
                        }
                     }
                  }
               );
         }
      }
   }
}
