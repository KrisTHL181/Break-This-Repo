package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MultifaceSpreader {
   public static final MultifaceSpreader.SpreadType[] DEFAULT_SPREAD_ORDER = new MultifaceSpreader.SpreadType[]{
      MultifaceSpreader.SpreadType.SAME_POSITION, MultifaceSpreader.SpreadType.SAME_PLANE, MultifaceSpreader.SpreadType.WRAP_AROUND
   };
   private final MultifaceSpreader.SpreadConfig config;

   public MultifaceSpreader(final MultifaceBlock multifaceBlock) {
      this(new MultifaceSpreader.DefaultSpreaderConfig(multifaceBlock));
   }

   public MultifaceSpreader(final MultifaceSpreader.SpreadConfig config) {
      this.config = config;
   }

   public boolean canSpreadInAnyDirection(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction startingFace) {
      return Direction.stream()
         .anyMatch(
            spreadDirection -> this.getSpreadFromFaceTowardDirection(state, level, pos, startingFace, spreadDirection, this.config::canSpreadInto).isPresent()
         );
   }

   public Optional<MultifaceSpreader.SpreadPos> spreadFromRandomFaceTowardRandomDirection(
      final BlockState state, final LevelAccessor level, final BlockPos pos, final RandomSource random
   ) {
      return Direction.allShuffled(random)
         .stream()
         .filter(faceDirection -> this.config.canSpreadFrom(state, faceDirection))
         .map(faceDirection -> this.spreadFromFaceTowardRandomDirection(state, level, pos, faceDirection, random, false))
         .filter(Optional::isPresent)
         .findFirst()
         .orElse(Optional.empty());
   }

   public long spreadAll(final BlockState state, final LevelAccessor level, final BlockPos pos, final boolean postProcess) {
      return Direction.stream()
         .filter(faceDirection -> this.config.canSpreadFrom(state, faceDirection))
         .map(faceDirection -> this.spreadFromFaceTowardAllDirections(state, level, pos, faceDirection, postProcess))
         .reduce(0L, Long::sum);
   }

   public Optional<MultifaceSpreader.SpreadPos> spreadFromFaceTowardRandomDirection(
      final BlockState state, final LevelAccessor level, final BlockPos pos, final Direction startingFace, final RandomSource random, final boolean postProcess
   ) {
      return Direction.allShuffled(random)
         .stream()
         .map(spreadDirection -> this.spreadFromFaceTowardDirection(state, level, pos, startingFace, spreadDirection, postProcess))
         .filter(Optional::isPresent)
         .findFirst()
         .orElse(Optional.empty());
   }

   private long spreadFromFaceTowardAllDirections(
      final BlockState state, final LevelAccessor level, final BlockPos pos, final Direction startingFace, final boolean postProcess
   ) {
      return Direction.stream()
         .map(spreadDirection -> this.spreadFromFaceTowardDirection(state, level, pos, startingFace, spreadDirection, postProcess))
         .filter(Optional::isPresent)
         .count();
   }

   @VisibleForTesting
   public Optional<MultifaceSpreader.SpreadPos> spreadFromFaceTowardDirection(
      final BlockState state,
      final LevelAccessor level,
      final BlockPos pos,
      final Direction fromFace,
      final Direction spreadDirection,
      final boolean postProcess
   ) {
      return this.getSpreadFromFaceTowardDirection(state, level, pos, fromFace, spreadDirection, this.config::canSpreadInto)
         .flatMap(spreadPos -> this.spreadToFace(level, spreadPos, postProcess));
   }

   public Optional<MultifaceSpreader.SpreadPos> getSpreadFromFaceTowardDirection(
      final BlockState state,
      final BlockGetter level,
      final BlockPos pos,
      final Direction startingFace,
      final Direction spreadDirection,
      final MultifaceSpreader.SpreadPredicate canSpreadInto
   ) {
      if (spreadDirection.getAxis() == startingFace.getAxis()) {
         return Optional.empty();
      }

      if (this.config.isOtherBlockValidAsSource(state) || this.config.hasFace(state, startingFace) && !this.config.hasFace(state, spreadDirection)) {
         for (MultifaceSpreader.SpreadType type : this.config.getSpreadTypes()) {
            MultifaceSpreader.SpreadPos spreadPos = type.getSpreadPos(pos, spreadDirection, startingFace);
            if (canSpreadInto.test(level, pos, spreadPos)) {
               return Optional.of(spreadPos);
            }
         }

         return Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   public Optional<MultifaceSpreader.SpreadPos> spreadToFace(final LevelAccessor level, final MultifaceSpreader.SpreadPos spreadPos, final boolean postProcess) {
      BlockState oldState = level.getBlockState(spreadPos.pos());
      return this.config.placeBlock(level, spreadPos, oldState, postProcess) ? Optional.of(spreadPos) : Optional.empty();
   }

   public static class DefaultSpreaderConfig implements MultifaceSpreader.SpreadConfig {
      protected final MultifaceBlock block;

      public DefaultSpreaderConfig(final MultifaceBlock block) {
         this.block = block;
      }

      @Override
      public @Nullable BlockState getStateForPlacement(
         final BlockState oldState, final BlockGetter level, final BlockPos placementPos, final Direction placementDirection
      ) {
         return this.block.getStateForPlacement(oldState, level, placementPos, placementDirection);
      }

      protected boolean stateCanBeReplaced(
         final BlockGetter level, final BlockPos sourcePos, final BlockPos placementPos, final Direction placementDirection, final BlockState existingState
      ) {
         return existingState.isAir() || existingState.is(this.block) || existingState.is(Blocks.WATER) && existingState.getFluidState().isSource();
      }

      @Override
      public boolean canSpreadInto(final BlockGetter level, final BlockPos sourcePos, final MultifaceSpreader.SpreadPos spreadPos) {
         BlockState existingState = level.getBlockState(spreadPos.pos());
         return this.stateCanBeReplaced(level, sourcePos, spreadPos.pos(), spreadPos.face(), existingState)
            && this.block.isValidStateForPlacement(level, existingState, spreadPos.pos(), spreadPos.face());
      }
   }

   public interface SpreadConfig {
      @Nullable BlockState getStateForPlacement(
         final BlockState oldState, final BlockGetter level, final BlockPos placementPos, final Direction placementDirection
      );

      boolean canSpreadInto(final BlockGetter level, final BlockPos sourcePos, final MultifaceSpreader.SpreadPos spreadPos);

      default MultifaceSpreader.SpreadType[] getSpreadTypes() {
         return MultifaceSpreader.DEFAULT_SPREAD_ORDER;
      }

      default boolean hasFace(final BlockState state, final Direction face) {
         return MultifaceBlock.hasFace(state, face);
      }

      default boolean isOtherBlockValidAsSource(final BlockState state) {
         return false;
      }

      default boolean canSpreadFrom(final BlockState state, final Direction face) {
         return this.isOtherBlockValidAsSource(state) || this.hasFace(state, face);
      }

      default boolean placeBlock(final LevelAccessor level, final MultifaceSpreader.SpreadPos spreadPos, final BlockState oldState, final boolean postProcess) {
         BlockState spreadState = this.getStateForPlacement(oldState, level, spreadPos.pos(), spreadPos.face());
         if (spreadState != null) {
            if (postProcess) {
               level.getChunk(spreadPos.pos()).markPosForPostProcessing(spreadPos.pos());
            }

            return level.setBlock(spreadPos.pos(), spreadState, 2);
         } else {
            return false;
         }
      }
   }

   public record SpreadPos(BlockPos pos, Direction face) {
   }

   @FunctionalInterface
   public interface SpreadPredicate {
      boolean test(final BlockGetter level, final BlockPos sourcePos, final MultifaceSpreader.SpreadPos spreadPos);
   }

   public enum SpreadType {
      SAME_POSITION {
         @Override
         public MultifaceSpreader.SpreadPos getSpreadPos(final BlockPos pos, final Direction spreadDirection, final Direction fromFace) {
            return new MultifaceSpreader.SpreadPos(pos, spreadDirection);
         }
      },
      SAME_PLANE {
         @Override
         public MultifaceSpreader.SpreadPos getSpreadPos(final BlockPos pos, final Direction spreadDirection, final Direction fromFace) {
            return new MultifaceSpreader.SpreadPos(pos.relative(spreadDirection), fromFace);
         }
      },
      WRAP_AROUND {
         @Override
         public MultifaceSpreader.SpreadPos getSpreadPos(final BlockPos pos, final Direction spreadDirection, final Direction fromFace) {
            return new MultifaceSpreader.SpreadPos(pos.relative(spreadDirection).relative(fromFace), spreadDirection.getOpposite());
         }
      };

      public abstract MultifaceSpreader.SpreadPos getSpreadPos(final BlockPos pos, final Direction spreadDirection, final Direction fromFace);
   }
}
