package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.configurations.PointedDripstoneConfiguration;

public class PointedDripstoneFeature extends Feature<PointedDripstoneConfiguration> {
   public PointedDripstoneFeature(Codec<PointedDripstoneConfiguration> p_191067_) {
      super(p_191067_);
   }

   @Override
   public boolean place(FeaturePlaceContext<PointedDripstoneConfiguration> p_191078_) {
      LevelAccessor levelaccessor = p_191078_.level();
      BlockPos blockpos = p_191078_.origin();
      RandomSource randomsource = p_191078_.random();
      PointedDripstoneConfiguration pointeddripstoneconfiguration = p_191078_.config();
      Optional<Direction> optional = getTipDirection(levelaccessor, blockpos, randomsource);
      if (optional.isEmpty()) {
         return false;
      }

      BlockPos blockpos1 = blockpos.relative(optional.get().getOpposite());
      createPatchOfDripstoneBlocks(levelaccessor, randomsource, blockpos1, pointeddripstoneconfiguration);
      int i = randomsource.nextFloat() < pointeddripstoneconfiguration.chanceOfTallerDripstone
            && DripstoneUtils.isEmptyOrWater(levelaccessor.getBlockState(blockpos.relative(optional.get())))
         ? 2
         : 1;
      DripstoneUtils.growPointedDripstone(levelaccessor, blockpos, optional.get(), i, false);
      return true;
   }

   private static Optional<Direction> getTipDirection(LevelAccessor p_225199_, BlockPos p_225200_, RandomSource p_225201_) {
      boolean flag = DripstoneUtils.isDripstoneBase(p_225199_.getBlockState(p_225200_.above()));
      boolean flag1 = DripstoneUtils.isDripstoneBase(p_225199_.getBlockState(p_225200_.below()));
      if (flag && flag1) {
         return Optional.of(p_225201_.nextBoolean() ? Direction.DOWN : Direction.UP);
      } else if (flag) {
         return Optional.of(Direction.DOWN);
      } else {
         return flag1 ? Optional.of(Direction.UP) : Optional.empty();
      }
   }

   private static void createPatchOfDripstoneBlocks(
      LevelAccessor p_225194_, RandomSource p_225195_, BlockPos p_225196_, PointedDripstoneConfiguration p_225197_
   ) {
      DripstoneUtils.placeDripstoneBlockIfPossible(p_225194_, p_225196_);

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         if (!(p_225195_.nextFloat() > p_225197_.chanceOfDirectionalSpread)) {
            BlockPos blockpos = p_225196_.relative(direction);
            DripstoneUtils.placeDripstoneBlockIfPossible(p_225194_, blockpos);
            if (!(p_225195_.nextFloat() > p_225197_.chanceOfSpreadRadius2)) {
               BlockPos blockpos1 = blockpos.relative(Direction.getRandom(p_225195_));
               DripstoneUtils.placeDripstoneBlockIfPossible(p_225194_, blockpos1);
               if (!(p_225195_.nextFloat() > p_225197_.chanceOfSpreadRadius3)) {
                  BlockPos blockpos2 = blockpos1.relative(Direction.getRandom(p_225195_));
                  DripstoneUtils.placeDripstoneBlockIfPossible(p_225194_, blockpos2);
               }
            }
         }
      }
   }
}
