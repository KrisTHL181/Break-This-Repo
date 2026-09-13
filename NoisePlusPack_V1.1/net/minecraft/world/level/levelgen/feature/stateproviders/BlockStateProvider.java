package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BlockStateProvider {
   public static final Codec<BlockStateProvider> CODEC = BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE
      .byNameCodec()
      .dispatch(BlockStateProvider::type, BlockStateProviderType::codec);

   public static SimpleStateProvider simple(BlockState p_191385_) {
      return new SimpleStateProvider(p_191385_);
   }

   public static SimpleStateProvider simple(Block p_191383_) {
      return new SimpleStateProvider(p_191383_.defaultBlockState());
   }

   protected abstract BlockStateProviderType<?> type();

   public abstract BlockState getState(RandomSource var1, BlockPos var2);
}
