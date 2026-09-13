package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class HugeFungusFeature extends Feature<HugeFungusConfiguration> {
   private static final float HUGE_PROBABILITY = 0.06F;

   public HugeFungusFeature(Codec<HugeFungusConfiguration> p_65922_) {
      super(p_65922_);
   }

   @Override
   public boolean place(FeaturePlaceContext<HugeFungusConfiguration> p_159878_) {
      WorldGenLevel worldgenlevel = p_159878_.level();
      BlockPos blockpos = p_159878_.origin();
      RandomSource randomsource = p_159878_.random();
      ChunkGenerator chunkgenerator = p_159878_.chunkGenerator();
      HugeFungusConfiguration hugefungusconfiguration = p_159878_.config();
      Block block = hugefungusconfiguration.validBaseState.getBlock();
      BlockPos blockpos1 = null;
      BlockState blockstate = worldgenlevel.getBlockState(blockpos.below());
      if (blockstate.is(block)) {
         blockpos1 = blockpos;
      }

      if (blockpos1 == null) {
         return false;
      }

      int i = Mth.nextInt(randomsource, 4, 13);
      if (randomsource.nextInt(12) == 0) {
         i *= 2;
      }

      if (!hugefungusconfiguration.planted) {
         int j = chunkgenerator.getGenDepth();
         if (blockpos1.getY() + i + 1 >= j) {
            return false;
         }
      }

      boolean flag = !hugefungusconfiguration.planted && randomsource.nextFloat() < 0.06F;
      worldgenlevel.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 260);
      this.placeStem(worldgenlevel, randomsource, hugefungusconfiguration, blockpos1, i, flag);
      this.placeHat(worldgenlevel, randomsource, hugefungusconfiguration, blockpos1, i, flag);
      return true;
   }

   private static boolean isReplaceable(WorldGenLevel p_285049_, BlockPos p_285309_, HugeFungusConfiguration p_284992_, boolean p_285162_) {
      if (p_285049_.isStateAtPosition(p_285309_, BlockBehaviour.BlockStateBase::canBeReplaced)) {
         return true;
      } else {
         return p_285162_ ? p_284992_.replaceableBlocks.test(p_285049_, p_285309_) : false;
      }
   }

   private void placeStem(
      WorldGenLevel p_285364_, RandomSource p_285032_, HugeFungusConfiguration p_285198_, BlockPos p_285090_, int p_285249_, boolean p_285355_
   ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      BlockState blockstate = p_285198_.stemState;
      int i = p_285355_ ? 1 : 0;

      for (int j = -i; j <= i; j++) {
         for (int k = -i; k <= i; k++) {
            boolean flag = p_285355_ && Mth.abs(j) == i && Mth.abs(k) == i;

            for (int l = 0; l < p_285249_; l++) {
               blockpos$mutableblockpos.setWithOffset(p_285090_, j, l, k);
               if (isReplaceable(p_285364_, blockpos$mutableblockpos, p_285198_, true)) {
                  if (p_285198_.planted) {
                     if (!p_285364_.getBlockState(blockpos$mutableblockpos.below()).isAir()) {
                        p_285364_.destroyBlock(blockpos$mutableblockpos, true);
                     }

                     p_285364_.setBlock(blockpos$mutableblockpos, blockstate, 3);
                  } else if (flag) {
                     if (p_285032_.nextFloat() < 0.1F) {
                        this.setBlock(p_285364_, blockpos$mutableblockpos, blockstate);
                     }
                  } else {
                     this.setBlock(p_285364_, blockpos$mutableblockpos, blockstate);
                  }
               }
            }
         }
      }
   }

   private void placeHat(
      WorldGenLevel p_285200_, RandomSource p_285456_, HugeFungusConfiguration p_285146_, BlockPos p_285097_, int p_285156_, boolean p_285265_
   ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      boolean flag = p_285146_.hatState.is(Blocks.NETHER_WART_BLOCK);
      int i = Math.min(p_285456_.nextInt(1 + p_285156_ / 3) + 5, p_285156_);
      int j = p_285156_ - i;

      for (int k = j; k <= p_285156_; k++) {
         int l = k < p_285156_ - p_285456_.nextInt(3) ? 2 : 1;
         if (i > 8 && k < j + 4) {
            l = 3;
         }

         if (p_285265_) {
            l++;
         }

         for (int i1 = -l; i1 <= l; i1++) {
            for (int j1 = -l; j1 <= l; j1++) {
               boolean flag1 = i1 == -l || i1 == l;
               boolean flag2 = j1 == -l || j1 == l;
               boolean flag3 = !flag1 && !flag2 && k != p_285156_;
               boolean flag4 = flag1 && flag2;
               boolean flag5 = k < j + 3;
               blockpos$mutableblockpos.setWithOffset(p_285097_, i1, k, j1);
               if (isReplaceable(p_285200_, blockpos$mutableblockpos, p_285146_, false)) {
                  if (p_285146_.planted && !p_285200_.getBlockState(blockpos$mutableblockpos.below()).isAir()) {
                     p_285200_.destroyBlock(blockpos$mutableblockpos, true);
                  }

                  if (flag5) {
                     if (!flag3) {
                        this.placeHatDropBlock(p_285200_, p_285456_, blockpos$mutableblockpos, p_285146_.hatState, flag);
                     }
                  } else if (flag3) {
                     this.placeHatBlock(p_285200_, p_285456_, p_285146_, blockpos$mutableblockpos, 0.1F, 0.2F, flag ? 0.1F : 0.0F);
                  } else if (flag4) {
                     this.placeHatBlock(p_285200_, p_285456_, p_285146_, blockpos$mutableblockpos, 0.01F, 0.7F, flag ? 0.083F : 0.0F);
                  } else {
                     this.placeHatBlock(p_285200_, p_285456_, p_285146_, blockpos$mutableblockpos, 5.0E-4F, 0.98F, flag ? 0.07F : 0.0F);
                  }
               }
            }
         }
      }
   }

   private void placeHatBlock(
      LevelAccessor p_225050_,
      RandomSource p_225051_,
      HugeFungusConfiguration p_225052_,
      BlockPos.MutableBlockPos p_225053_,
      float p_225054_,
      float p_225055_,
      float p_225056_
   ) {
      if (p_225051_.nextFloat() < p_225054_) {
         this.setBlock(p_225050_, p_225053_, p_225052_.decorState);
      } else if (p_225051_.nextFloat() < p_225055_) {
         this.setBlock(p_225050_, p_225053_, p_225052_.hatState);
         if (p_225051_.nextFloat() < p_225056_) {
            tryPlaceWeepingVines(p_225053_, p_225050_, p_225051_);
         }
      }
   }

   private void placeHatDropBlock(LevelAccessor p_225065_, RandomSource p_225066_, BlockPos p_225067_, BlockState p_225068_, boolean p_225069_) {
      if (p_225065_.getBlockState(p_225067_.below()).is(p_225068_.getBlock())) {
         this.setBlock(p_225065_, p_225067_, p_225068_);
      } else if (p_225066_.nextFloat() < 0.15) {
         this.setBlock(p_225065_, p_225067_, p_225068_);
         if (p_225069_ && p_225066_.nextInt(11) == 0) {
            tryPlaceWeepingVines(p_225067_, p_225065_, p_225066_);
         }
      }
   }

   private static void tryPlaceWeepingVines(BlockPos p_225071_, LevelAccessor p_225072_, RandomSource p_225073_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_225071_.mutable().move(Direction.DOWN);
      if (p_225072_.isEmptyBlock(blockpos$mutableblockpos)) {
         int i = Mth.nextInt(p_225073_, 1, 5);
         if (p_225073_.nextInt(7) == 0) {
            i *= 2;
         }

         int j = 23;
         int k = 25;
         WeepingVinesFeature.placeWeepingVinesColumn(p_225072_, p_225073_, blockpos$mutableblockpos, i, 23, 25);
      }
   }
}
