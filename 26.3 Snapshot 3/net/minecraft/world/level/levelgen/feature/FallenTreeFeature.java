package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

public record FallenTreeFeature(BlockStateProvider trunkProvider, IntProvider logLength, List<TreeDecorator> stumpDecorators, List<TreeDecorator> logDecorators)
   implements Feature {
   private static final int STUMP_HEIGHT = 1;
   private static final int STUMP_HEIGHT_PLUS_EMPTY_SPACE = 2;
   private static final int FALLEN_LOG_MAX_FALL_HEIGHT_TO_GROUND = 5;
   private static final int FALLEN_LOG_MAX_GROUND_GAP = 2;
   private static final int FALLEN_LOG_MAX_SPACE_FROM_STUMP = 2;
   public static final MapCodec<FallenTreeFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(FallenTreeFeature::trunkProvider),
            IntProviders.codec(0, 16).fieldOf("log_length").forGetter(FallenTreeFeature::logLength),
            TreeDecorator.CODEC.listOf().fieldOf("stump_decorators").forGetter(FallenTreeFeature::stumpDecorators),
            TreeDecorator.CODEC.listOf().fieldOf("log_decorators").forGetter(FallenTreeFeature::logDecorators)
         )
         .apply(i, FallenTreeFeature::new)
   );

   @Override
   public MapCodec<FallenTreeFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      this.placeFallenTree(origin, level, random);
      return true;
   }

   private void placeFallenTree(final BlockPos origin, final WorldGenLevel level, final RandomSource random) {
      this.placeStump(level, random, origin.mutable());
      Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      int logLength = this.logLength.sample(random) - 2;
      BlockPos.MutableBlockPos logStartPos = origin.relative(direction, 2 + random.nextInt(2)).mutable();
      this.setGroundHeightForFallenLogStartPos(level, logStartPos);
      if (this.canPlaceEntireFallenLog(level, logLength, logStartPos, direction)) {
         this.placeFallenLog(level, random, logLength, logStartPos, direction);
      }
   }

   private void setGroundHeightForFallenLogStartPos(final WorldGenLevel level, final BlockPos.MutableBlockPos logStartPos) {
      logStartPos.move(Direction.UP, 1);

      for (int i = 0; i < 6; i++) {
         if (this.mayPlaceOn(level, logStartPos)) {
            return;
         }

         logStartPos.move(Direction.DOWN);
      }
   }

   private void placeStump(final WorldGenLevel level, final RandomSource random, final BlockPos.MutableBlockPos stumpPos) {
      BlockPos stump = this.placeLogBlock(level, random, stumpPos, Function.identity());
      this.decorateLogs(level, random, Set.of(stump), this.stumpDecorators);
   }

   private boolean canPlaceEntireFallenLog(
      final WorldGenLevel level, final int logLength, final BlockPos.MutableBlockPos logStartPos, final Direction direction
   ) {
      int gapInGround = 0;

      for (int i = 0; i < logLength; i++) {
         if (!TreeFeature.validTreePos(level, logStartPos)) {
            return false;
         }

         if (!this.isOverSolidGround(level, logStartPos)) {
            if (++gapInGround > 2) {
               return false;
            }
         } else {
            gapInGround = 0;
         }

         logStartPos.move(direction);
      }

      logStartPos.move(direction.getOpposite(), logLength);
      return true;
   }

   private void placeFallenLog(
      final WorldGenLevel level, final RandomSource random, final int logLength, final BlockPos.MutableBlockPos logStartPos, final Direction direction
   ) {
      Set<BlockPos> fallenLog = new HashSet<>();

      for (int i = 0; i < logLength; i++) {
         fallenLog.add(this.placeLogBlock(level, random, logStartPos, getSidewaysStateModifier(direction)));
         logStartPos.move(direction);
      }

      this.decorateLogs(level, random, fallenLog, this.logDecorators);
   }

   private boolean mayPlaceOn(final LevelAccessor level, final BlockPos blockPos) {
      return TreeFeature.validTreePos(level, blockPos) && this.isOverSolidGround(level, blockPos);
   }

   private boolean isOverSolidGround(final LevelAccessor level, final BlockPos blockPos) {
      return level.getBlockState(blockPos.below()).isFaceSturdy(level, blockPos, Direction.UP);
   }

   private BlockPos placeLogBlock(
      final WorldGenLevel level,
      final RandomSource random,
      final BlockPos.MutableBlockPos blockPos,
      final Function<BlockState, BlockState> sidewaysStateModifier
   ) {
      level.setBlockAndUpdate(blockPos, sidewaysStateModifier.apply(this.trunkProvider.getState(level, random, blockPos)));
      this.markAboveForPostProcessing(level, blockPos);
      return blockPos.immutable();
   }

   private void decorateLogs(final WorldGenLevel level, final RandomSource random, final Set<BlockPos> logs, final List<TreeDecorator> decorators) {
      if (!decorators.isEmpty()) {
         TreeDecorator.Context decoratorContext = new TreeDecorator.Context(level, this.getDecorationSetter(level), random, logs, Set.of(), Set.of());
         decorators.forEach(decorator -> decorator.place(decoratorContext));
      }
   }

   private BiConsumer<BlockPos, BlockState> getDecorationSetter(final WorldGenLevel level) {
      return (pos, state) -> level.setBlock(pos, state, 19);
   }

   private static Function<BlockState, BlockState> getSidewaysStateModifier(final Direction direction) {
      return state -> state.trySetValue(RotatedPillarBlock.AXIS, direction.getAxis());
   }

   public static FallenTreeFeature.Builder builder(final BlockStateProvider trunkProvider, final IntProvider logLength) {
      return new FallenTreeFeature.Builder(trunkProvider, logLength);
   }

   public static class Builder {
      private final BlockStateProvider trunkProvider;
      private final IntProvider logLength;
      private final List<TreeDecorator> stumpDecorators = new ArrayList<>();
      private final List<TreeDecorator> logDecorators = new ArrayList<>();

      public Builder(final BlockStateProvider trunkProvider, final IntProvider logLength) {
         this.trunkProvider = trunkProvider;
         this.logLength = logLength;
      }

      public FallenTreeFeature.Builder stumpDecorator(final TreeDecorator stumpDecorator) {
         this.stumpDecorators.add(stumpDecorator);
         return this;
      }

      public FallenTreeFeature.Builder logDecorator(final TreeDecorator logDecorator) {
         this.logDecorators.add(logDecorator);
         return this;
      }

      public FallenTreeFeature build() {
         return new FallenTreeFeature(this.trunkProvider, this.logLength, List.copyOf(this.stumpDecorators), List.copyOf(this.logDecorators));
      }
   }
}
