package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.Products.P2;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.material.Fluids;

public abstract class FoliagePlacer {
   public static final Codec<FoliagePlacer> CODEC = BuiltInRegistries.FOLIAGE_PLACER_TYPE.byNameCodec().dispatch(FoliagePlacer::type, FoliagePlacerType::codec);
   protected final IntProvider radius;
   protected final IntProvider offset;

   protected static <P extends FoliagePlacer> P2<Mu<P>, IntProvider, IntProvider> foliagePlacerParts(final Instance<P> instance) {
      return instance.group(
         IntProviders.codec(0, 16).fieldOf("radius").forGetter(p -> p.radius), IntProviders.codec(0, 16).fieldOf("offset").forGetter(p -> p.offset)
      );
   }

   public FoliagePlacer(final IntProvider radius, final IntProvider offset) {
      this.radius = radius;
      this.offset = offset;
   }

   protected abstract FoliagePlacerType<?> type();

   public void createFoliage(
      final WorldGenLevel level,
      final FoliagePlacer.FoliageSetter foliageSetter,
      final RandomSource random,
      final TreeFeature tree,
      final int treeHeight,
      final FoliagePlacer.FoliageAttachment foliageAttachment,
      final int foliageHeight,
      final int leafRadius
   ) {
      this.createFoliage(level, foliageSetter, random, tree, treeHeight, foliageAttachment, foliageHeight, leafRadius, this.offset(random));
   }

   protected abstract void createFoliage(
      final WorldGenLevel level,
      final FoliagePlacer.FoliageSetter foliageSetter,
      final RandomSource random,
      final TreeFeature tree,
      final int treeHeight,
      final FoliagePlacer.FoliageAttachment foliageAttachment,
      final int foliageHeight,
      final int leafRadius,
      final int offset
   );

   public abstract int foliageHeight(final RandomSource random, final int treeHeight, final TreeFeature tree);

   public int foliageRadius(final RandomSource random, final int trunkHeight) {
      return this.radius.sample(random);
   }

   private int offset(final RandomSource random) {
      return this.offset.sample(random);
   }

   protected abstract boolean shouldSkipLocation(
      final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk
   );

   protected boolean shouldSkipLocationSigned(
      final RandomSource random, final int dx, final int y, final int dz, final int currentRadius, final boolean doubleTrunk
   ) {
      int minDx;
      int minDz;
      if (doubleTrunk) {
         minDx = Math.min(Math.abs(dx), Math.abs(dx - 1));
         minDz = Math.min(Math.abs(dz), Math.abs(dz - 1));
      } else {
         minDx = Math.abs(dx);
         minDz = Math.abs(dz);
      }

      return this.shouldSkipLocation(random, minDx, y, minDz, currentRadius, doubleTrunk);
   }

   protected void placeLeavesRow(
      final WorldGenLevel level,
      final FoliagePlacer.FoliageSetter foliageSetter,
      final RandomSource random,
      final TreeFeature tree,
      final BlockPos origin,
      final int currentRadius,
      final int y,
      final boolean doubleTrunk
   ) {
      int offset = doubleTrunk ? 1 : 0;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int dx = -currentRadius; dx <= currentRadius + offset; dx++) {
         for (int dz = -currentRadius; dz <= currentRadius + offset; dz++) {
            if (!this.shouldSkipLocationSigned(random, dx, y, dz, currentRadius, doubleTrunk)) {
               pos.setWithOffset(origin, dx, y, dz);
               tryPlaceLeaf(level, foliageSetter, random, tree, pos);
            }
         }
      }
   }

   protected final void placeLeavesRowWithHangingLeavesBelow(
      final WorldGenLevel level,
      final FoliagePlacer.FoliageSetter foliageSetter,
      final RandomSource random,
      final TreeFeature tree,
      final BlockPos origin,
      final int currentRadius,
      final int y,
      final boolean doubleTrunk,
      final float hangingLeavesChance,
      final float hangingLeavesExtensionChance
   ) {
      this.placeLeavesRow(level, foliageSetter, random, tree, origin, currentRadius, y, doubleTrunk);
      int offset = doubleTrunk ? 1 : 0;
      BlockPos logPos = origin.below();
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (Direction alongEdge : Direction.Plane.HORIZONTAL) {
         Direction toEdge = alongEdge.getClockWise();
         int offsetToEdge = toEdge.getAxisDirection() == Direction.AxisDirection.POSITIVE ? currentRadius + offset : currentRadius;
         pos.setWithOffset(origin, 0, y - 1, 0).move(toEdge, offsetToEdge).move(alongEdge, -currentRadius);
         int offsetAlongEdge = -currentRadius;

         while (offsetAlongEdge < currentRadius + offset) {
            boolean leavesAbove = foliageSetter.isSet(pos.move(Direction.UP));
            pos.move(Direction.DOWN);
            if (leavesAbove && tryPlaceExtension(level, foliageSetter, random, tree, hangingLeavesChance, logPos, pos)) {
               pos.move(Direction.DOWN);
               tryPlaceExtension(level, foliageSetter, random, tree, hangingLeavesExtensionChance, logPos, pos);
               pos.move(Direction.UP);
            }

            offsetAlongEdge++;
            pos.move(alongEdge);
         }
      }
   }

   private static boolean tryPlaceExtension(
      final WorldGenLevel level,
      final FoliagePlacer.FoliageSetter foliageSetter,
      final RandomSource random,
      final TreeFeature tree,
      final float chance,
      final BlockPos logPos,
      final BlockPos.MutableBlockPos pos
   ) {
      if (pos.distManhattan(logPos) >= 7) {
         return false;
      } else {
         return random.nextFloat() > chance ? false : tryPlaceLeaf(level, foliageSetter, random, tree, pos);
      }
   }

   protected static boolean tryPlaceLeaf(
      final WorldGenLevel level, final FoliagePlacer.FoliageSetter foliageSetter, final RandomSource random, final TreeFeature tree, final BlockPos pos
   ) {
      boolean isPersistent = level.isStateAtPosition(pos, state -> state.getValueOrElse(BlockStateProperties.PERSISTENT, false));
      if (!isPersistent && TreeFeature.validTreePos(level, pos)) {
         BlockState foliageState = tree.foliageProvider().getState(level, random, pos);
         if (foliageState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            foliageState = foliageState.setValue(
               BlockStateProperties.WATERLOGGED, level.isFluidAtPosition(pos, fluidState -> fluidState.isSourceOfType(Fluids.WATER))
            );
         }

         foliageSetter.set(pos, foliageState);
         return true;
      } else {
         return false;
      }
   }

   public record FoliageAttachment(BlockPos pos, int radiusOffsetXZ, int foliageHeightOffset, int sizeX, int sizeZ) {
      public FoliageAttachment(final BlockPos pos, final int radiusOffsetXZ, final boolean doubleTrunk) {
         int sizeXZ = doubleTrunk ? 2 : 1;
         this(pos, radiusOffsetXZ, sizeXZ, sizeXZ);
      }

      public FoliageAttachment(final BlockPos pos, final int radiusOffsetXZ, final int sizeX, final int sizeZ) {
         this(pos, radiusOffsetXZ, 0, sizeX, sizeZ);
      }

      public boolean doubleTrunk() {
         return this.sizeX == 2 && this.sizeZ == 2;
      }
   }

   public interface FoliageSetter {
      void set(final BlockPos pos, final BlockState state);

      boolean isSet(final BlockPos pos);
   }
}
