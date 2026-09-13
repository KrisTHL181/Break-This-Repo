package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

public class AcaciaFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<AcaciaFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(i -> foliagePlacerParts(i).apply(i, AcaciaFoliagePlacer::new));

   public AcaciaFoliagePlacer(final IntProvider radius, final IntProvider offset) {
      super(radius, offset);
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.ACACIA_FOLIAGE_PLACER;
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
      boolean doubleTrunk = foliageAttachment.doubleTrunk();
      BlockPos foliagePos = foliageAttachment.pos().above(offset);
      int foliageHeightWithOffset = foliageHeight + foliageAttachment.foliageHeightOffset();
      this.placeLeavesRow(
         level, foliageSetter, random, tree, foliagePos, leafRadius + foliageAttachment.radiusOffsetXZ(), -1 - foliageHeightWithOffset, doubleTrunk
      );
      this.placeLeavesRow(level, foliageSetter, random, tree, foliagePos, leafRadius - 1, -foliageHeightWithOffset, doubleTrunk);
      this.placeLeavesRow(level, foliageSetter, random, tree, foliagePos, leafRadius + foliageAttachment.radiusOffsetXZ() - 1, 0, doubleTrunk);
   }

   @Override
   public int foliageHeight(final RandomSource random, final int treeHeight, final TreeFeature tree) {
      return 0;
   }

   @Override
   protected boolean shouldSkipLocation(final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk) {
      return y == 0 ? (dx > 1 || dz > 1) && dx != 0 && dz != 0 : dx == currentRadius && dz == currentRadius && currentRadius > 0;
   }
}
