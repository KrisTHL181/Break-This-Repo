package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

public class SpruceFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<SpruceFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      i -> foliagePlacerParts(i).and(IntProviders.codec(0, 24).fieldOf("trunk_height").forGetter(p -> p.trunkHeight)).apply(i, SpruceFoliagePlacer::new)
   );
   private final IntProvider trunkHeight;

   public SpruceFoliagePlacer(final IntProvider radius, final IntProvider offset, final IntProvider trunkHeight) {
      super(radius, offset);
      this.trunkHeight = trunkHeight;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.SPRUCE_FOLIAGE_PLACER;
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
      BlockPos foliagePos = foliageAttachment.pos();
      int currentRadius = random.nextInt(2);
      int maxRadius = 1;
      int minRadius = 0;
      int foliageHeightWithOffset = foliageHeight + foliageAttachment.foliageHeightOffset();

      for (int yo = offset; yo >= -foliageHeightWithOffset; yo--) {
         this.placeLeavesRow(level, foliageSetter, random, tree, foliagePos, currentRadius, yo, foliageAttachment.doubleTrunk());
         if (currentRadius >= maxRadius) {
            currentRadius = minRadius;
            minRadius = 1;
            maxRadius = Math.min(maxRadius + 1, leafRadius + foliageAttachment.radiusOffsetXZ());
         } else {
            currentRadius++;
         }
      }
   }

   @Override
   public int foliageHeight(final RandomSource random, final int treeHeight, final TreeFeature tree) {
      return Math.max(4, treeHeight - this.trunkHeight.sample(random));
   }

   @Override
   protected boolean shouldSkipLocation(final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk) {
      return dx == currentRadius && dz == currentRadius && currentRadius > 0;
   }
}
