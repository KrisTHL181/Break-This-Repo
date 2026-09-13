package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.DripstoneClusterConfiguration;

public class DripstoneClusterFeature extends Feature<DripstoneClusterConfiguration> {
   public DripstoneClusterFeature(Codec<DripstoneClusterConfiguration> p_159575_) {
      super(p_159575_);
   }

   @Override
   public boolean place(FeaturePlaceContext<DripstoneClusterConfiguration> p_159605_) {
      WorldGenLevel worldgenlevel = p_159605_.level();
      BlockPos blockpos = p_159605_.origin();
      DripstoneClusterConfiguration dripstoneclusterconfiguration = p_159605_.config();
      RandomSource randomsource = p_159605_.random();
      if (!DripstoneUtils.isEmptyOrWater(worldgenlevel, blockpos)) {
         return false;
      }

      int i = dripstoneclusterconfiguration.height.sample(randomsource);
      float f = dripstoneclusterconfiguration.wetness.sample(randomsource);
      float f1 = dripstoneclusterconfiguration.density.sample(randomsource);
      int j = dripstoneclusterconfiguration.radius.sample(randomsource);
      int k = dripstoneclusterconfiguration.radius.sample(randomsource);

      for (int l = -j; l <= j; l++) {
         for (int i1 = -k; i1 <= k; i1++) {
            double d0 = this.getChanceOfStalagmiteOrStalactite(j, k, l, i1, dripstoneclusterconfiguration);
            BlockPos blockpos1 = blockpos.offset(l, 0, i1);
            this.placeColumn(worldgenlevel, randomsource, blockpos1, l, i1, f, d0, i, f1, dripstoneclusterconfiguration);
         }
      }

      return true;
   }

   private void placeColumn(
      WorldGenLevel p_225016_,
      RandomSource p_225017_,
      BlockPos p_225018_,
      int p_225019_,
      int p_225020_,
      float p_225021_,
      double p_225022_,
      int p_225023_,
      float p_225024_,
      DripstoneClusterConfiguration p_225025_
   ) {
      Optional<Column> optional = Column.scan(
         p_225016_, p_225018_, p_225025_.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isNeitherEmptyNorWater
      );
      if (!optional.isEmpty()) {
         OptionalInt optionalint = optional.get().getCeiling();
         OptionalInt optionalint1 = optional.get().getFloor();
         if (!optionalint.isEmpty() || !optionalint1.isEmpty()) {
            boolean flag = p_225017_.nextFloat() < p_225021_;
            Column column;
            if (flag && optionalint1.isPresent() && this.canPlacePool(p_225016_, p_225018_.atY(optionalint1.getAsInt()))) {
               int i = optionalint1.getAsInt();
               column = optional.get().withFloor(OptionalInt.of(i - 1));
               p_225016_.setBlock(p_225018_.atY(i), Blocks.WATER.defaultBlockState(), 2);
            } else {
               column = optional.get();
            }

            OptionalInt optionalint2 = column.getFloor();
            boolean flag1 = p_225017_.nextDouble() < p_225022_;
            int j;
            if (optionalint.isPresent() && flag1 && !this.isLava(p_225016_, p_225018_.atY(optionalint.getAsInt()))) {
               int k = p_225025_.dripstoneBlockLayerThickness.sample(p_225017_);
               this.replaceBlocksWithDripstoneBlocks(p_225016_, p_225018_.atY(optionalint.getAsInt()), k, Direction.UP);
               int l;
               if (optionalint2.isPresent()) {
                  l = Math.min(p_225023_, optionalint.getAsInt() - optionalint2.getAsInt());
               } else {
                  l = p_225023_;
               }

               j = this.getDripstoneHeight(p_225017_, p_225019_, p_225020_, p_225024_, l, p_225025_);
            } else {
               j = 0;
            }

            boolean flag2 = p_225017_.nextDouble() < p_225022_;
            int i3;
            if (optionalint2.isPresent() && flag2 && !this.isLava(p_225016_, p_225018_.atY(optionalint2.getAsInt()))) {
               int i1 = p_225025_.dripstoneBlockLayerThickness.sample(p_225017_);
               this.replaceBlocksWithDripstoneBlocks(p_225016_, p_225018_.atY(optionalint2.getAsInt()), i1, Direction.DOWN);
               if (optionalint.isPresent()) {
                  i3 = Math.max(
                     0, j + Mth.randomBetweenInclusive(p_225017_, -p_225025_.maxStalagmiteStalactiteHeightDiff, p_225025_.maxStalagmiteStalactiteHeightDiff)
                  );
               } else {
                  i3 = this.getDripstoneHeight(p_225017_, p_225019_, p_225020_, p_225024_, p_225023_, p_225025_);
               }
            } else {
               i3 = 0;
            }

            int j3;
            int j1;
            if (optionalint.isPresent() && optionalint2.isPresent() && optionalint.getAsInt() - j <= optionalint2.getAsInt() + i3) {
               int k1 = optionalint2.getAsInt();
               int l1 = optionalint.getAsInt();
               int i2 = Math.max(l1 - j, k1 + 1);
               int j2 = Math.min(k1 + i3, l1 - 1);
               int k2 = Mth.randomBetweenInclusive(p_225017_, i2, j2 + 1);
               int l2 = k2 - 1;
               j3 = l1 - k2;
               j1 = l2 - k1;
            } else {
               j3 = j;
               j1 = i3;
            }

            boolean flag3 = p_225017_.nextBoolean() && j3 > 0 && j1 > 0 && column.getHeight().isPresent() && j3 + j1 == column.getHeight().getAsInt();
            if (optionalint.isPresent()) {
               DripstoneUtils.growPointedDripstone(p_225016_, p_225018_.atY(optionalint.getAsInt() - 1), Direction.DOWN, j3, flag3);
            }

            if (optionalint2.isPresent()) {
               DripstoneUtils.growPointedDripstone(p_225016_, p_225018_.atY(optionalint2.getAsInt() + 1), Direction.UP, j1, flag3);
            }
         }
      }
   }

