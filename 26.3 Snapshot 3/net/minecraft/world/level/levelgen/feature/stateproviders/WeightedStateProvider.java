package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public record WeightedStateProvider(WeightedList<BlockState> weightedList) implements BlockStateProvider {
   public static final MapCodec<WeightedStateProvider> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(WeightedList.nonEmptyCodec(BlockState.CODEC).fieldOf("entries").forGetter(o -> o.weightedList)).apply(i, WeightedStateProvider::new)
   );

   public WeightedStateProvider {
      if (weightedList.isEmpty()) {
         throw new IllegalArgumentException("Weighted list must have at least one entry");
      }
   }

   public WeightedStateProvider(final WeightedList.Builder<BlockState> weightedList) {
      this(weightedList.build());
   }

   @Override
   public MapCodec<WeightedStateProvider> codec() {
      return CODEC;
   }

   @Override
   public BlockState getState(final LevelAccessor level, final RandomSource random, final BlockPos pos) {
      return this.weightedList.getRandomOrThrow(random);
   }
}
