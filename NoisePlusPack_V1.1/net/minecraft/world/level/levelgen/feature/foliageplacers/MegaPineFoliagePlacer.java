package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class MegaPineFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<MegaPineFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_68664_ -> foliagePlacerParts(p_68664_)
         .and(IntProvider.codec(0, 24).fieldOf("crown_height").forGetter(p_161484_ -> p_161484_.crownHeight))
         .apply(p_68664_, MegaPineFoliagePlacer::new)
   );
   private final IntProvider crownHeight;

   public MegaPineFoliagePlacer(IntProvider p_161470_, IntProvider p_161471_, IntProvider p_161472_) {
      super(p_161470_, p_161471_);
      this.crownHeight = p_161472_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.MEGA_PINE_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader p_225678_,
      FoliagePlacer.FoliageSetter p_273345_,
      RandomSource p_225680_,
      TreeConfiguration p_225681_,
      int p_225682_,
      FoliagePlacer.FoliageAttachment p_225683_,
      int p_225684_,
      int p_225685_,
      int p_225686_
   ) {
      BlockPos blockpos = p_225683_.pos();
      int i = 0;

      for (int j = blockpos.getY() - p_225684_ + p_225686_; j <= blockpos.getY() + p_225686_; j++) {
         int k = blockpos.getY() - j;
         int l = p_225685_ + p_225683_.radiusOffset() + Mth.floor((float)k / p_225684_ * 3.5F);
         int i1;
         if (k > 0 && l == i && (j & 1) == 0) {
            i1 = l + 1;
         } else {
            i1 = l;
         }

         this.placeLeavesRow(p_225678_, p_273345_, p_225680_, p_225681_, new BlockPos(blockpos.getX(), j, blockpos.getZ()), i1, 0, p_225683_.doubleTrunk());
         i = l;
      }
   }

   @Override
   public int foliageHeight(RandomSource p_225674_, int p_225675_, TreeConfiguration p_225676_) {
      return this.crownHeight.sample(p_225674_);
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource p_225667_, int p_225668_, int p_225669_, int p_225670_, int p_225671_, boolean p_225672_) {
      return p_225668_ + p_225670_ >= 7 ? true : p_225668_ * p_225668_ + p_225670_ * p_225670_ > p_225671_ * p_225671_;
   }
}