   private boolean isLava(LevelReader p_159586_, BlockPos p_159587_) {
      return p_159586_.getBlockState(p_159587_).is(Blocks.LAVA);
   }

   private int getDripstoneHeight(RandomSource p_225009_, int p_225010_, int p_225011_, float p_225012_, int p_225013_, DripstoneClusterConfiguration p_225014_) {
      if (p_225009_.nextFloat() > p_225012_) {
         return 0;
      }

      int i = Math.abs(p_225010_) + Math.abs(p_225011_);
      float f = (float)Mth.clampedMap(i, 0.0, p_225014_.maxDistanceFromCenterAffectingHeightBias, p_225013_ / 2.0, 0.0);
      return (int)randomBetweenBiased(p_225009_, 0.0F, p_225013_, f, p_225014_.heightDeviation);
   }

   private boolean canPlacePool(WorldGenLevel p_159620_, BlockPos p_159621_) {
      BlockState blockstate = p_159620_.getBlockState(p_159621_);
      if (!blockstate.is(Blocks.WATER) && !blockstate.is(Blocks.DRIPSTONE_BLOCK) && !blockstate.is(Blocks.POINTED_DRIPSTONE)) {
         if (p_159620_.getBlockState(p_159621_.above()).getFluidState().is(FluidTags.WATER)) {
            return false;
         }

         for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!this.canBeAdjacentToWater(p_159620_, p_159621_.relative(direction))) {
               return false;
            }
         }

         return this.canBeAdjacentToWater(p_159620_, p_159621_.below());
      } else {
         return false;
      }
   }

   private boolean canBeAdjacentToWater(LevelAccessor p_159583_, BlockPos p_159584_) {
      BlockState blockstate = p_159583_.getBlockState(p_159584_);
      return blockstate.is(BlockTags.BASE_STONE_OVERWORLD) || blockstate.getFluidState().is(FluidTags.WATER);
   }

   private void replaceBlocksWithDripstoneBlocks(WorldGenLevel p_159589_, BlockPos p_159590_, int p_159591_, Direction p_159592_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_159590_.mutable();

      for (int i = 0; i < p_159591_; i++) {
         if (!DripstoneUtils.placeDripstoneBlockIfPossible(p_159589_, blockpos$mutableblockpos)) {
            return;
         }

         blockpos$mutableblockpos.move(p_159592_);
      }
   }

   private double getChanceOfStalagmiteOrStalactite(int p_159577_, int p_159578_, int p_159579_, int p_159580_, DripstoneClusterConfiguration p_159581_) {
      int i = p_159577_ - Math.abs(p_159579_);
      int j = p_159578_ - Math.abs(p_159580_);
      int k = Math.min(i, j);
      return Mth.clampedMap(
         k, 0.0F, p_159581_.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn, p_159581_.chanceOfDripstoneColumnAtMaxDistanceFromCenter, 1.0F
      );
   }

   private static float randomBetweenBiased(RandomSource p_225003_, float p_225004_, float p_225005_, float p_225006_, float p_225007_) {
      return ClampedNormalFloat.sample(p_225003_, p_225006_, p_225007_, p_225004_, p_225005_);
   }
}
