package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record CopyPropertiesProvider(BlockStateProvider source) implements BlockStateProvider {
   public static final MapCodec<CopyPropertiesProvider> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(BlockStateProvider.CODEC.fieldOf("source").forGetter(CopyPropertiesProvider::source)).apply(i, CopyPropertiesProvider::new)
   );

   public CopyPropertiesProvider(final Block block) {
      this(BlockStateProvider.simple(block.defaultBlockState()));
   }

   @Override
   public MapCodec<CopyPropertiesProvider> codec() {
      return CODEC;
   }

   @Override
   public BlockState getState(final LevelAccessor level, final RandomSource random, final BlockPos pos) {
      return this.source.getState(level, random, pos).withPropertiesOf(level.getBlockState(pos));
   }
}
