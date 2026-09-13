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
      .xmap(CreakingHeartDecorator::new, d -> d.probability);
   private final float probability;

   public CreakingHeartDecorator(final float probability) {
      this.probability = probability;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.CREAKING_HEART;
   }

   @Override
   public void place(final TreeDecorator.Context context) {
      RandomSource random = context.random();
      List<BlockPos> logs = context.logs();
      if (!logs.isEmpty()) {
         if (!(random.nextFloat() >= this.probability)) {
            List<BlockPos> heartPlacements = new ArrayList<>(logs);
            Util.shuffle(heartPlacements, random);
            Optional<BlockPos> targetPos = heartPlacements.stream().filter(pos -> {
               for (Direction dir : Direction.values()) {
                  if (!context.checkBlock(pos.relative(dir), state -> state.is(BlockTags.LOGS))) {
                     return false;
                  }
               }

               return true;
            }).findFirst();
            if (!targetPos.isEmpty()) {
               context.setBlock(
                  targetPos.get(),
                  Blocks.CREAKING_HEART
                     .defaultBlockState()
                     .setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT)
                     .setValue(CreakingHeartBlock.NATURAL, true)
               );
            }
         }
      }
   }
}
