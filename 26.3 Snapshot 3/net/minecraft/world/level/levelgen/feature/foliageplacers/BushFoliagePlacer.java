package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

public class BushFoliagePlacer extends BlobFoliagePlacer {
   public static final MapCodec<BushFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(i -> blobParts(i).apply(i, BushFoliagePlacer::new));

   public BushFoliagePlacer(final IntProvider radius, final IntProvider offset, final int height) {
      super(radius, offset, height);
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.BUSH_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      final WorldGenLevel level,
      final FoliagePlacer.FoliageSetter foliageSetter,
      final RandomSource random,
      final TreeFeature tree,
      final int treeHeight,
      final FoliagePlacer.FoliageAttachment foliageAttachment,
      final int foliageHeight,
      final int leafRadius,
      final int offset
   ) {
      int foliageHeightWithOffset = foliageHeight + foliageAttachment.foliageHeightOffset();

      for (int yo = offset; yo >= offset - foliageHeightWithOffset; yo--) {
         int currentRadius = leafRadius + foliageAttachment.radiusOffsetXZ() - 1 - yo;
         this.placeLeavesRow(level, foliageSetter, random, tree, foliageAttachment.pos(), currentRadius, yo, foliageAttachment.doubleTrunk());
      }
   }

   @Override
   protected boolean shouldSkipLocation(final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk) {
      return dx == currentRadius && dz == currentRadius && random.nextInt(2) == 0;
   }
}
