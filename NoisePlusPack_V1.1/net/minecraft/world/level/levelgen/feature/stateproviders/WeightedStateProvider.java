package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;

public class WeightedStateProvider extends BlockStateProvider {
   public static final MapCodec<WeightedStateProvider> CODEC = WeightedList.nonEmptyCodec(BlockState.CODEC)
      .comapFlatMap(WeightedStateProvider::create, p_391045_ -> p_391045_.weightedList)
      .fieldOf("entries");
   private final WeightedList<BlockState> weightedList;

   private static DataResult<WeightedStateProvider> create(WeightedList<BlockState> p_396945_) {
      return p_396945_.isEmpty() ? DataResult.error(() -> "WeightedStateProvider with no states") : DataResult.success(new WeightedStateProvider(p_396945_));
   }

   public WeightedStateProvider(WeightedList<BlockState> p_396330_) {
      this.weightedList = p_396330_;
   }

   public WeightedStateProvider(WeightedList.Builder<BlockState> p_396337_) {
      this(p_396337_.build());
   }

   @Override
   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.WEIGHTED_STATE_PROVIDER;
   }

   @Override
   public BlockState getState(RandomSource p_225966_, BlockPos p_225967_) {
      return this.weightedList.getRandomOrThrow(p_225966_);
   }
}
