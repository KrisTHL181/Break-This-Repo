package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CaveWorldCarver extends WorldCarver<CaveCarverConfiguration> {
   public CaveWorldCarver(Codec<CaveCarverConfiguration> p_159194_) {
      super(p_159194_);
   }

   public boolean isStartChunk(CaveCarverConfiguration p_224894_, RandomSource p_224895_) {
      return p_224895_.nextFloat() <= p_224894_.probability;
   }

   public boolean carve(
      CarvingContext p_224885_,
      CaveCarverConfiguration p_224886_,
      ChunkAccess p_224887_,
      Function<BlockPos, Holder<Biome>> p_224888_,
      RandomSource p_224889_,
      Aquifer p_224890_,
      ChunkPos p_224891_,
      CarvingMask p_224892_
   ) {
      int i = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
      int j = p_224889_.nextInt(p_224889_.nextInt(p_224889_.nextInt(this.getCaveBound()) + 1) + 1);

      for (int k = 0; k < j; k++) {
         double d0 = p_224891_.getBlockX(p_224889_.nextInt(16));
         double d1 = p_224886_.y.sample(p_224889_, p_224885_);
         double d2 = p_224891_.getBlockZ(p_224889_.nextInt(16));
         double d3 = p_224886_.horizontalRadiusMultiplier.sample(p_224889_);
         double d4 = p_224886_.verticalRadiusMultiplier.sample(p_224889_);
         double d5 = p_224886_.floorLevel.sample(p_224889_);
         WorldCarver.CarveSkipChecker worldcarver$carveskipchecker = (p_159202_, p_159203_, p_159204_, p_159205_, p_159206_) -> shouldSkip(
            p_159203_, p_159204_, p_159205_, d5
         );
         int l = 1;
         if (p_224889_.nextInt(4) == 0) {
            double d6 = p_224886_.yScale.sample(p_224889_);
            float f1 = 1.0F + p_224889_.nextFloat() * 6.0F;
            this.createRoom(p_224885_, p_224886_, p_224887_, p_224888_, p_224890_, d0, d1, d2, f1, d6, p_224892_, worldcarver$carveskipchecker);
            l += p_224889_.nextInt(4);
         }

         for (int k1 = 0; k1 < l; k1++) {
            float f = p_224889_.nextFloat() * (float) (Math.PI * 2);
            float f3 = (p_224889_.nextFloat() - 0.5F) / 4.0F;
            float f2 = this.getThickness(p_224889_);
            int i1 = i - p_224889_.nextInt(i / 4);
            int j1 = 0;
            this.createTunnel(
               p_224885_,
               p_224886_,
               p_224887_,
               p_224888_,
               p_224889_.nextLong(),
               p_224890_,
               d0,
               d1,
               d2,
               d3,
               d4,
               f2,
               f,
               f3,
               0,
               i1,
               this.getYScale(),
               p_224892_,
               worldcarver$carveskipchecker
            );
         }
      }

      return true;
   }

   protected int getCaveBound() {
      return 15;
   }

   protected float getThickness(RandomSource p_224871_) {
      float f = p_224871_.nextFloat() * 2.0F + p_224871_.nextFloat();
      if (p_224871_.nextInt(10) == 0) {
         f *= p_224871_.nextFloat() * p_224871_.nextFloat() * 3.0F + 1.0F;
      }

      return f;
   }

   protected double getYScale() {
      return 1.0;
   }

   protected void createRoom(
      CarvingContext p_190691_,
      CaveCarverConfiguration p_190692_,
      ChunkAccess p_190693_,
      Function<BlockPos, Holder<Biome>> p_190694_,
      Aquifer p_190695_,
      double p_190696_,
      double p_190697_,
      double p_190698_,
      float p_190699_,
      double p_190700_,
      CarvingMask p_190701_,
      WorldCarver.CarveSkipChecker p_190702_
   ) {
      double d0 = 1.5 + Mth.sin((float) (Math.PI / 2)) * p_190699_;
      double d1 = d0 * p_190700_;
      this.carveEllipsoid(p_190691_, p_190692_, p_190693_, p_190694_, p_190695_, p_190696_ + 1.0, p_190697_, p_190698_, d0, d1, p_190701_, p_190702_);
   }

   protected void createTunnel(
      CarvingContext p_190671_,
      CaveCarverConfiguration p_190672_,
      ChunkAccess p_190673_,
      Function<BlockPos, Holder<Biome>> p_190674_,
      long p_190675_,
      Aquifer p_190676_,
      double p_190677_,
      double p_190678_,
      double p_190679_,
      double p_190680_,
      double p_190681_,
      float p_190682_,
      float p_190683_,
      float p_190684_,
      int p_190685_,
      int p_190686_,
      double p_190687_,
      CarvingMask p_190688_,
      WorldCarver.CarveSkipChecker p_190689_
   ) {
      RandomSource randomsource = RandomSource.create(p_190675_);
      int i = randomsource.nextInt(p_190686_ / 2) + p_190686_ / 4;
      boolean flag = randomsource.nextInt(6) == 0;
      float f = 0.0F;
      float f1 = 0.0F;

      for (int j = p_190685_; j < p_190686_; j++) {
         double d0 = 1.5 + Mth.sin((float) Math.PI * j / p_190686_) * p_190682_;
         double d1 = d0 * p_190687_;
         float f2 = Mth.cos(p_190684_);
         p_190677_ += Mth.cos(p_190683_) * f2;
         p_190678_ += Mth.sin(p_190684_);
         p_190679_ += Mth.sin(p_190683_) * f2;
         p_190684_ *= flag ? 0.92F : 0.7F;
         p_190684_ += f1 * 0.1F;
         p_190683_ += f * 0.1F;
         f1 *= 0.9F;
         f *= 0.75F;
         f1 += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 2.0F;
         f += (randomsource.nextFloat() - randomsource.nextFloat()) * randomsource.nextFloat() * 4.0F;
         if (j == i && p_190682_ > 1.0F) {
            this.createTunnel(
               p_190671_,
               p_190672_,
               p_190673_,
               p_190674_,
               randomsource.nextLong(),
               p_190676_,
               p_190677_,
               p_190678_,
               p_190679_,
               p_190680_,
               p_190681_,
               randomsource.nextFloat() * 0.5F + 0.5F,
               p_190683_ - (float) (Math.PI / 2),
               p_190684_ / 3.0F,
               j,
               p_190686_,
               1.0,
               p_190688_,
               p_190689_
            );
            this.createTunnel(
               p_190671_,
               p_190672_,
               p_190673_,
               p_190674_,
               randomsource.nextLong(),
               p_190676_,
               p_190677_,
               p_190678_,
               p_190679_,
               p_190680_,
               p_190681_,
               randomsource.nextFloat() * 0.5F + 0.5F,
               p_190683_ + (float) (Math.PI / 2),
               p_190684_ / 3.0F,
               j,
               p_190686_,
               1.0,
               p_190688_,
               p_190689_
            );
            return;
         }

         if (randomsource.nextInt(4) != 0) {
            if (!canReach(p_190673_.getPos(), p_190677_, p_190679_, j, p_190686_, p_190682_)) {
               return;
            }

            this.carveEllipsoid(
               p_190671_, p_190672_, p_190673_, p_190674_, p_190676_, p_190677_, p_190678_, p_190679_, d0 * p_190680_, d1 * p_190681_, p_190688_, p_190689_
            );
         }
      }
   }

   private static boolean shouldSkip(double p_159196_, double p_159197_, double p_159198_, double p_159199_) {
      return p_159197_ <= p_159199_ ? true : p_159196_ * p_159196_ + p_159197_ * p_159197_ + p_159198_ * p_159198_ >= 1.0;
   }
}
