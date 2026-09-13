package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

public class IcebergFeature extends Feature<BlockStateConfiguration> {
   public IcebergFeature(Codec<BlockStateConfiguration> p_66017_) {
      super(p_66017_);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockStateConfiguration> p_159884_) {
      BlockPos blockpos = p_159884_.origin();
      WorldGenLevel worldgenlevel = p_159884_.level();
      blockpos = new BlockPos(blockpos.getX(), p_159884_.chunkGenerator().getSeaLevel(), blockpos.getZ());
      RandomSource randomsource = p_159884_.random();
      boolean flag = randomsource.nextDouble() > 0.7;
      BlockState blockstate = p_159884_.config().state;
      double d0 = randomsource.nextDouble() * 2.0 * Math.PI;
      int i = 11 - randomsource.nextInt(5);
      int j = 3 + randomsource.nextInt(3);
      boolean flag1 = randomsource.nextDouble() > 0.7;
      int k = 11;
      int l = flag1 ? randomsource.nextInt(6) + 6 : randomsource.nextInt(15) + 3;
      if (!flag1 && randomsource.nextDouble() > 0.9) {
         l += randomsource.nextInt(19) + 7;
      }

      int i1 = Math.min(l + randomsource.nextInt(11), 18);
      int j1 = Math.min(l + randomsource.nextInt(7) - randomsource.nextInt(5), 11);
      int k1 = flag1 ? i : 11;

      for (int l1 = -k1; l1 < k1; l1++) {
         for (int i2 = -k1; i2 < k1; i2++) {
            for (int j2 = 0; j2 < l; j2++) {
               int k2 = flag1 ? this.heightDependentRadiusEllipse(j2, l, j1) : this.heightDependentRadiusRound(randomsource, j2, l, j1);
               if (flag1 || l1 < k2) {
                  this.generateIcebergBlock(worldgenlevel, randomsource, blockpos, l, l1, j2, i2, k2, k1, flag1, j, d0, flag, blockstate);
               }
            }
         }
      }

      this.smooth(worldgenlevel, blockpos, j1, l, flag1, i);

      for (int i3 = -k1; i3 < k1; i3++) {
         for (int j3 = -k1; j3 < k1; j3++) {
            for (int k3 = -1; k3 > -i1; k3--) {
               int l3 = flag1 ? Mth.ceil(k1 * (1.0F - (float)Math.pow(k3, 2.0) / (i1 * 8.0F))) : k1;
               int l2 = this.heightDependentRadiusSteep(randomsource, -k3, i1, j1);
               if (i3 < l2) {
                  this.generateIcebergBlock(worldgenlevel, randomsource, blockpos, i1, i3, k3, j3, l2, l3, flag1, j, d0, flag, blockstate);
               }
            }
         }
      }

      boolean flag2 = flag1 ? randomsource.nextDouble() > 0.1 : randomsource.nextDouble() > 0.7;
      if (flag2) {
         this.generateCutOut(randomsource, worldgenlevel, j1, l, blockpos, flag1, i, d0, j);
      }

      return true;
   }

   private void generateCutOut(
      RandomSource p_225100_,
      LevelAccessor p_225101_,
      int p_225102_,
      int p_225103_,
      BlockPos p_225104_,
      boolean p_225105_,
      int p_225106_,
      double p_225107_,
      int p_225108_
   ) {
      int i = p_225100_.nextBoolean() ? -1 : 1;
      int j = p_225100_.nextBoolean() ? -1 : 1;
      int k = p_225100_.nextInt(Math.max(p_225102_ / 2 - 2, 1));
      if (p_225100_.nextBoolean()) {
         k = p_225102_ / 2 + 1 - p_225100_.nextInt(Math.max(p_225102_ - p_225102_ / 2 - 1, 1));
      }

      int l = p_225100_.nextInt(Math.max(p_225102_ / 2 - 2, 1));
      if (p_225100_.nextBoolean()) {
         l = p_225102_ / 2 + 1 - p_225100_.nextInt(Math.max(p_225102_ - p_225102_ / 2 - 1, 1));
      }

      if (p_225105_) {
         k = l = p_225100_.nextInt(Math.max(p_225106_ - 5, 1));
      }

      BlockPos blockpos = new BlockPos(i * k, 0, j * l);
      double d0 = p_225105_ ? p_225107_ + (Math.PI / 2) : p_225100_.nextDouble() * 2.0 * Math.PI;

      for (int i1 = 0; i1 < p_225103_ - 3; i1++) {
         int j1 = this.heightDependentRadiusRound(p_225100_, i1, p_225103_, p_225102_);
         this.carve(j1, i1, p_225104_, p_225101_, false, d0, blockpos, p_225106_, p_225108_);
      }

      for (int k1 = -1; k1 > -p_225103_ + p_225100_.nextInt(5); k1--) {
         int l1 = this.heightDependentRadiusSteep(p_225100_, -k1, p_225103_, p_225102_);
         this.carve(l1, k1, p_225104_, p_225101_, true, d0, blockpos, p_225106_, p_225108_);
      }
   }

