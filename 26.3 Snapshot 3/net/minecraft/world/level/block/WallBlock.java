package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final EnumProperty<WallSide> EAST = BlockStateProperties.EAST_WALL;
   public static final EnumProperty<WallSide> NORTH = BlockStateProperties.NORTH_WALL;
   public static final EnumProperty<WallSide> SOUTH = BlockStateProperties.SOUTH_WALL;
   public static final EnumProperty<WallSide> WEST = BlockStateProperties.WEST_WALL;
   public static final Map<Direction, EnumProperty<WallSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
      Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
   );
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private final Function<BlockState, VoxelShape> shapes;
   private final Function<BlockState, VoxelShape> collisionShapes;
   private static final VoxelShape TEST_SHAPE_POST = Block.column(2.0, 0.0, 16.0);
   private static final Map<Direction, VoxelShape> TEST_SHAPES_WALL = Shapes.rotateHorizontal(Block.boxZ(2.0, 16.0, 0.0, 9.0));

   public WallBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(UP, true)
            .setValue(NORTH, WallSide.NONE)
            .setValue(EAST, WallSide.NONE)
            .setValue(SOUTH, WallSide.NONE)
            .setValue(WEST, WallSide.NONE)
            .setValue(WATERLOGGED, false)
      );
      this.shapes = this.makeShapes(16.0F, 14.0F);
      this.collisionShapes = this.makeShapes(24.0F, 24.0F);
   }

   private Function<BlockState, VoxelShape> makeShapes(final float postHeight, final float wallTop) {
      VoxelShape post = Block.column(8.0, 0.0, postHeight);
      int width = 6;
      Map<Direction, VoxelShape> low = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, wallTop, 0.0, 11.0));
      Map<Direction, VoxelShape> tall = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, postHeight, 0.0, 11.0));
      return this.getShapeForEachState(state -> {
         VoxelShape shape = state.getValue(UP) ? post : Shapes.empty();

         for (Entry<Direction, EnumProperty<WallSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            shape = Shapes.or(shape, switch ((WallSide)state.getValue(entry.getValue())) {
               case NONE -> Shapes.empty();
               case LOW -> (VoxelShape)low.get(entry.getKey());
               case TALL -> (VoxelShape)tall.get(entry.getKey());
            });
         }

         return shape;
      }, WATERLOGGED);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return this.shapes.apply(state);
   }

   @Override
   protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return this.collisionShapes.apply(state);
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   private boolean connectsTo(final BlockState state, final boolean faceSolid, final Direction direction) {
      Block block = state.getBlock();
      boolean connectedFenceGate = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, direction);
      return state.is(BlockTags.WALLS) || !isExceptionForConnection(state) && faceSolid || block instanceof IronBarsBlock || connectedFenceGate;
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      LevelReader level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      BlockPos northPos = pos.north();
      BlockPos eastPos = pos.east();
      BlockPos southPos = pos.south();
      BlockPos westPos = pos.west();
      BlockPos topPos = pos.above();
      BlockState northState = level.getBlockState(northPos);
      BlockState eastState = level.getBlockState(eastPos);
      BlockState southState = level.getBlockState(southPos);
      BlockState westState = level.getBlockState(westPos);
      BlockState topState = level.getBlockState(topPos);
      boolean north = this.connectsTo(northState, northState.isFaceSturdy(level, northPos, Direction.SOUTH), Direction.SOUTH);
      boolean east = this.connectsTo(eastState, eastState.isFaceSturdy(level, eastPos, Direction.WEST), Direction.WEST);
      boolean south = this.connectsTo(southState, southState.isFaceSturdy(level, southPos, Direction.NORTH), Direction.NORTH);
      boolean west = this.connectsTo(westState, westState.isFaceSturdy(level, westPos, Direction.EAST), Direction.EAST);
      BlockState state = this.defaultBlockState().setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
      return this.updateShape(level, state, topPos, topState, north, east, south, west);
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

      if (directionToNeighbour == Direction.DOWN) {
         return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
      } else {
         return directionToNeighbour == Direction.UP
            ? this.topUpdate(level, state, neighbourPos, neighbourState)
            : this.sideUpdate(level, pos, state, neighbourPos, neighbourState, directionToNeighbour);
      }
   }

   private static boolean isConnected(final BlockState state, final Property<WallSide> northWall) {
      return state.getValue(northWall) != WallSide.NONE;
   }

   private static boolean isCovered(final VoxelShape aboveShape, final VoxelShape testShape) {
      return !Shapes.joinIsNotEmpty(testShape, aboveShape, BooleanOp.ONLY_FIRST);
   }

   private BlockState topUpdate(final LevelReader level, final BlockState state, final BlockPos topPos, final BlockState topNeighbour) {
      boolean north = isConnected(state, NORTH);
      boolean east = isConnected(state, EAST);
      boolean south = isConnected(state, SOUTH);
      boolean west = isConnected(state, WEST);
      return this.updateShape(level, state, topPos, topNeighbour, north, east, south, west);
   }

   private BlockState sideUpdate(
      final LevelReader level, final BlockPos pos, final BlockState state, final BlockPos neighbourPos, final BlockState neighbour, final Direction direction
   ) {
      Direction opposite = direction.getOpposite();
      boolean isNorthConnected = direction == Direction.NORTH
         ? this.connectsTo(neighbour, neighbour.isFaceSturdy(level, neighbourPos, opposite), opposite)
         : isConnected(state, NORTH);
      boolean isEastConnected = direction == Direction.EAST
         ? this.connectsTo(neighbour, neighbour.isFaceSturdy(level, neighbourPos, opposite), opposite)
         : isConnected(state, EAST);
      boolean isSouthConnected = direction == Direction.SOUTH
         ? this.connectsTo(neighbour, neighbour.isFaceSturdy(level, neighbourPos, opposite), opposite)
         : isConnected(state, SOUTH);
      boolean isWestConnected = direction == Direction.WEST
         ? this.connectsTo(neighbour, neighbour.isFaceSturdy(level, neighbourPos, opposite), opposite)
         : isConnected(state, WEST);
      BlockPos above = pos.above();
      BlockState aboveState = level.getBlockState(above);
      return this.updateShape(level, state, above, aboveState, isNorthConnected, isEastConnected, isSouthConnected, isWestConnected);
   }

   private BlockState updateShape(
      final LevelReader level,
      final BlockState state,
      final BlockPos topPos,
      final BlockState topNeighbour,
      final boolean north,
      final boolean east,
      final boolean south,
      final boolean west
   ) {
      VoxelShape aboveShape = topNeighbour.getCollisionShape(level, topPos).getFaceShape(Direction.DOWN);
      BlockState sidesUpdatedState = this.updateSides(state, north, east, south, west, aboveShape);
      return sidesUpdatedState.setValue(UP, this.shouldRaisePost(sidesUpdatedState, topNeighbour, aboveShape));
   }

   private boolean shouldRaisePost(final BlockState state, final BlockState topNeighbour, final VoxelShape aboveShape) {
      boolean topNeighbourHasPost = topNeighbour.getBlock() instanceof WallBlock && topNeighbour.getValue(UP);
      if (topNeighbourHasPost) {
         return true;
      }

      WallSide northWall = state.getValue(NORTH);
      WallSide southWall = state.getValue(SOUTH);
      WallSide eastWall = state.getValue(EAST);
      WallSide westWall = state.getValue(WEST);
      boolean southNone = southWall == WallSide.NONE;
      boolean westNone = westWall == WallSide.NONE;
      boolean eastNone = eastWall == WallSide.NONE;
      boolean northNone = northWall == WallSide.NONE;
      boolean hasCorner = northNone && southNone && westNone && eastNone || northNone != southNone || westNone != eastNone;
      if (hasCorner) {
         return true;
      }

      boolean hasHighWall = northWall == WallSide.TALL && southWall == WallSide.TALL || eastWall == WallSide.TALL && westWall == WallSide.TALL;
      return hasHighWall ? false : topNeighbour.is(BlockTags.WALL_POST_OVERRIDE) || isCovered(aboveShape, TEST_SHAPE_POST);
   }

   private BlockState updateSides(
      final BlockState state,
      final boolean northConnection,
      final boolean eastConnection,
      final boolean southConnection,
      final boolean westConnection,
      final VoxelShape aboveShape
   ) {
      return state.setValue(NORTH, this.makeWallState(northConnection, aboveShape, TEST_SHAPES_WALL.get(Direction.NORTH)))
         .setValue(EAST, this.makeWallState(eastConnection, aboveShape, TEST_SHAPES_WALL.get(Direction.EAST)))
         .setValue(SOUTH, this.makeWallState(southConnection, aboveShape, TEST_SHAPES_WALL.get(Direction.SOUTH)))
         .setValue(WEST, this.makeWallState(westConnection, aboveShape, TEST_SHAPES_WALL.get(Direction.WEST)));
   }

   private WallSide makeWallState(final boolean connectsToSide, final VoxelShape aboveShape, final VoxelShape testShape) {
      if (connectsToSide) {
         return isCovered(aboveShape, testShape) ? WallSide.TALL : WallSide.LOW;
      } else {
         return WallSide.NONE;
      }
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   protected boolean propagatesSkylightDown(final BlockState state) {
      return !state.getValue(WATERLOGGED);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(UP, NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return switch (rotation) {
         case CLOCKWISE_180 -> (BlockState)state.setValue(NORTH, state.getValue(SOUTH))
            .setValue(EAST, state.getValue(WEST))
            .setValue(SOUTH, state.getValue(NORTH))
            .setValue(WEST, state.getValue(EAST));
         case COUNTERCLOCKWISE_90 -> (BlockState)state.setValue(NORTH, state.getValue(EAST))
            .setValue(EAST, state.getValue(SOUTH))
            .setValue(SOUTH, state.getValue(WEST))
            .setValue(WEST, state.getValue(NORTH));
         case CLOCKWISE_90 -> (BlockState)state.setValue(NORTH, state.getValue(WEST))
            .setValue(EAST, state.getValue(NORTH))
            .setValue(SOUTH, state.getValue(EAST))
            .setValue(WEST, state.getValue(SOUTH));
         default -> state;
      };
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      switch (mirror) {
         case LEFT_RIGHT:
            return state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
         case FRONT_BACK:
            return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
         default:
            return super.mirror(state, mirror);
      }
   }
}
