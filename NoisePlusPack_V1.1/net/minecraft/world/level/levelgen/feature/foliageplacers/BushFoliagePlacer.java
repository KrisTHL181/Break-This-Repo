package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class BushFoliagePlacer extends BlobFoliagePlacer {
   public static final MapCodec<BushFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(p_68454_ -> blobParts(p_68454_).apply(p_68454_, BushFoliagePlacer::new));

   public BushFoliagePlacer(IntProvider p_161370_, IntProvider p_161371_, int p_161372_) {
      super(p_161370_, p_161371_, p_161372_);
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.BUSH_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader p_273251_,
      FoliagePlacer.FoliageSetter p_273782_,
      RandomSource p_273626_,
      TreeConfiguration p_272956_,
      int p_273384_,
      FoliagePlacer.FoliageAttachment p_273459_,
      int p_273161_,
      int p_272989_,
      int p_273166_
   ) {
      for (int i = p_273166_; i >= p_273166_ - p_273161_; i--) {
         int j = p_272989_ + p_273459_.radiusOffset() - 1 - i;
         this.placeLeavesRow(p_273251_, p_273782_, p_273626_, p_272956_, p_273459_.pos(), j, i, p_273459_.doubleTrunk());
      }
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource p_225530_, int p_225531_, int p_225532_, int p_225533_, int p_225534_, boolean p_225535_) {
      return p_225531_ == p_225534_ && p_225533_ == p_225534_ && p_225530_.nextInt(2) == 0;
   }
}