   private void carve(
      int p_66036_, int p_66037_, BlockPos p_66038_, LevelAccessor p_66039_, boolean p_66040_, double p_66041_, BlockPos p_66042_, int p_66043_, int p_66044_
   ) {
      int i = p_66036_ + 1 + p_66043_ / 3;
      int j = Math.min(p_66036_ - 3, 3) + p_66044_ / 2 - 1;

      for (int k = -i; k < i; k++) {
         for (int l = -i; l < i; l++) {
            double d0 = this.signedDistanceEllipse(k, l, p_66042_, i, j, p_66041_);
            if (d0 < 0.0) {
               BlockPos blockpos = p_66038_.offset(k, p_66037_, l);
               BlockState blockstate = p_66039_.getBlockState(blockpos);
               if (isIcebergState(blockstate) || blockstate.is(Blocks.SNOW_BLOCK)) {
                  if (p_66040_) {
                     this.setBlock(p_66039_, blockpos, Blocks.WATER.defaultBlockState());
                  } else {
                     this.setBlock(p_66039_, blockpos, Blocks.AIR.defaultBlockState());
                     this.removeFloatingSnowLayer(p_66039_, blockpos);
                  }
               }
            }
         }
      }
   }

   private void removeFloatingSnowLayer(LevelAccessor p_66049_, BlockPos p_66050_) {
      if (p_66049_.getBlockState(p_66050_.above()).is(Blocks.SNOW)) {
         this.setBlock(p_66049_, p_66050_.above(), Blocks.AIR.defaultBlockState());
      }
   }

   private void generateIcebergBlock(
      LevelAccessor p_225110_,
      RandomSource p_225111_,
      BlockPos p_225112_,
      int p_225113_,
      int p_225114_,
      int p_225115_,
      int p_225116_,
      int p_225117_,
      int p_225118_,
      boolean p_225119_,
      int p_225120_,
      double p_225121_,
      boolean p_225122_,
      BlockState p_225123_
   ) {
      double d0 = p_225119_
         ? this.signedDistanceEllipse(p_225114_, p_225116_, BlockPos.ZERO, p_225118_, this.getEllipseC(p_225115_, p_225113_, p_225120_), p_225121_)
         : this.signedDistanceCircle(p_225114_, p_225116_, BlockPos.ZERO, p_225117_, p_225111_);
      if (d0 < 0.0) {
         BlockPos blockpos = p_225112_.offset(p_225114_, p_225115_, p_225116_);
         double d1 = p_225119_ ? -0.5 : -6 - p_225111_.nextInt(3);
         if (d0 > d1 && p_225111_.nextDouble() > 0.9) {
            return;
         }

         this.setIcebergBlock(blockpos, p_225110_, p_225111_, p_225113_ - p_225115_, p_225113_, p_225119_, p_225122_, p_225123_);
      }
   }

   private void setIcebergBlock(
      BlockPos p_225125_,
      LevelAccessor p_225126_,
      RandomSource p_225127_,
      int p_225128_,
      int p_225129_,
      boolean p_225130_,
      boolean p_225131_,
      BlockState p_225132_
   ) {
      BlockState blockstate = p_225126_.getBlockState(p_225125_);
      if (blockstate.isAir() || blockstate.is(Blocks.SNOW_BLOCK) || blockstate.is(Blocks.ICE) || blockstate.is(Blocks.WATER)) {
         boolean flag = !p_225130_ || p_225127_.nextDouble() > 0.05;
         int i = p_225130_ ? 3 : 2;
         if (p_225131_ && !blockstate.is(Blocks.WATER) && p_225128_ <= p_225127_.nextInt(Math.max(1, p_225129_ / i)) + p_225129_ * 0.6 && flag) {
            this.setBlock(p_225126_, p_225125_, Blocks.SNOW_BLOCK.defaultBlockState());
         } else {
            this.setBlock(p_225126_, p_225125_, p_225132_);
         }
      }
   }

   private int getEllipseC(int p_66019_, int p_66020_, int p_66021_) {
      int i = p_66021_;
      if (p_66019_ > 0 && p_66020_ - p_66019_ <= 3) {
         i = p_66021_ - (4 - (p_66020_ - p_66019_));
      }

      return i;
   }

