package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;

public class CreakingHeartDecorator extends TreeDecorator {
   public static final MapCodec<CreakingHeartDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(CreakingHeartDecorator::new, p_364526_ -> p_364526_.probability);
   private final float probability;

   public CreakingHeartDecorator(float p_363257_) {
      this.probability = p_363257_;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.CREAKING_HEART;
   }

   @Override
   public void place(TreeDecorator.Context p_363618_) {
      RandomSource randomsource = p_363618_.random();
      List<BlockPos> list = p_363618_.logs();
      if (!list.isEmpty() && !(randomsource.nextFloat() >= this.probability)) {
         List<BlockPos> list1 = new ArrayList<>(list);
         Util.shuffle(list1, randomsource);
         Optional<BlockPos> optional = list1.stream().filter(p_368858_ -> {
            for (Direction direction : Direction.values()) {
               if (!p_363618_.checkBlock(p_368858_.relative(direction), p_368826_ -> p_368826_.is(BlockTags.LOGS))) {
                  return false;
               }
            }

            return true;
         }).findFirst();
         if (!optional.isEmpty()) {
            p_363618_.setBlock(
               optional.get(),
               Blocks.CREAKING_HEART
                  .defaultBlockState()
                  .setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT)
                  .setValue(CreakingHeartBlock.NATURAL, true)
            );
         }
      }
   }
}
