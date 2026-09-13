package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StairBlock extends Block implements SimpleWaterloggedBlock {
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
   public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE_OUTER = Shapes.or(Block.column(16.0, 0.0, 8.0), Block.box(0.0, 8.0, 0.0, 8.0, 16.0, 8.0));
   private static final VoxelShape SHAPE_STRAIGHT = Shapes.or(SHAPE_OUTER, Shapes.rotate(SHAPE_OUTER, OctahedralGroup.BLOCK_ROT_Y_90));
   private static final VoxelShape SHAPE_INNER = Shapes.or(SHAPE_STRAIGHT, Shapes.rotate(SHAPE_STRAIGHT, OctahedralGroup.BLOCK_ROT_Y_90));
   private static final Map<Direction, VoxelShape> SHAPE_BOTTOM_OUTER = Shapes.rotateHorizontal(SHAPE_OUTER);
   private static final Map<Direction, VoxelShape> SHAPE_BOTTOM_STRAIGHT = Shapes.rotateHorizontal(SHAPE_STRAIGHT);
   private static final Map<Direction, VoxelShape> SHAPE_BOTTOM_INNER = Shapes.rotateHorizontal(SHAPE_INNER);
   private static final Map<Direction, VoxelShape> SHAPE_TOP_OUTER = Shapes.rotateHorizontal(SHAPE_OUTER, OctahedralGroup.INVERT_Y);
   private static final Map<Direction, VoxelShape> SHAPE_TOP_STRAIGHT = Shapes.rotateHorizontal(SHAPE_STRAIGHT, OctahedralGroup.INVERT_Y);
   private static final Map<Direction, VoxelShape> SHAPE_TOP_INNER = Shapes.rotateHorizontal(SHAPE_INNER, OctahedralGroup.INVERT_Y);
   private final Block base;
   protected final BlockState baseState;

   protected StairBlock(final BlockState baseState, final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HALF, Half.BOTTOM)
            .setValue(SHAPE, StairsShape.STRAIGHT)
            .setValue(WATERLOGGED, false)
      );
      this.base = baseState.getBlock();
      this.baseState = baseState;
   }

   @Override
   protected boolean useShapeForLightOcclusion(final BlockState state) {
      return true;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      boolean isBottom = state.getValue(HALF) == Half.BOTTOM;
      Direction facing = state.getValue(FACING);

      Map var10000 = switch ((StairsShape)state.getValue(SHAPE)) {
         case STRAIGHT -> isBottom ? SHAPE_BOTTOM_STRAIGHT : SHAPE_TOP_STRAIGHT;
         case OUTER_LEFT, OUTER_RIGHT -> isBottom ? SHAPE_BOTTOM_OUTER : SHAPE_TOP_OUTER;
         case INNER_RIGHT, INNER_LEFT -> isBottom ? SHAPE_BOTTOM_INNER : SHAPE_TOP_INNER;
      };

      return (VoxelShape)var10000.get(switch ((StairsShape)state.getValue(SHAPE)) {
         case STRAIGHT, OUTER_LEFT, INNER_RIGHT -> facing;
         case INNER_LEFT -> facing.getCounterClockWise();
         case OUTER_RIGHT -> facing.getClockWise();
      });
   }

   @Override
   public float getExplosionResistance() {
      return this.base.getExplosionResistance();
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      Direction clickedFace = context.getClickedFace();
      BlockPos pos = context.getClickedPos();
      FluidState replacedFluidState = context.getLevel().getFluidState(pos);
      BlockState state = this.defaultBlockState()
         .setValue(FACING, context.getHorizontalDirection())
         .setValue(
            HALF, clickedFace != Direction.DOWN && (clickedFace == Direction.UP || !(context.getClickLocation().y - pos.getY() > 0.5)) ? Half.BOTTOM : Half.TOP
         )
         .setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
      return state.setValue(SHAPE, getStairsShape(state, context.getLevel(), pos));
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

      return directionToNeighbour.getAxis().isHorizontal()
         ? state.setValue(SHAPE, getStairsShape(state, level, pos))
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   private static StairsShape getStairsShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
      Direction facing = state.getValue(FACING);
      BlockState behindState = level.getBlockState(pos.relative(facing));
      if (isStairs(behindState) && state.getValue(HALF) == behindState.getValue(HALF)) {
         Direction behindFacing = behindState.getValue(FACING);
         if (behindFacing.getAxis() != state.getValue(FACING).getAxis() && canTakeShape(state, level, pos, behindFacing.getOpposite())) {
            if (behindFacing == facing.getCounterClockWise()) {
               return StairsShape.OUTER_LEFT;
            }

            return StairsShape.OUTER_RIGHT;
         }
      }

      BlockState frontState = level.getBlockState(pos.relative(facing.getOpposite()));
      if (isStairs(frontState) && state.getValue(HALF) == frontState.getValue(HALF)) {
         Direction frontFacing = frontState.getValue(FACING);
         if (frontFacing.getAxis() != state.getValue(FACING).getAxis() && canTakeShape(state, level, pos, frontFacing)) {
            if (frontFacing == facing.getCounterClockWise()) {
               return StairsShape.INNER_LEFT;
            }

            return StairsShape.INNER_RIGHT;
         }
      }

      return StairsShape.STRAIGHT;
   }

   private static boolean canTakeShape(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction neighbour) {
      BlockState neighborState = level.getBlockState(pos.relative(neighbour));
      return !isStairs(neighborState) || neighborState.getValue(FACING) != state.getValue(FACING) || neighborState.getValue(HALF) != state.getValue(HALF);
   }

   public static boolean isStairs(final BlockState state) {
      return state.getBlock() instanceof StairBlock;
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      Direction direction = state.getValue(FACING);
      StairsShape shape = state.getValue(SHAPE);
      switch (mirror) {
         case LEFT_RIGHT:
            if (direction.getAxis() == Direction.Axis.Z) {
               switch (shape) {
                  case OUTER_LEFT:
                     return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                  case INNER_RIGHT:
                     return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                  case INNER_LEFT:
                     return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                  case OUTER_RIGHT:
                     return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                  default:
                     return state.rotate(Rotation.CLOCKWISE_180);
               }
            }
            break;
         case FRONT_BACK:
            if (direction.getAxis() == Direction.Axis.X) {
               switch (shape) {
                  case STRAIGHT:
                     return state.rotate(Rotation.CLOCKWISE_180);
                  case OUTER_LEFT:
                     return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                  case INNER_RIGHT:
                     return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                  case INNER_LEFT:
                     return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                  case OUTER_RIGHT:
                     return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
               }
            }
      }

      return super.mirror(state, mirror);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, HALF, SHAPE, WATERLOGGED);
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }
}
