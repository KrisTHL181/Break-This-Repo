package net.minecraft.world.level.block;

import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class DoubleBlockCombiner {
   public static <S extends BlockEntity> DoubleBlockCombiner.NeighborCombineResult<S> combineWithNeigbour(
      final BlockEntityType<S> entityType,
      final Function<BlockState, DoubleBlockCombiner.BlockType> typeResolver,
      final Function<BlockState, Direction> connectionResolver,
      final Property<Direction> facingProperty,
      final BlockState state,
      final LevelAccessor level,
      final BlockPos pos,
      final BiPredicate<LevelAccessor, BlockPos> blockedChecker
   ) {
      S blockEntity = entityType.getBlockEntity(level, pos);
      if (blockEntity == null) {
         return DoubleBlockCombiner.Combiner::acceptNone;
      }

      if (blockedChecker.test(level, pos)) {
         return DoubleBlockCombiner.Combiner::acceptNone;
      }

      DoubleBlockCombiner.BlockType type = typeResolver.apply(state);
      boolean single = type == DoubleBlockCombiner.BlockType.SINGLE;
      boolean isFirst = type == DoubleBlockCombiner.BlockType.FIRST;
      if (single) {
         return new DoubleBlockCombiner.NeighborCombineResult.Single<>(blockEntity);
      }

      BlockPos neighborPos = pos.relative(connectionResolver.apply(state));
      BlockState neighbourState = level.getBlockState(neighborPos);
      if (neighbourState.is(state.getBlock())) {
         DoubleBlockCombiner.BlockType neighbourType = typeResolver.apply(neighbourState);
         if (neighbourType != DoubleBlockCombiner.BlockType.SINGLE
            && type != neighbourType
            && neighbourState.getValue(facingProperty) == state.getValue(facingProperty)) {
            if (blockedChecker.test(level, neighborPos)) {
               return DoubleBlockCombiner.Combiner::acceptNone;
            }

            S neighbour = entityType.getBlockEntity(level, neighborPos);
            if (neighbour != null) {
               S first = isFirst ? blockEntity : neighbour;
               S second = isFirst ? neighbour : blockEntity;
               return new DoubleBlockCombiner.NeighborCombineResult.Double<>(first, second);
            }
         }
      }

      return new DoubleBlockCombiner.NeighborCombineResult.Single<>(blockEntity);
   }

   public enum BlockType {
      SINGLE,
      FIRST,
      SECOND;
   }

   public interface Combiner<S, T> {
      T acceptDouble(S first, S second);

      T acceptSingle(S single);

      T acceptNone();
   }

   public interface NeighborCombineResult<S> {
      <T> T apply(DoubleBlockCombiner.Combiner<? super S, T> callback);

      final class Double<S> implements DoubleBlockCombiner.NeighborCombineResult<S> {
         private final S first;
         private final S second;

         public Double(final S first, final S second) {
            this.first = first;
            this.second = second;
         }

         @Override
         public <T> T apply(final DoubleBlockCombiner.Combiner<? super S, T> callback) {
            return callback.acceptDouble(this.first, this.second);
         }
      }

      final class Single<S> implements DoubleBlockCombiner.NeighborCombineResult<S> {
         private final S single;

         public Single(final S single) {
            this.single = single;
         }

         @Override
         public <T> T apply(final DoubleBlockCombiner.Combiner<? super S, T> callback) {
            return callback.acceptSingle(this.single);
         }
      }
   }
}
