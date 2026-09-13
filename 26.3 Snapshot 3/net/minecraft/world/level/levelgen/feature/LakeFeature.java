package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

@Deprecated
public record LakeFeature(
   BlockStateProvider fluid,
   BlockStateProvider barrier,
   BlockPredicate canPlaceFeature,
   BlockPredicate canReplaceWithAirOrFluid,
   BlockPredicate canReplaceWithBarrier
) implements Feature {
   public static final MapCodec<LakeFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("fluid").forGetter(LakeFeature::fluid),
            BlockStateProvider.CODEC.fieldOf("barrier").forGetter(LakeFeature::barrier),
            BlockPredicate.CODEC.fieldOf("can_place_feature").forGetter(LakeFeature::canPlaceFeature),
            BlockPredicate.CODEC.fieldOf("can_replace_with_air_or_fluid").forGetter(LakeFeature::canReplaceWithAirOrFluid),
            BlockPredicate.CODEC.fieldOf("can_replace_with_barrier").forGetter(LakeFeature::canReplaceWithBarrier)
         )
         .apply(i, LakeFeature::new)
   );
   private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

   @Override
   public MapCodec<LakeFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, BlockPos origin) {
      if (origin.getY() <= level.getMinY() + 4) {
         return false;
      }

      origin = origin.offset(-8, -4, -8);
      boolean[] grid = new boolean[2048];
      int spots = random.nextInt(4) + 4;

      for (int i = 0; i < spots; i++) {
         double xr = random.nextDouble() * 6.0 + 3.0;
         double yr = random.nextDouble() * 4.0 + 2.0;
         double zr = random.nextDouble() * 6.0 + 3.0;
         double xp = random.nextDouble() * (16.0 - xr - 2.0) + 1.0 + xr / 2.0;
         double yp = random.nextDouble() * (8.0 - yr - 4.0) + 2.0 + yr / 2.0;
         double zp = random.nextDouble() * (16.0 - zr - 2.0) + 1.0 + zr / 2.0;

         for (int xx = 1; xx < 15; xx++) {
            for (int zz = 1; zz < 15; zz++) {
               for (int yy = 1; yy < 7; yy++) {
                  double xd = (xx - xp) / (xr / 2.0);
                  double yd = (yy - yp) / (yr / 2.0);
                  double zd = (zz - zp) / (zr / 2.0);
                  double d = xd * xd + yd * yd + zd * zd;
                  if (d < 1.0) {
                     grid[(xx * 16 + zz) * 8 + yy] = true;
                  }
               }
            }
         }
      }

      BlockState fluid = this.fluid.getState(level, random, origin);

      for (int xx = 0; xx < 16; xx++) {
         for (int zz = 0; zz < 16; zz++) {
            for (int yy = 0; yy < 8; yy++) {
               boolean check = !grid[(xx * 16 + zz) * 8 + yy]
                  && (
                     xx < 15 && grid[((xx + 1) * 16 + zz) * 8 + yy]
                        || xx > 0 && grid[((xx - 1) * 16 + zz) * 8 + yy]
                        || zz < 15 && grid[(xx * 16 + zz + 1) * 8 + yy]
                        || zz > 0 && grid[(xx * 16 + (zz - 1)) * 8 + yy]
                        || yy < 7 && grid[(xx * 16 + zz) * 8 + yy + 1]
                        || yy > 0 && grid[(xx * 16 + zz) * 8 + (yy - 1)]
                  );
               if (check) {
                  BlockPos offsetPos = origin.offset(xx, yy, zz);
                  BlockState blockState = level.getBlockState(offsetPos);
                  if (yy >= 4 && blockState.liquid()) {
                     return false;
                  }

                  if (yy < 4 && !blockState.isSolid() && blockState != fluid) {
                     return false;
                  }

                  if (!this.canPlaceFeature.test(level, offsetPos)) {
                     return false;
                  }
               }
            }
         }
      }

      for (int xx = 0; xx < 16; xx++) {
         for (int zz = 0; zz < 16; zz++) {
            for (int yy = 0; yy < 8; yy++) {
               if (grid[(xx * 16 + zz) * 8 + yy]) {
                  BlockPos placePos = origin.offset(xx, yy, zz);
                  if (this.canReplaceWithAirOrFluid.test(level, placePos)) {
                     boolean placeAir = yy >= 4;
                     level.setBlock(placePos, placeAir ? AIR : fluid, 2);
                     if (placeAir) {
                        level.scheduleTick(placePos, AIR.getBlock(), 0);
                        this.markAboveForPostProcessing(level, placePos);
                     }
                  }
               }
            }
         }
      }

      BlockState barrier = this.barrier.getState(level, random, origin);
      if (!barrier.isAir()) {
         for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
               for (int yy = 0; yy < 8; yy++) {
                  boolean check = !grid[(xx * 16 + zz) * 8 + yy]
                     && (
                        xx < 15 && grid[((xx + 1) * 16 + zz) * 8 + yy]
                           || xx > 0 && grid[((xx - 1) * 16 + zz) * 8 + yy]
                           || zz < 15 && grid[(xx * 16 + zz + 1) * 8 + yy]
                           || zz > 0 && grid[(xx * 16 + (zz - 1)) * 8 + yy]
                           || yy < 7 && grid[(xx * 16 + zz) * 8 + yy + 1]
                           || yy > 0 && grid[(xx * 16 + zz) * 8 + (yy - 1)]
                     );
                  if (check && (yy < 4 || random.nextInt(2) != 0)) {
                     BlockPos offset = origin.offset(xx, yy, zz);
                     BlockState blockState = level.getBlockState(offset);
                     if (blockState.isSolid() && this.canReplaceWithBarrier.test(level, offset)) {
                        BlockPos barrierPos = origin.offset(xx, yy, zz);
                        level.setBlock(barrierPos, barrier, 2);
                        this.markAboveForPostProcessing(level, barrierPos);
                     }
                  }
               }
            }
         }
      }

      if (fluid.getFluidState().is(FluidTags.WATER)) {
         for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
               int yy = 4;
               BlockPos offset = origin.offset(xx, 4, zz);
               if (level.getBiome(offset).value().shouldFreeze(level, offset, false) && this.canReplaceWithAirOrFluid.test(level, offset)) {
                  level.setBlock(offset, Blocks.ICE.defaultBlockState(), 2);
               }
            }
         }
      }

      return true;
   }
}
