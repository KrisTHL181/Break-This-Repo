package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CanyonWorldCarver extends WorldCarver<CanyonCarverConfiguration> {
   public CanyonWorldCarver(Codec<CanyonCarverConfiguration> p_64711_) {
      super(p_64711_);
   }

   public boolean isStartChunk(CanyonCarverConfiguration p_224797_, RandomSource p_224798_) {
      return p_224798_.nextFloat() <= p_224797_.probability;
   }

   public boolean carve(
      CarvingContext p_224813_,
      CanyonCarverConfiguration p_224814_,
      ChunkAccess p_224815_,
      Function<BlockPos, Holder<Biome>> p_224816_,
      RandomSource p_224817_,
      Aquifer p_224818_,
      ChunkPos p_224819_,
      CarvingMask p_224820_
   ) {
      int i = (this.getRange() * 2 - 1) * 16;
      double d0 = p_224819_.getBlockX(p_224817_.nextInt(16));
      int j = p_224814_.y.sample(p_224817_, p_224813_);
      double d1 = p_224819_.getBlockZ(p_224817_.nextInt(16));
      float f = p_224817_.nextFloat() * (float) (Math.PI * 2);
      float f1 = p_224814_.verticalRotation.sample(p_224817_);
      double d2 = p_224814_.yScale.sample(p_224817_);
      float f2 = p_224814_.shape.thickness.sample(p_224817_);
      int k = (int)(i * p_224814_.shape.distanceFactor.sample(p_224817_));
      int l = 0;
      this.doCarve(p_224813_, p_224814_, p_224815_, p_224816_, p_224817_.nextLong(), p_224818_, d0, j, d1, f2, f, f1, 0, k, d2, p_224820_);
      return true;
   }

   private void doCarve(
      CarvingContext p_190594_,
      CanyonCarverConfiguration p_190595_,
      ChunkAccess p_190596_,
      Function<BlockPos, Holder<Biome>> p_190597_,
      long p_190598_,
      Aquifer p_190599_,
      double p_190600_,
      double p_190601_,
      double p_190602_,
      float p_190603_,
      float p_190604_,
      float p_190605_,
      int p_190606_,
      int p_190607_,
      double p_190608_,
      CarvingMask p_190609_
   ) {
      RandomSource randomsource = RandomSource.create(p_190598_);
      float[] afloat = this.initWidthFactors(p_190594_, p_190595_, randomsource);
      float f = 0.0F;
      float f1 = 0.0F;

      for (int i = p_190606_; i < p_190607_; i++) {
         double d0 = 1.5 + Mth.sin(i * (float) Math.PI / p_190607_) * p_190603_;
         double d1 = d0 * p_190608_;
         d0 *= p_190595_.shape.horizontalRadiusFactor.sample(randomsource);
         d1 = this.updateVerticalRadius(p_190595_, randomsource, d1, p_190607_, i);
         float f2 = Mth.cos(p_190605_);
         float f3 = Mth.sin(p_190605_);
         p_190600_ += Mth.cos(p_190604_) * f2;
         p_190601_ += f3;
         p_190602_ += Mth.sin(p_190604_) * f2;
         p_190605_ *= 0.7F;
         p_190605_ += f1 * 0.05F;
         p_190604_ += f * 0.05F;
         f1 *= 0.8F;
         f *= 0.5F;
         f1 += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 2.0F;
         f += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 4.0F;
         if (randomsource.nextInt(4) != 0) {
            if (!canReach(p_190596_.getPos(), p_190600_, p_190602_, i, p_190607_, p_190603_)) {
               return;
            }

            this.carveEllipsoid(
               p_190594_,
               p_190595_,
               p_190596_,
               p_190597_,
               p_190599_,
               p_190600_,
               p_190601_,
               p_190602_,
               d0,
               d1,
               p_190609_,
               (p_159082_, p_159083_, p_159084_, p_159085_, p_159086_) -> this.shouldSkip(p_159082_, afloat, p_159083_, p_159084_, p_159085_, p_159086_)
            );
         }
      }
   }

   private float[] initWidthFactors(CarvingContext p_224809_, CanyonCarverConfiguration p_224810_, RandomSource p_224811_) {
      int i = p_224809_.getGenDepth();
      float[] afloat = new float[i];
      float f = 1.0F;

      for (int j = 0; j < i; j++) {
         if (j == 0 || p_224811_.nextInt(p_224810_.shape.widthSmoothness) == 0) {
            f = 1.0F + p_224811_.nextFloat() * p_224811_.nextFloat();
         }

         afloat[j] = f * f;
      }

      return afloat;
   }

   private double updateVerticalRadius(CanyonCarverConfiguration p_224800_, RandomSource p_224801_, double p_224802_, float p_224803_, float p_224804_) {
      float f = 1.0F - Mth.abs(0.5F - p_224804_ / p_224803_) * 2.0F;
      float f1 = p_224800_.shape.verticalRadiusDefaultFactor + p_224800_.shape.verticalRadiusCenterFactor * f;
      return f1 * p_224802_ * Mth.randomBetween(p_224801_, 0.75F, 1.0F);
   }

   private boolean shouldSkip(CarvingContext p_159074_, float[] p_159075_, double p_159076_, double p_159077_, double p_159078_, int p_159079_) {
      int i = p_159079_ - p_159074_.getMinGenY();
      return (p_159076_ * p_159076_ + p_159078_ * p_159078_) * p_159075_[i - 1] + p_159077_ * p_159077_ / 6.0 >= 1.0;
   }
}
