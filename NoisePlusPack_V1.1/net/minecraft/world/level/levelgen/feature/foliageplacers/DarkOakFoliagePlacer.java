package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class DarkOakFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<DarkOakFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_68473_ -> foliagePlacerParts(p_68473_).apply(p_68473_, DarkOakFoliagePlacer::new)
   );

   public DarkOakFoliagePlacer(IntProvider p_161384_, IntProvider p_161385_) {
      super(p_161384_, p_161385_);
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.DARK_OAK_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader p_225558_,
      FoliagePlacer.FoliageSetter p_273641_,
      RandomSource p_225560_,
      TreeConfiguration p_225561_,
      int p_225562_,
      FoliagePlacer.FoliageAttachment p_225563_,
      int p_225564_,
      int p_225565_,
      int p_225566_
   ) {
      BlockPos blockpos = p_225563_.pos().above(p_225566_);
      boolean flag = p_225563_.doubleTrunk();
      if (flag) {
         this.placeLeavesRow(p_225558_, p_273641_, p_225560_, p_225561_, blockpos, p_225565_ + 2, -1, flag);
         this.placeLeavesRow(p_225558_, p_273641_, p_225560_, p_225561_, blockpos, p_225565_ + 3, 0, flag);
         this.placeLeavesRow(p_225558_, p_273641_, p_225560_, p_225561_, blockpos, p_225565_ + 2, 1, flag);
         if (p_225560_.nextBoolean()) {
            this.placeLeavesRow(p_225558_, p_273641_, p_225560_, p_225561_, blockpos, p_225565_, 2, flag);
         }
      } else {
         this.placeLeavesRow(p_225558_, p_273641_, p_225560_, p_225561_, blockpos, p_225565_ + 2, -1, flag);
         this.placeLeavesRow(p_225558_, p_273641_, p_225560_, p_225561_, blockpos, p_225565_ + 1, 0, flag);
      }
   }

   @Override
   public int foliageHeight(RandomSource p_225554_, int p_225555_, TreeConfiguration p_225556_) {
      return 4;
   }

   @Override
   protected boolean shouldSkipLocationSigned(RandomSource p_225568_, int p_225569_, int p_225570_, int p_225571_, int p_225572_, boolean p_225573_) {
      return p_225570_ == 0 && p_225573_ && (p_225569_ == -p_225572_ || p_225569_ >= p_225572_) && (p_225571_ == -p_225572_ || p_225571_ >= p_225572_)
         ? true
         : super.shouldSkipLocationSigned(p_225568_, p_225569_, p_225570_, p_225571_, p_225572_, p_225573_);
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource p_225547_, int p_225548_, int p_225549_, int p_225550_, int p_225551_, boolean p_225552_) {
      if (p_225549_ == -1 && !p_225552_) {
         return p_225548_ == p_225551_ && p_225550_ == p_225551_;
      } else {
         return p_225549_ == 1 ? p_225548_ + p_225550_ > p_225551_ * 2 - 2 : false;
      }
   }
}
