package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class MegaJungleFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<MegaJungleFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_68630_ -> foliagePlacerParts(p_68630_)
         .and(Codec.intRange(0, 16).fieldOf("height").forGetter(p_161468_ -> p_161468_.height))
         .apply(p_68630_, MegaJungleFoliagePlacer::new)
   );
   protected final int height;

   public MegaJungleFoliagePlacer(IntProvider p_161454_, IntProvider p_161455_, int p_161456_) {
      super(p_161454_, p_161455_);
      this.height = p_161456_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.MEGA_JUNGLE_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader p_225657_,
      FoliagePlacer.FoliageSetter p_273447_,
      RandomSource p_225659_,
      TreeConfiguration p_225660_,
      int p_225661_,
      FoliagePlacer.FoliageAttachment p_225662_,
      int p_225663_,
      int p_225664_,
      int p_225665_
   ) {
      int i = p_225662_.doubleTrunk() ? p_225663_ : 1 + p_225659_.nextInt(2);

      for (int j = p_225665_; j >= p_225665_ - i; j--) {
         int k = p_225664_ + p_225662_.radiusOffset() + 1 - j;
         this.placeLeavesRow(p_225657_, p_273447_, p_225659_, p_225660_, p_225662_.pos(), k, j, p_225662_.doubleTrunk());
      }
   }

   @Override
   public int foliageHeight(RandomSource p_225653_, int p_225654_, TreeConfiguration p_225655_) {
      return this.height;
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource p_225646_, int p_225647_, int p_225648_, int p_225649_, int p_225650_, boolean p_225651_) {
      return p_225647_ + p_225649_ >= 7 ? true : p_225647_ * p_225647_ + p_225649_ * p_225649_ > p_225650_ * p_225650_;
   }
}
