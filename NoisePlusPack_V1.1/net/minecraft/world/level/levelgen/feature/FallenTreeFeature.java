package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FallenTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

public class FallenTreeFeature extends Feature<FallenTreeConfiguration> {
   private static final int STUMP_HEIGHT = 1;
   private static final int STUMP_HEIGHT_PLUS_EMPTY_SPACE = 2;
   private static final int FALLEN_LOG_MAX_FALL_HEIGHT_TO_GROUND = 5;
   private static final int FALLEN_LOG_MAX_GROUND_GAP = 2;
   private static final int FALLEN_LOG_MAX_SPACE_FROM_STUMP = 2;

   public FallenTreeFeature(Codec<FallenTreeConfiguration> p_392549_) {
      super(p_392549_);
   }

   @Override
   public boolean place(FeaturePlaceContext<FallenTreeConfiguration> p_393978_) {
      this.placeFallenTree(p_393978_.config(), p_393978_.origin(), p_393978_.level(), p_393978_.random());
      return true;
   }

   private void placeFallenTree(FallenTreeConfiguration p_392644_, BlockPos p_394187_, WorldGenLevel p_395647_, RandomSource p_391733_) {
      this.placeStump(p_392644_, p_395647_, p_391733_, p_394187_.mutable());
      Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_391733_);
      int i = p_392644_.logLength.sample(p_391733_) - 2;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_394187_.relative(direction, 2 + p_391733_.nextInt(2)).mutable();
      this.setGroundHeightForFallenLogStartPos(p_395647_, blockpos$mutableblockpos);
      if (this.canPlaceEntireFallenLog(p_395647_, i, blockpos$mutableblockpos, direction)) {
         this.placeFallenLog(p_392644_, p_395647_, p_391733_, i, blockpos$mutableblockpos, direction);
      }
   }

   private void setGroundHeightForFallenLogStartPos(WorldGenLevel p_396419_, BlockPos.MutableBlockPos p_397012_) {
      p_397012_.move(Direction.UP, 1);

      for (int i = 0; i < 6; i++) {
         if (this.mayPlaceOn(p_396419_, p_397012_)) {
            return;
         }

         p_397012_.move(Direction.DOWN);
      }
   }

   private void placeStump(FallenTreeConfiguration p_395657_, WorldGenLevel p_396801_, RandomSource p_392489_, BlockPos.MutableBlockPos p_394715_) {
      BlockPos blockpos = this.placeLogBlock(p_395657_, p_396801_, p_392489_, p_394715_, Function.identity());
      this.decorateLogs(p_396801_, p_392489_, Set.of(blockpos), p_395657_.stumpDecorators);
   }

   private boolean canPlaceEntireFallenLog(WorldGenLevel p_394921_, int p_393137_, BlockPos.MutableBlockPos p_393745_, Direction p_393391_) {
      int i = 0;

      for (int j = 0; j < p_393137_; j++) {
         if (!TreeFeature.validTreePos(p_394921_, p_393745_)) {
            return false;
         }

         if (!this.isOverSolidGround(p_394921_, p_393745_)) {
            if (++i > 2) {
               return false;
            }
         } else {
            i = 0;
         }

         p_393745_.move(p_393391_);
      }

      p_393745_.move(p_393391_.getOpposite(), p_393137_);
      return true;
   }

   private void placeFallenLog(
      FallenTreeConfiguration p_394838_,
      WorldGenLevel p_396397_,
      RandomSource p_394699_,
      int p_396625_,
      BlockPos.MutableBlockPos p_394037_,
      Direction p_396491_
   ) {
      Set<BlockPos> set = new HashSet<>();

      for (int i = 0; i < p_396625_; i++) {
         set.add(this.placeLogBlock(p_394838_, p_396397_, p_394699_, p_394037_, getSidewaysStateModifier(p_396491_)));
         p_394037_.move(p_396491_);
      }

      this.decorateLogs(p_396397_, p_394699_, set, p_394838_.logDecorators);
   }

   private boolean mayPlaceOn(LevelAccessor p_394406_, BlockPos p_392150_) {
      return TreeFeature.validTreePos(p_394406_, p_392150_) && this.isOverSolidGround(p_394406_, p_392150_);
   }

   private boolean isOverSolidGround(LevelAccessor p_395614_, BlockPos p_395166_) {
      return p_395614_.getBlockState(p_395166_.below()).isFaceSturdy(p_395614_, p_395166_, Direction.UP);
   }

   private BlockPos placeLogBlock(
      FallenTreeConfiguration p_395209_,
      WorldGenLevel p_394250_,
      RandomSource p_392111_,
      BlockPos.MutableBlockPos p_394770_,
      Function<BlockState, BlockState> p_397029_
   ) {
      p_394250_.setBlock(p_394770_, p_397029_.apply(p_395209_.trunkProvider.getState(p_392111_, p_394770_)), 3);
      this.markAboveForPostProcessing(p_394250_, p_394770_);
      return p_394770_.immutable();
   }

   private void decorateLogs(WorldGenLevel p_395747_, RandomSource p_396895_, Set<BlockPos> p_394439_, List<TreeDecorator> p_394258_) {
      if (!p_394258_.isEmpty()) {
         TreeDecorator.Context treedecorator$context = new TreeDecorator.Context(
            p_395747_, this.getDecorationSetter(p_395747_), p_396895_, p_394439_, Set.of(), Set.of()
         );
         p_394258_.forEach(p_393767_ -> p_393767_.place(treedecorator$context));
      }
   }

   private BiConsumer<BlockPos, BlockState> getDecorationSetter(WorldGenLevel p_392222_) {
      return (p_391644_, p_394520_) -> p_392222_.setBlock(p_391644_, p_394520_, 19);
   }

   private static Function<BlockState, BlockState> getSidewaysStateModifier(Direction p_397923_) {
      return p_391565_ -> p_391565_.trySetValue(RotatedPillarBlock.AXIS, p_397923_.getAxis());
   }
}