   private double signedDistanceCircle(int p_225089_, int p_225090_, BlockPos p_225091_, int p_225092_, RandomSource p_225093_) {
      float f = 10.0F * Mth.clamp(p_225093_.nextFloat(), 0.2F, 0.8F) / p_225092_;
      return f + Math.pow(p_225089_ - p_225091_.getX(), 2.0) + Math.pow(p_225090_ - p_225091_.getZ(), 2.0) - Math.pow(p_225092_, 2.0);
   }

   private double signedDistanceEllipse(int p_66023_, int p_66024_, BlockPos p_66025_, int p_66026_, int p_66027_, double p_66028_) {
      return Math.pow(((p_66023_ - p_66025_.getX()) * Math.cos(p_66028_) - (p_66024_ - p_66025_.getZ()) * Math.sin(p_66028_)) / p_66026_, 2.0)
         + Math.pow(((p_66023_ - p_66025_.getX()) * Math.sin(p_66028_) + (p_66024_ - p_66025_.getZ()) * Math.cos(p_66028_)) / p_66027_, 2.0)
         - 1.0;
   }

   private int heightDependentRadiusRound(RandomSource p_225095_, int p_225096_, int p_225097_, int p_225098_) {
      float f = 3.5F - p_225095_.nextFloat();
      float f1 = (1.0F - (float)Math.pow(p_225096_, 2.0) / (p_225097_ * f)) * p_225098_;
      if (p_225097_ > 15 + p_225095_.nextInt(5)) {
         int i = p_225096_ < 3 + p_225095_.nextInt(6) ? p_225096_ / 2 : p_225096_;
         f1 = (1.0F - i / (p_225097_ * f * 0.4F)) * p_225098_;
      }

      return Mth.ceil(f1 / 2.0F);
   }

   private int heightDependentRadiusEllipse(int p_66110_, int p_66111_, int p_66112_) {
      float f = 1.0F;
      float f1 = (1.0F - (float)Math.pow(p_66110_, 2.0) / (p_66111_ * 1.0F)) * p_66112_;
      return Mth.ceil(f1 / 2.0F);
   }

   private int heightDependentRadiusSteep(RandomSource p_225134_, int p_225135_, int p_225136_, int p_225137_) {
      float f = 1.0F + p_225134_.nextFloat() / 2.0F;
      float f1 = (1.0F - p_225135_ / (p_225136_ * f)) * p_225137_;
      return Mth.ceil(f1 / 2.0F);
   }

   private static boolean isIcebergState(BlockState p_159886_) {
      return p_159886_.is(Blocks.PACKED_ICE) || p_159886_.is(Blocks.SNOW_BLOCK) || p_159886_.is(Blocks.BLUE_ICE);
   }

   private boolean belowIsAir(BlockGetter p_66046_, BlockPos p_66047_) {
      return p_66046_.getBlockState(p_66047_.below()).isAir();
   }

   private void smooth(LevelAccessor p_66052_, BlockPos p_66053_, int p_66054_, int p_66055_, boolean p_66056_, int p_66057_) {
      int i = p_66056_ ? p_66057_ : p_66054_ / 2;

      for (int j = -i; j <= i; j++) {
         for (int k = -i; k <= i; k++) {
            for (int l = 0; l <= p_66055_; l++) {
               BlockPos blockpos = p_66053_.offset(j, l, k);
               BlockState blockstate = p_66052_.getBlockState(blockpos);
               if (isIcebergState(blockstate) || blockstate.is(Blocks.SNOW)) {
                  if (this.belowIsAir(p_66052_, blockpos)) {
                     this.setBlock(p_66052_, blockpos, Blocks.AIR.defaultBlockState());
                     this.setBlock(p_66052_, blockpos.above(), Blocks.AIR.defaultBlockState());
                  } else if (isIcebergState(blockstate)) {
                     BlockState[] ablockstate = new BlockState[]{
                        p_66052_.getBlockState(blockpos.west()),
                        p_66052_.getBlockState(blockpos.east()),
                        p_66052_.getBlockState(blockpos.north()),
                        p_66052_.getBlockState(blockpos.south())
                     };
                     int i1 = 0;

                     for (BlockState blockstate1 : ablockstate) {
                        if (!isIcebergState(blockstate1)) {
                           i1++;
                        }
                     }

                     if (i1 >= 3) {
                        this.setBlock(p_66052_, blockpos, Blocks.AIR.defaultBlockState());
                     }
                  }
               }
            }
         }
      }
   }
}
