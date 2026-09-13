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
      p_394426_ -> p_394426_.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(p_396823_ -> p_396823_.probability),
            BlockStateProvider.CODEC.fieldOf("block_provider").forGetter(p_393035_ -> p_393035_.blockProvider),
            ExtraCodecs.nonEmptyList(Direction.CODEC.listOf()).fieldOf("directions").forGetter(p_397159_ -> p_397159_.directions)
         )
         .apply(p_394426_, AttachedToLogsDecorator::new)
   );
   private final float probability;
   private final BlockStateProvider blockProvider;
   private final List<Direction> directions;

   public AttachedToLogsDecorator(float p_394913_, BlockStateProvider p_394694_, List<Direction> p_392249_) {
      this.probability = p_394913_;
      this.blockProvider = p_394694_;
      this.directions = p_392249_;
   }

   @Override
   public void place(TreeDecorator.Context p_397655_) {
      RandomSource randomsource = p_397655_.random();

      for (BlockPos blockpos : Util.shuffledCopy(p_397655_.logs(), randomsource)) {
         Direction direction = Util.getRandom(this.directions, randomsource);
         BlockPos blockpos1 = blockpos.relative(direction);
         if (randomsource.nextFloat() <= this.probability && p_397655_.isAir(blockpos1)) {
            p_397655_.setBlock(blockpos1, this.blockProvider.getState(randomsource, blockpos1));
         }
      }
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.ATTACHED_TO_LOGS;
   }
}
