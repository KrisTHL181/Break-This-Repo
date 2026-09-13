package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.Products.P3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class BlobFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<BlobFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(p_68427_ -> blobParts(p_68427_).apply(p_68427_, BlobFoliagePlacer::new));
   protected final int height;

   protected static <P extends BlobFoliagePlacer> P3<Mu<P>, IntProvider, IntProvider, Integer> blobParts(Instance<P> p_68414_) {
      return foliagePlacerParts(p_68414_).and(Codec.intRange(0, 16).fieldOf("height").forGetter(p_68412_ -> p_68412_.height));
   }

   public BlobFoliagePlacer(IntProvider p_161356_, IntProvider p_161357_, int p_161358_) {
      super(p_161356_, p_161357_);
      this.height = p_161358_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.BLOB_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader p_273066_,
      FoliagePlacer.FoliageSetter p_272716_,
      RandomSource p_273178_,
      TreeConfiguration p_272850_,
      int p_273067_,
      FoliagePlacer.FoliageAttachment p_273711_,
      int p_273580_,
      int p_273511_,
      int p_273685_
   ) {
      for (int i = p_273685_; i >= p_273685_ - p_273580_; i--) {
         int j = Math.max(p_273511_ + p_273711_.radiusOffset() - 1 - i / 2, 0);
         this.placeLeavesRow(p_273066_, p_272716_, p_273178_, p_272850_, p_273711_.pos(), j, i, p_273711_.doubleTrunk());
      }
   }

   @Override
   public int foliageHeight(RandomSource p_225516_, int p_225517_, TreeConfiguration p_225518_) {
      return this.height;
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource p_225509_, int p_225510_, int p_225511_, int p_225512_, int p_225513_, boolean p_225514_) {
      return p_225510_ == p_225513_ && p_225512_ == p_225513_ && (p_225509_.nextInt(2) == 0 || p_225511_ == 0);
   }
}
