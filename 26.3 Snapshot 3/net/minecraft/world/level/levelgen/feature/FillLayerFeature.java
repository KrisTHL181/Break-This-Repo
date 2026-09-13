package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;

public record FillLayerFeature(int height, BlockState state) implements Feature {
   public static final MapCodec<FillLayerFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter(FillLayerFeature::height),
            BlockState.CODEC.fieldOf("state").forGetter(FillLayerFeature::state)
         )
         .apply(i, FillLayerFeature::new)
   );

   @Override
   public MapCodec<FillLayerFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int dx = 0; dx < 16; dx++) {
         for (int dz = 0; dz < 16; dz++) {
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = level.getMinY() + this.height;
            pos.set(x, y, z);
            if (level.getBlockState(pos).isAir()) {
               level.setBlock(pos, this.state, 2);
            }
         }
      }

      return true;
   }
}
