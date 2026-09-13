package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public record SimpleStateProvider(BlockState state) implements BlockStateProvider {
   public static final MapCodec<SimpleStateProvider> CODEC = BlockState.CODEC.fieldOf("state").xmap(SimpleStateProvider::new, p -> p.state);

   @Override
   public MapCodec<SimpleStateProvider> codec() {
      return CODEC;
   }

   @Override
   public BlockState getState(final LevelAccessor level, final RandomSource random, final BlockPos pos) {
      return this.state;
   }
}
