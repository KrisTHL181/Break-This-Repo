package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

public class DarkOakFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<DarkOakFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(i -> foliagePlacerParts(i).apply(i, DarkOakFoliagePlacer::new));

   public DarkOakFoliagePlacer(final IntProvider radius, final IntProvider offset) {
      super(radius, offset);
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.DARK_OAK_FOLIAGE_PLACER;
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
      BlockPos pos = foliageAttachment.pos().above(offset);
      boolean doubleTrunk = foliageAttachment.doubleTrunk();
      if (doubleTrunk) {
         this.placeLeavesRow(level, foliageSetter, random, tree, pos, leafRadius + 2, -1, doubleTrunk);
         this.placeLeavesRow(level, foliageSetter, random, tree, pos, leafRadius + 3, 0, doubleTrunk);
         this.placeLeavesRow(level, foliageSetter, random, tree, pos, leafRadius + 2, 1, doubleTrunk);
         if (random.nextBoolean()) {
            this.placeLeavesRow(level, foliageSetter, random, tree, pos, leafRadius, 2, doubleTrunk);
         }
      } else {
         this.placeLeavesRow(level, foliageSetter, random, tree, pos, leafRadius + 2, -1, doubleTrunk);
         this.placeLeavesRow(level, foliageSetter, random, tree, pos, leafRadius + 1, 0, doubleTrunk);
      }
   }

   @Override
   public int foliageHeight(final RandomSource random, final int treeHeight, final TreeFeature tree) {
      return 4;
   }

   @Override
   protected boolean shouldSkipLocationSigned(
      final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk
   ) {
      return y != 0 || !doubleTrunk || dx != -currentRadius && dx < currentRadius || dz != -currentRadius && dz < currentRadius
         ? super.shouldSkipLocationSigned(random, dx, y, dz, currentRadius, doubleTrunk)
         : true;
   }

   @Override
   protected boolean shouldSkipLocation(final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk) {
      if (y == -1 && !doubleTrunk) {
         return dx == currentRadius && dz == currentRadius;
      } else {
         return y == 1 ? dx + dz > currentRadius * 2 - 2 : false;
      }
   }
}
