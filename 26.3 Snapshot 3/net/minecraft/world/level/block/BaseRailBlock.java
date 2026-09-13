package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BaseRailBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE_FLAT = Block.column(16.0, 0.0, 2.0);
   private static final VoxelShape SHAPE_SLOPE = Block.column(16.0, 0.0, 8.0);
   private final boolean isStraight;

   public static boolean isRail(final Level level, final BlockPos pos) {
      return isRail(level.getBlockState(pos));
   }

   public static boolean isRail(final BlockState state) {
      return state.is(BlockTags.RAILS) && state.getBlock() instanceof BaseRailBlock;
   }

   protected BaseRailBlock(final boolean isStraight, final BlockBehaviour.Properties properties) {
      super(properties);
      this.isStraight = isStraight;
   }

   public boolean isStraight() {
      return this.isStraight;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return state.getValue(this.getShapeProperty()).isSlope() ? SHAPE_SLOPE : SHAPE_FLAT;
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      return canSupportRigidBlock(level, pos.below());
   }

   @Override
   protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
      if (!oldState.is(state.getBlock())) {
         this.updateState(state, level, pos, movedByPiston);
      }
   }

   protected BlockState updateState(BlockState state, final Level level, final BlockPos pos, final boolean movedByPiston) {
      state = this.updateDir(level, pos, state, true);
      if (this.isStraight) {
         level.neighborChanged(state, pos, this, null, movedByPiston);
      }

      return state;
   }

   @Override
   protected void neighborChanged(
      final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      if (!level.isClientSide() && level.getBlockState(pos).is(this)) {
         RailShape shape = state.getValue(this.getShapeProperty());
         if (shouldBeRemoved(pos, level, shape)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, movedByPiston);
         } else {
            this.updateState(state, level, pos, block);
         }
      }
   }

   private static boolean shouldBeRemoved(final BlockPos pos, final Level level, final RailShape shape) {
      if (!canSupportRigidBlock(level, pos.below())) {
         return true;
      }

      return switch (shape) {
         case ASCENDING_EAST -> !canSupportRigidBlock(level, pos.east());
         case ASCENDING_WEST -> !canSupportRigidBlock(level, pos.west());
         case ASCENDING_NORTH -> !canSupportRigidBlock(level, pos.north());
         case ASCENDING_SOUTH -> !canSupportRigidBlock(level, pos.south());
         default -> false;
      };
   }

   protected void updateState(final BlockState state, final Level level, final BlockPos pos, final Block block) {
   }

   protected BlockState updateDir(final Level level, final BlockPos pos, final BlockState state, final boolean first) {
      if (level.isClientSide()) {
         return state;
      }

      RailShape current = state.getValue(this.getShapeProperty());
      return new RailState(level, pos, state).place(level.hasNeighborSignal(pos), first, current).getState();
   }

   @Override
   protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
      if (!movedByPiston) {
         if (state.getValue(this.getShapeProperty()).isSlope()) {
            level.updateNeighborsAt(pos.above(), this);
         }

         if (this.isStraight) {
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      boolean isWaterSource = replacedFluidState.is(Fluids.WATER);
      BlockState state = super.defaultBlockState();
      Direction direction = context.getHorizontalDirection();
      boolean isEastWest = direction == Direction.EAST || direction == Direction.WEST;
      return state.setValue(this.getShapeProperty(), isEastWest ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH).setValue(WATERLOGGED, isWaterSource);
   }

   public abstract Property<RailShape> getShapeProperty();

   protected RailShape rotate(final RailShape shape, final Rotation rotation) {
      return switch (rotation) {
         case CLOCKWISE_180 -> {
            switch (shape) {
               case ASCENDING_EAST:
                  yield RailShape.ASCENDING_WEST;
               case ASCENDING_WEST:
                  yield RailShape.ASCENDING_EAST;
               case ASCENDING_NORTH:
                  yield RailShape.ASCENDING_SOUTH;
               case ASCENDING_SOUTH:
                  yield RailShape.ASCENDING_NORTH;
               case NORTH_SOUTH:
                  yield RailShape.NORTH_SOUTH;
               case EAST_WEST:
                  yield RailShape.EAST_WEST;
               case SOUTH_EAST:
                  yield RailShape.NORTH_WEST;
               case SOUTH_WEST:
                  yield RailShape.NORTH_EAST;
               case NORTH_WEST:
                  yield RailShape.SOUTH_EAST;
               case NORTH_EAST:
                  yield RailShape.SOUTH_WEST;
               default:
                  throw new MatchException(null, null);
            }
         }
         case COUNTERCLOCKWISE_90 -> {
            switch (shape) {
               case ASCENDING_EAST:
                  yield RailShape.ASCENDING_NORTH;
               case ASCENDING_WEST:
                  yield RailShape.ASCENDING_SOUTH;
               case ASCENDING_NORTH:
                  yield RailShape.ASCENDING_WEST;
               case ASCENDING_SOUTH:
                  yield RailShape.ASCENDING_EAST;
               case NORTH_SOUTH:
                  yield RailShape.EAST_WEST;
               case EAST_WEST:
                  yield RailShape.NORTH_SOUTH;
               case SOUTH_EAST:
                  yield RailShape.NORTH_EAST;
               case SOUTH_WEST:
                  yield RailShape.SOUTH_EAST;
               case NORTH_WEST:
                  yield RailShape.SOUTH_WEST;
               case NORTH_EAST:
                  yield RailShape.NORTH_WEST;
               default:
                  throw new MatchException(null, null);
            }
         }
         case CLOCKWISE_90 -> {
            switch (shape) {
               case ASCENDING_EAST:
                  yield RailShape.ASCENDING_SOUTH;
               case ASCENDING_WEST:
                  yield RailShape.ASCENDING_NORTH;
               case ASCENDING_NORTH:
                  yield RailShape.ASCENDING_EAST;
               case ASCENDING_SOUTH:
                  yield RailShape.ASCENDING_WEST;
               case NORTH_SOUTH:
                  yield RailShape.EAST_WEST;
               case EAST_WEST:
                  yield RailShape.NORTH_SOUTH;
               case SOUTH_EAST:
                  yield RailShape.SOUTH_WEST;
               case SOUTH_WEST:
                  yield RailShape.NORTH_WEST;
               case NORTH_WEST:
                  yield RailShape.NORTH_EAST;
               case NORTH_EAST:
                  yield RailShape.SOUTH_EAST;
               default:
                  throw new MatchException(null, null);
            }
         }
         default -> shape;
      };
   }

   protected RailShape mirror(final RailShape shape, final Mirror mirror) {
      return switch (mirror) {
         case LEFT_RIGHT -> {
            switch (shape) {
               case ASCENDING_NORTH:
                  yield RailShape.ASCENDING_SOUTH;
               case ASCENDING_SOUTH:
                  yield RailShape.ASCENDING_NORTH;
               case NORTH_SOUTH:
               case EAST_WEST:
               default:
                  yield shape;
               case SOUTH_EAST:
                  yield RailShape.NORTH_EAST;
               case SOUTH_WEST:
                  yield RailShape.NORTH_WEST;
               case NORTH_WEST:
                  yield RailShape.SOUTH_WEST;
               case NORTH_EAST:
                  yield RailShape.SOUTH_EAST;
            }
         }
         case FRONT_BACK -> {
            switch (shape) {
               case ASCENDING_EAST:
                  yield RailShape.ASCENDING_WEST;
               case ASCENDING_WEST:
                  yield RailShape.ASCENDING_EAST;
               case ASCENDING_NORTH:
               case ASCENDING_SOUTH:
               case NORTH_SOUTH:
               case EAST_WEST:
               default:
                  yield shape;
               case SOUTH_EAST:
                  yield RailShape.SOUTH_WEST;
               case SOUTH_WEST:
                  yield RailShape.SOUTH_EAST;
               case NORTH_WEST:
                  yield RailShape.NORTH_EAST;
               case NORTH_EAST:
                  yield RailShape.NORTH_WEST;
            }
         }
         default -> shape;
      };
   }

   @Override
   protected BlockState updateShape(
      final BlockState state,
      final LevelReader level,
      final ScheduledTickAccess ticks,
      final BlockPos pos,
      final Direction directionToNeighbour,
      final BlockPos neighbourPos,
      final BlockState neighbourState,
      final RandomSource random
   ) {
      if (state.getValue(WATERLOGGED)) {
         ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }
}
