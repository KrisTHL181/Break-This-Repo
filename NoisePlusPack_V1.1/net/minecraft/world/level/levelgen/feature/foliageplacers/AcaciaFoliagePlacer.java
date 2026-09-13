package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class AcaciaFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<AcaciaFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_68380_ -> foliagePlacerParts(p_68380_).apply(p_68380_, AcaciaFoliagePlacer::new)
   );

   public AcaciaFoliagePlacer(IntProvider p_161343_, IntProvider p_161344_) {
      super(p_161343_, p_161344_);
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.ACACIA_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader p_225499_,
      FoliagePlacer.FoliageSetter p_273746_,
      RandomSource p_225501_,
      TreeConfiguration p_225502_,
      int p_225503_,
      FoliagePlacer.FoliageAttachment p_225504_,
      int p_225505_,
      int p_225506_,
      int p_225507_
   ) {
      boolean flag = p_225504_.doubleTrunk();
      BlockPos blockpos = p_225504_.pos().above(p_225507_);
      this.placeLeavesRow(p_225499_, p_273746_, p_225501_, p_225502_, blockpos, p_225506_ + p_225504_.radiusOffset(), -1 - p_225505_, flag);
      this.placeLeavesRow(p_225499_, p_273746_, p_225501_, p_225502_, blockpos, p_225506_ - 1, -p_225505_, flag);
      this.placeLeavesRow(p_225499_, p_273746_, p_225501_, p_225502_, blockpos, p_225506_ + p_225504_.radiusOffset() - 1, 0, flag);
   }

   @Override
   public int foliageHeight(RandomSource p_225495_, int p_225496_, TreeConfiguration p_225497_) {
      return 0;
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource p_225488_, int p_225489_, int p_225490_, int p_225491_, int p_225492_, boolean p_225493_) {
      return p_225490_ == 0
         ? (p_225489_ > 1 || p_225491_ > 1) && p_225489_ != 0 && p_225491_ != 0
         : p_225489_ == p_225492_ && p_225491_ == p_225492_ && p_225492_ > 0;
   }
}
