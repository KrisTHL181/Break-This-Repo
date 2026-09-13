package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface BlockStateProvider {
   Codec<BlockStateProvider> CODEC = BuiltInRegistries.BLOCK_STATE_PROVIDER_TYPE.byNameCodec().dispatch(BlockStateProvider::codec, c -> c);

   static SimpleStateProvider simple(final BlockState state) {
      return new SimpleStateProvider(state);
   }

   static SimpleStateProvider simple(final Block block) {
      return new SimpleStateProvider(block.defaultBlockState());
   }

   MapCodec<? extends BlockStateProvider> codec();

   BlockState getState(final LevelAccessor level, final RandomSource random, final BlockPos pos);

   default @Nullable BlockState getOptionalState(final LevelAccessor level, final RandomSource random, final BlockPos pos) {
      return this.getState(level, random, pos);
   }
}
