package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

public class PoplarFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<PoplarFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      i -> foliagePlacerParts(i)
         .and(
            i.group(
               IntProviders.codec(5, 16).fieldOf("height").forGetter(p -> p.height),
               Codec.floatRange(0.0F, 1.0F).fieldOf("side_hole_chance").forGetter(p -> p.sideHoleChance)
            )
         )
         .apply(i, PoplarFoliagePlacer::new)
   );
   private final IntProvider height;
   private final float sideHoleChance;

   public PoplarFoliagePlacer(final IntProvider radius, final IntProvider offset, final IntProvider height, final float sideHoleChance) {
      super(radius, offset);
      this.height = height;
      this.sideHoleChance = sideHoleChance;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.POPLAR_FOLIAGE_PLACER;
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
      int currentRadius = leafRadius + foliageAttachment.radiusOffsetXZ() - 1;
      boolean flipRhombusShape = random.nextBoolean();
      int foliageHeightWithOffset = foliageHeight + foliageAttachment.foliageHeightOffset();
      this.placeLeavesRow(
         level, foliageSetter, random, tree, foliagePos, currentRadius - 2, foliageHeightWithOffset - 1, doubleTrunk, foliageHeightWithOffset, flipRhombusShape
      );
      this.placeLeavesRow(
         level, foliageSetter, random, tree, foliagePos, currentRadius - 1, foliageHeightWithOffset - 2, doubleTrunk, foliageHeightWithOffset, flipRhombusShape
      );
      this.placeLeavesRow(
         level, foliageSetter, random, tree, foliagePos, currentRadius - 1, foliageHeightWithOffset - 3, doubleTrunk, foliageHeightWithOffset, flipRhombusShape
      );

      for (int y = foliageHeightWithOffset - 4; y >= 1; y--) {
         this.placeLeavesRow(level, foliageSetter, random, tree, foliagePos, currentRadius, y, doubleTrunk, foliageHeightWithOffset, flipRhombusShape);
      }

      this.replaceLeavesWithLog(
         level, foliageSetter, tree, random, foliagePos, currentRadius, foliageHeightWithOffset - 4, doubleTrunk, foliageHeightWithOffset, flipRhombusShape
      );
      this.placeLeavesRow(level, foliageSetter, random, tree, foliagePos, currentRadius - 1, 0, doubleTrunk, foliageHeightWithOffset, flipRhombusShape);
      this.placeLeavesRow(
         level, foliageSetter, random, tree, foliagePos, Mth.clamp(currentRadius - 2, 1, 2), -1, doubleTrunk, foliageHeightWithOffset, flipRhombusShape
      );
   }

   private void replaceLeavesWithLog(
      final WorldGenLevel level,
      final FoliagePlacer.FoliageSetter foliageSetter,
      final TreeFeature tree,
      final RandomSource random,
      final BlockPos origin,
      final int currentRadius,
      final int y,
      final boolean doubleTrunk,
      final int foliageHeight,
      final boolean flipRhombusShape
   ) {
      int offset = doubleTrunk ? 1 : 0;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int dx = -currentRadius; dx <= currentRadius + offset; dx++) {
         for (int dz = -currentRadius; dz <= currentRadius + offset; dz++) {
            int absDz = Mth.abs(dz);
            int absDx = Mth.abs(dx);
            if (isWithinRhombusShape(
                  currentRadius,
                  absDx,
                  absDz,
                  this.getCornerBlocksToCutForRhombusShape(dx, dz, currentRadius, this.shouldRowBePartialRhombusShape(foliageHeight, y), flipRhombusShape),
                  2
               )
               && (absDz == 0 && currentRadius - absDx >= 4 || absDx == 0 && currentRadius - absDz >= 4)) {
               pos.setWithOffset(origin, dx, y, dz);
               tryPlaceLog(
                  level,
                  foliageSetter,
                  random,
                  tree,
                  pos,
                  getSidewaysStateModifier(Direction.fromAxisAndDirection(absDz == 0 ? Direction.Axis.X : Direction.Axis.Z, Direction.AxisDirection.POSITIVE))
               );
            }
         }
      }
   }

   private static void tryPlaceLog(
      final WorldGenLevel level,
      final FoliagePlacer.FoliageSetter foliageSetter,
      final RandomSource random,
      final TreeFeature tree,
      final BlockPos pos,
      final Function<BlockState, BlockState> stateModifier
   ) {
      if (level.isStateAtPosition(pos, state -> state.equals(tree.foliageProvider().getState(level, random, pos)))) {
         foliageSetter.set(pos, stateModifier.apply(tree.trunkProvider().getState(level, random, pos)));
      }
   }

   private static Function<BlockState, BlockState> getSidewaysStateModifier(final Direction branchDirection) {
      return state -> state.trySetValue(RotatedPillarBlock.AXIS, branchDirection.getAxis());
   }

   @Override
   public int foliageHeight(final RandomSource random, final int treeHeight, final TreeFeature tree) {
      return this.height.sample(random);
   }

   private void placeLeavesRow(
      final WorldGenLevel level,
      final FoliagePlacer.FoliageSetter foliageSetter,
      final RandomSource random,
      final TreeFeature tree,
      final BlockPos origin,
      final int currentRadius,
      final int y,
      final boolean doubleTrunk,
      final int foliageHeight,
      final boolean flipRhombusShape
   ) {
      int offset = doubleTrunk ? 1 : 0;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int dx = -currentRadius; dx <= currentRadius + offset; dx++) {
         for (int dz = -currentRadius; dz <= currentRadius + offset; dz++) {
            if (!this.shouldSkipLocation(random, dx, y, dz, currentRadius, doubleTrunk, foliageHeight, flipRhombusShape)) {
               pos.setWithOffset(origin, dx, y, dz);
               tryPlaceLeaf(level, foliageSetter, random, tree, pos);
            }
         }
      }
   }

   private boolean shouldSkipLocation(
      final RandomSource random,
      final int dx,
      final int y,
      final int dz,
      final int currentRadius,
      final boolean doubleTrunk,
      final int foliageHeight,
      final boolean flipRhombusShape
   ) {
      boolean shouldRowBePartialRhombusShape = this.shouldRowBePartialRhombusShape(foliageHeight, y);
      int cornerBlocksToCutForRhombusShape = this.getCornerBlocksToCutForRhombusShape(dx, dz, currentRadius, shouldRowBePartialRhombusShape, flipRhombusShape);
      int absDx = Mth.abs(dx);
      int absDz = Mth.abs(dz);
      boolean isRhombusEdgeBlock = absDx == currentRadius || absDz == currentRadius;
      if (shouldRowBePartialRhombusShape && isRhombusEdgeBlock) {
         return true;
      }

      int additionalSideRemoval = random.nextFloat() <= this.sideHoleChance ? 1 : 0;
      return !isWithinRhombusShape(currentRadius, absDx, absDz, cornerBlocksToCutForRhombusShape, additionalSideRemoval);
   }

   @Override
   protected boolean shouldSkipLocationSigned(
      final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk
   ) {
      throw new IllegalStateException("Overridden method needs more context");
   }

   @Override
   protected boolean shouldSkipLocation(final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk) {
      throw new IllegalStateException("Overridden method needs more context");
   }

   private int getCornerBlocksToCutForRhombusShape(
      final int dx, final int dz, final int currentRadius, final boolean shouldRowBePartialRhombusShape, final boolean flipRhombusShape
   ) {
      boolean isSmallCornerOfShape = flipRhombusShape ? isLeftTopCornerOrRightLowerCorner(dx, dz) : isLeftLowerCornerOrRightTopCorner(dx, dz);
      return isSmallCornerOfShape ? currentRadius - 1 : (shouldRowBePartialRhombusShape ? currentRadius + 1 : currentRadius);
   }

   private static boolean isWithinRhombusShape(
      final int currentRadius, final int absDx, final int absDz, final int cornerBlocksToCutForRhombusShape, final int additionalSideRemoval
   ) {
      return absDx + absDz <= currentRadius * 2 - (cornerBlocksToCutForRhombusShape + additionalSideRemoval);
   }

   private static boolean isLeftLowerCornerOrRightTopCorner(final int dx, final int dz) {
      return dx > 0 && dz < 0 || dz > 0 && dx < 0;
   }

   private static boolean isLeftTopCornerOrRightLowerCorner(final int dx, final int dz) {
      return dx > 0 && dz > 0 || dz < 0 && dx < 0;
   }

   private boolean shouldRowBePartialRhombusShape(final int foliageHeight, final int y) {
      return foliageHeight - 1 == y || foliageHeight - 2 == y;
   }
}
