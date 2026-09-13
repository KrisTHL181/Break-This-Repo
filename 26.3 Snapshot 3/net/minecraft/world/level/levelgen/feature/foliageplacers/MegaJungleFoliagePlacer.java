package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

public class MegaJungleFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<MegaJungleFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      i -> foliagePlacerParts(i).and(Codec.intRange(0, 16).fieldOf("height").forGetter(p -> p.height)).apply(i, MegaJungleFoliagePlacer::new)
   );
   protected final int height;

   public MegaJungleFoliagePlacer(final IntProvider radius, final IntProvider offset, final int height) {
      super(radius, offset);
      this.height = height;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.MEGA_JUNGLE_FOLIAGE_PLACER;
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
      int leafHeight = (foliageAttachment.doubleTrunk() ? foliageHeight : 1 + random.nextInt(2)) + foliageAttachment.foliageHeightOffset();

      for (int yo = offset; yo >= offset - leafHeight; yo--) {
         int currentRadius = leafRadius + foliageAttachment.radiusOffsetXZ() + 1 - yo;
         this.placeLeavesRow(level, foliageSetter, random, tree, foliageAttachment.pos(), currentRadius, yo, foliageAttachment.doubleTrunk());
      }
   }

   @Override
   public int foliageHeight(final RandomSource random, final int treeHeight, final TreeFeature tree) {
      return this.height;
   }

   @Override
   protected boolean shouldSkipLocation(final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk) {
      return dx + dz >= 7 ? true : dx * dx + dz * dz > currentRadius * currentRadius;
   }
}
