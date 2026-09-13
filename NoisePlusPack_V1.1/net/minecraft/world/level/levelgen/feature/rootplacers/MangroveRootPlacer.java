package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class MangroveRootPlacer extends RootPlacer {
   public static final int ROOT_WIDTH_LIMIT = 8;
   public static final int ROOT_LENGTH_LIMIT = 15;
   public static final MapCodec<MangroveRootPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_225856_ -> rootPlacerParts(p_225856_)
         .and(MangroveRootPlacement.CODEC.fieldOf("mangrove_root_placement").forGetter(p_225849_ -> p_225849_.mangroveRootPlacement))
         .apply(p_225856_, MangroveRootPlacer::new)
   );
   private final MangroveRootPlacement mangroveRootPlacement;

   public MangroveRootPlacer(IntProvider p_225817_, BlockStateProvider p_225818_, Optional<AboveRootPlacement> p_225819_, MangroveRootPlacement p_225820_) {
      super(p_225817_, p_225818_, p_225819_);
      this.mangroveRootPlacement = p_225820_;
   }

   @Override
   public boolean placeRoots(
      LevelSimulatedReader p_225840_,
      BiConsumer<BlockPos, BlockState> p_225841_,
      RandomSource p_225842_,
      BlockPos p_225843_,
      BlockPos p_225844_,
      TreeConfiguration p_225845_
   ) {
      List<BlockPos> list = Lists.newArrayList();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_225843_.mutable();

      while (blockpos$mutableblockpos.getY() < p_225844_.getY()) {
         if (!this.canPlaceRoot(p_225840_, blockpos$mutableblockpos)) {
            return false;
         }

         blockpos$mutableblockpos.move(Direction.UP);
      }

      list.add(p_225844_.below());

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos = p_225844_.relative(direction);
         List<BlockPos> list1 = Lists.newArrayList();
         if (!this.simulateRoots(p_225840_, p_225842_, blockpos, direction, p_225844_, list1, 0)) {
            return false;
         }

         list.addAll(list1);
         list.add(p_225844_.relative(direction));
      }

      for (BlockPos blockpos1 : list) {
         this.placeRoot(p_225840_, p_225841_, p_225842_, blockpos1, p_225845_);
      }

      return true;
   }

   private boolean simulateRoots(
      LevelSimulatedReader p_225823_,
      RandomSource p_225824_,
      BlockPos p_225825_,
      Direction p_225826_,
      BlockPos p_225827_,
      List<BlockPos> p_225828_,
      int p_225829_
   ) {
      int i = this.mangroveRootPlacement.maxRootLength();
      if (p_225829_ != i && p_225828_.size() <= i) {
         for (BlockPos blockpos : this.potentialRootPositions(p_225825_, p_225826_, p_225824_, p_225827_)) {
            if (this.canPlaceRoot(p_225823_, blockpos)) {
               p_225828_.add(blockpos);
               if (!this.simulateRoots(p_225823_, p_225824_, blockpos, p_225826_, p_225827_, p_225828_, p_225829_ + 1)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   protected List<BlockPos> potentialRootPositions(BlockPos p_225851_, Direction p_225852_, RandomSource p_225853_, BlockPos p_225854_) {
      BlockPos blockpos = p_225851_.below();
      BlockPos blockpos1 = p_225851_.relative(p_225852_);
      int i = p_225851_.distManhattan(p_225854_);
      int j = this.mangroveRootPlacement.maxRootWidth();
      float f = this.mangroveRootPlacement.randomSkewChance();
      if (i > j - 3 && i <= j) {
         return p_225853_.nextFloat() < f ? List.of(blockpos, blockpos1.below()) : List.of(blockpos);
      } else if (i > j) {
         return List.of(blockpos);
      } else if (p_225853_.nextFloat() < f) {
         return List.of(blockpos);
      } else {
         return p_225853_.nextBoolean() ? List.of(blockpos1) : List.of(blockpos);
      }
   }

   @Override
   protected boolean canPlaceRoot(LevelSimulatedReader p_225831_, BlockPos p_225832_) {
      return super.canPlaceRoot(p_225831_, p_225832_)
         || p_225831_.isStateAtPosition(p_225832_, p_225858_ -> p_225858_.is(this.mangroveRootPlacement.canGrowThrough()));
   }

   @Override
   protected void placeRoot(
      LevelSimulatedReader p_225834_, BiConsumer<BlockPos, BlockState> p_225835_, RandomSource p_225836_, BlockPos p_225837_, TreeConfiguration p_225838_
   ) {
      if (p_225834_.isStateAtPosition(p_225837_, p_225847_ -> p_225847_.is(this.mangroveRootPlacement.muddyRootsIn()))) {
         BlockState blockstate = this.mangroveRootPlacement.muddyRootsProvider().getState(p_225836_, p_225837_);
         p_225835_.accept(p_225837_, this.getPotentiallyWaterloggedState(p_225834_, p_225837_, blockstate));
      } else {
         super.placeRoot(p_225834_, p_225835_, p_225836_, p_225837_, p_225838_);
      }
   }

   @Override
   protected RootPlacerType<?> type() {
      return RootPlacerType.MANGROVE_ROOT_PLACER;
   }
}
