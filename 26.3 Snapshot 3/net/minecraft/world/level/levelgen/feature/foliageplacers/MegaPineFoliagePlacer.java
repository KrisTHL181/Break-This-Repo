package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

public class MegaPineFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<MegaPineFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      i -> foliagePlacerParts(i).and(IntProviders.codec(0, 24).fieldOf("crown_height").forGetter(p -> p.crownHeight)).apply(i, MegaPineFoliagePlacer::new)
   );
   private final IntProvider crownHeight;

   public MegaPineFoliagePlacer(final IntProvider radius, final IntProvider offset, final IntProvider crownHeight) {
      super(radius, offset);
      this.crownHeight = crownHeight;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.MEGA_PINE_FOLIAGE_PLACER;
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
      int prevRadius = 0;
      int foliageHeightWithOffset = foliageHeight + foliageAttachment.foliageHeightOffset();

      for (int yy = foliagePos.getY() - foliageHeightWithOffset + offset; yy <= foliagePos.getY() + offset; yy++) {
         int yo = foliagePos.getY() - yy;
         int smoothRadius = leafRadius + foliageAttachment.radiusOffsetXZ() + Mth.floor((float)yo / foliageHeightWithOffset * 3.5F);
         int jaggedRadius;
         if (yo > 0 && smoothRadius == prevRadius && (yy & 1) == 0) {
            jaggedRadius = smoothRadius + 1;
         } else {
            jaggedRadius = smoothRadius;
         }

         this.placeLeavesRow(
            level, foliageSetter, random, tree, new BlockPos(foliagePos.getX(), yy, foliagePos.getZ()), jaggedRadius, 0, foliageAttachment.doubleTrunk()
         );
         prevRadius = smoothRadius;
      }
   }

   @Override
   public int foliageHeight(final RandomSource random, final int treeHeight, final TreeFeature tree) {
      return this.crownHeight.sample(random);
   }

   @Override
   protected boolean shouldSkipLocation(final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk) {
      return dx + dz >= 7 ? true : dx * dx + dz * dz > currentRadius * currentRadius;
   }
}
