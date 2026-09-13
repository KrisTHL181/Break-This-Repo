package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class CherryFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<CherryFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_273246_ -> foliagePlacerParts(p_273246_)
         .and(
            p_273246_.group(
               IntProvider.codec(4, 16).fieldOf("height").forGetter(p_273527_ -> p_273527_.height),
               Codec.floatRange(0.0F, 1.0F).fieldOf("wide_bottom_layer_hole_chance").forGetter(p_273760_ -> p_273760_.wideBottomLayerHoleChance),
               Codec.floatRange(0.0F, 1.0F).fieldOf("corner_hole_chance").forGetter(p_273020_ -> p_273020_.wideBottomLayerHoleChance),
               Codec.floatRange(0.0F, 1.0F).fieldOf("hanging_leaves_chance").forGetter(p_273148_ -> p_273148_.hangingLeavesChance),
               Codec.floatRange(0.0F, 1.0F).fieldOf("hanging_leaves_extension_chance").forGetter(p_273098_ -> p_273098_.hangingLeavesExtensionChance)
            )
         )
         .apply(p_273246_, CherryFoliagePlacer::new)
   );
   private final IntProvider height;
   private final float wideBottomLayerHoleChance;
   private final float cornerHoleChance;
   private final float hangingLeavesChance;
   private final float hangingLeavesExtensionChance;

   public CherryFoliagePlacer(
      IntProvider p_272646_, IntProvider p_272802_, IntProvider p_273604_, float p_272737_, float p_273720_, float p_273152_, float p_273529_
   ) {
      super(p_272646_, p_272802_);
      this.height = p_273604_;
      this.wideBottomLayerHoleChance = p_272737_;
      this.cornerHoleChance = p_273720_;
      this.hangingLeavesChance = p_273152_;
      this.hangingLeavesExtensionChance = p_273529_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.CHERRY_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader p_272723_,
      FoliagePlacer.FoliageSetter p_273410_,
      RandomSource p_273057_,
      TreeConfiguration p_273220_,
      int p_272975_,
      FoliagePlacer.FoliageAttachment p_273037_,
      int p_273647_,
      int p_273700_,
      int p_273188_
   ) {
      boolean flag = p_273037_.doubleTrunk();
      BlockPos blockpos = p_273037_.pos().above(p_273188_);
      int i = p_273700_ + p_273037_.radiusOffset() - 1;
      this.placeLeavesRow(p_272723_, p_273410_, p_273057_, p_273220_, blockpos, i - 2, p_273647_ - 3, flag);
      this.placeLeavesRow(p_272723_, p_273410_, p_273057_, p_273220_, blockpos, i - 1, p_273647_ - 4, flag);

      for (int j = p_273647_ - 5; j >= 0; j--) {
         this.placeLeavesRow(p_272723_, p_273410_, p_273057_, p_273220_, blockpos, i, j, flag);
      }

      this.placeLeavesRowWithHangingLeavesBelow(
         p_272723_, p_273410_, p_273057_, p_273220_, blockpos, i, -1, flag, this.hangingLeavesChance, this.hangingLeavesExtensionChance
      );
      this.placeLeavesRowWithHangingLeavesBelow(
         p_272723_, p_273410_, p_273057_, p_273220_, blockpos, i - 1, -2, flag, this.hangingLeavesChance, this.hangingLeavesExtensionChance
      );
   }

   @Override
   public int foliageHeight(RandomSource p_273679_, int p_273336_, TreeConfiguration p_273643_) {
      return this.height.sample(p_273679_);
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource p_273294_, int p_273380_, int p_272865_, int p_272853_, int p_272631_, boolean p_273432_) {
      if (p_272865_ == -1 && (p_273380_ == p_272631_ || p_272853_ == p_272631_) && p_273294_.nextFloat() < this.wideBottomLayerHoleChance) {
         return true;
      }

      boolean flag = p_273380_ == p_272631_ && p_272853_ == p_272631_;
      boolean flag1 = p_272631_ > 2;
      return flag1
         ? flag || p_273380_ + p_272853_ > p_272631_ * 2 - 2 && p_273294_.nextFloat() < this.cornerHoleChance
         : flag && p_273294_.nextFloat() < this.cornerHoleChance;
   }
}
