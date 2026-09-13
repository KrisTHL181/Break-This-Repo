package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AttachedToLogsDecorator extends TreeDecorator {
   public static final MapCodec<AttachedToLogsDecorator> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(p -> p.probability),
            BlockStateProvider.CODEC.fieldOf("block_provider").forGetter(p -> p.blockProvider),
            ExtraCodecs.nonEmptyList(Direction.CODEC.listOf()).fieldOf("directions").forGetter(p -> p.directions)
         )
         .apply(i, AttachedToLogsDecorator::new)
   );
   private final float probability;
   private final BlockStateProvider blockProvider;
   private final List<Direction> directions;

   public AttachedToLogsDecorator(final float probability, final BlockStateProvider blockProvider, final List<Direction> directions) {
      this.probability = probability;
      this.blockProvider = blockProvider;
      this.directions = directions;
   }

   @Override
   public void place(final TreeDecorator.Context context) {
      RandomSource random = context.random();

      for (BlockPos logsPos : Util.shuffledCopy(context.logs(), random)) {
         Direction direction = Util.getRandom(this.directions, random);
         BlockPos placementPos = logsPos.relative(direction);
         if (random.nextFloat() <= this.probability && context.isAir(placementPos)) {
            context.setBlock(placementPos, this.blockProvider.getState(context.level(), random, placementPos));
         }
      }
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.ATTACHED_TO_LOGS;
   }
}
