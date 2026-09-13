package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record ProjectedRandomPatchySquare(BlockStateProvider block, BlockPredicate projectThrough, IntProvider size, int maxProjectionHeight) implements Feature {
   public static final MapCodec<ProjectedRandomPatchySquare> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("block").forGetter(ProjectedRandomPatchySquare::block),
            BlockPredicate.CODEC.fieldOf("project_through").forGetter(ProjectedRandomPatchySquare::projectThrough),
            IntProviders.codec(1, 16).fieldOf("size").forGetter(ProjectedRandomPatchySquare::size),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_projection_height").forGetter(ProjectedRandomPatchySquare::maxProjectionHeight)
         )
         .apply(i, ProjectedRandomPatchySquare::new)
   );

   @Override
   public MapCodec<ProjectedRandomPatchySquare> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos pos) {
      BlockPos.MutableBlockPos basePos = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos tmpPos = new BlockPos.MutableBlockPos();
      int size = this.size.sample(random);
      int bound = Mth.square(size) + 1;

      for (int dx = -size; dx <= size; dx++) {
         for (int dz = -size; dz <= size; dz++) {
            int probability = Mth.abs(dx) * Mth.abs(dz);
            if (random.nextInt(bound) < bound - probability) {
               basePos.setWithOffset(pos, dx, 0, dz);
               int drop = this.maxProjectionHeight;

               while (this.projectThrough.test(level, tmpPos.setWithOffset(basePos, Direction.DOWN))) {
                  basePos.move(Direction.DOWN);
                  if (--drop <= 0) {
                     break;
                  }
               }

               BlockState state = this.block.getOptionalState(level, random, basePos);
               if (state != null) {
                  level.setBlock(basePos, state, 2);
               }
            }
         }
      }

      return true;
   }
}
