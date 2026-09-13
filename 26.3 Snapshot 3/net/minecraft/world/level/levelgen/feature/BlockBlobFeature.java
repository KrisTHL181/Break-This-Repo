package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public record BlockBlobFeature(BlockState state, BlockPredicate canPlaceOn) implements Feature {
   public static final MapCodec<BlockBlobFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockState.CODEC.fieldOf("state").forGetter(BlockBlobFeature::state),
            BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(BlockBlobFeature::canPlaceOn)
         )
         .apply(i, BlockBlobFeature::new)
   );

   @Override
   public MapCodec<BlockBlobFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, BlockPos origin) {
      while (origin.getY() > level.getMinY() + 3 && !this.canPlaceOn.test(level, origin.below())) {
         origin = origin.below();
      }

      if (origin.getY() <= level.getMinY() + 3) {
         return false;
      }

      for (int c = 0; c < 3; c++) {
         int xr = random.nextInt(2);
         int yr = random.nextInt(2);
         int zr = random.nextInt(2);
         float tr = (xr + yr + zr) * 0.333F + 0.5F;

         for (BlockPos blockPos : BlockPos.betweenClosed(origin.offset(-xr, -yr, -zr), origin.offset(xr, yr, zr))) {
            if (blockPos.distSqr(origin) <= tr * tr) {
               level.setBlockAndUpdate(blockPos, this.state);
            }
         }

         origin = origin.offset(-1 + random.nextInt(2), -random.nextInt(2), -1 + random.nextInt(2));
      }

      return true;
   }
}
