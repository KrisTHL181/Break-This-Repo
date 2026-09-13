package net.minecraft.world.level.block;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MultifaceBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;
   protected static final Direction[] DIRECTIONS = Direction.values();
   private final Function<BlockState, VoxelShape> shapes;
   private final boolean canRotate;
   private final boolean canMirrorX;
   private final boolean canMirrorZ;

   public MultifaceBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(getDefaultMultifaceState(this.stateDefinition));
      this.shapes = this.makeShapes();
      this.canRotate = Direction.Plane.HORIZONTAL.stream().allMatch(this::isFaceSupported);
      this.canMirrorX = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.X).filter(this::isFaceSupported).count() % 2L == 0L;
      this.canMirrorZ = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.Z).filter(this::isFaceSupported).count() % 2L == 0L;
   }

   private Function<BlockState, VoxelShape> makeShapes() {
      Map<Direction, VoxelShape> shapes = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
      return this.getShapeForEachState(state -> {
         VoxelShape shape = Shapes.empty();

         for (Direction direction : DIRECTIONS) {
            if (hasFace(state, direction)) {
               shape = Shapes.or(shape, shapes.get(direction));
            }
         }

         return shape.isEmpty() ? Shapes.block() : shape;
      }, WATERLOGGED);
   }

   public static Set<Direction> availableFaces(final BlockState state) {
      if (!(state.getBlock() instanceof MultifaceBlock)) {
         return Set.of();
      }

      Set<Direction> faces = EnumSet.noneOf(Direction.class);

      for (Direction direction : Direction.values()) {
         if (hasFace(state, direction)) {
            faces.add(direction);
         }
      }

      return faces;
   }

   public static Set<Direction> unpack(final byte data) {
      Set<Direction> presentDirections = EnumSet.noneOf(Direction.class);

      for (Direction direction : Direction.values()) {
         if ((data & (byte)(1 << direction.ordinal())) > 0) {
            presentDirections.add(direction);
         }
      }

      return presentDirections;
   }

   public static byte pack(final Collection<Direction> directions) {
      byte code = 0;

      for (Direction direction : directions) {
         code = (byte)(code | 1 << direction.ordinal());
      }

      return code;
   }

   protected boolean isFaceSupported(final Direction faceDirection) {
      return true;
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      for (Direction direction : DIRECTIONS) {
         if (this.isFaceSupported(direction)) {
            builder.add(getFaceProperty(direction));
         }
      }

      builder.add(WATERLOGGED);
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

      if (!hasAnyFace(state)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         return hasFace(state, directionToNeighbour) && !canAttachTo(level, directionToNeighbour, neighbourPos, neighbourState)
            ? removeFace(state, getFaceProperty(directionToNeighbour))
            : state;
      }
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return this.shapes.apply(state);
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      boolean hasAtLeastOneFace = false;

      for (Direction directionToNeighbour : DIRECTIONS) {
         if (hasFace(state, directionToNeighbour)) {
            if (!canAttachTo(level, pos, directionToNeighbour)) {
               return false;
            }

            hasAtLeastOneFace = true;
         }
      }

      return hasAtLeastOneFace;
   }

   @Override
   protected boolean canBeReplaced(final BlockState state, final BlockPlaceContext context) {
      return !context.getItemInHand().is(this.asItem()) || hasAnyVacantFace(state);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
      Level level = context.getLevel();
      BlockPos placePos = context.getClickedPos();
      BlockState oldState = level.getBlockState(placePos);
      return Arrays.stream(context.getNearestLookingDirections())
         .map(direction -> this.getStateForPlacement(oldState, level, placePos, direction))
         .filter(Objects::nonNull)
         .findFirst()
         .orElse(null);
   }

   public boolean isValidStateForPlacement(final BlockGetter level, final BlockState oldState, final BlockPos placementPos, final Direction placementDirection) {
      if (this.isFaceSupported(placementDirection) && (!oldState.is(this) || !hasFace(oldState, placementDirection))) {
         BlockPos neighbourPos = placementPos.relative(placementDirection);
         return canAttachTo(level, placementDirection, neighbourPos, level.getBlockState(neighbourPos));
      } else {
         return false;
      }
   }

   public @Nullable BlockState getStateForPlacement(
      final BlockState oldState, final BlockGetter level, final BlockPos placementPos, final Direction placementDirection
   ) {
      if (!this.isValidStateForPlacement(level, oldState, placementPos, placementDirection)) {
         return null;
      }

      BlockState newState;
      if (oldState.is(this)) {
         newState = oldState;
      } else if (oldState.getFluidState().isSourceOfType(Fluids.WATER)) {
         newState = this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
      } else {
         newState = this.defaultBlockState();
      }

      return newState.setValue(getFaceProperty(placementDirection), true);
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return !this.canRotate ? state : this.mapDirections(state, rotation::rotate);
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      if (mirror == Mirror.FRONT_BACK && !this.canMirrorX) {
         return state;
      } else {
         return mirror == Mirror.LEFT_RIGHT && !this.canMirrorZ ? state : this.mapDirections(state, mirror::mirror);
      }
   }

   private BlockState mapDirections(final BlockState state, final Function<Direction, Direction> mapping) {
      BlockState newState = state;

      for (Direction direction : DIRECTIONS) {
         if (this.isFaceSupported(direction)) {
            newState = newState.setValue(getFaceProperty(mapping.apply(direction)), state.getValue(getFaceProperty(direction)));
         }
      }

      return newState;
   }

   public static boolean hasFace(final BlockState state, final Direction faceDirection) {
      BooleanProperty property = getFaceProperty(faceDirection);
      return state.getValueOrElse(property, false);
   }

   public static boolean canAttachTo(final BlockGetter level, final BlockPos pos, final Direction directionTowardsNeighbour) {
      BlockPos neighbourPos = pos.relative(directionTowardsNeighbour);
      BlockState blockState = level.getBlockState(neighbourPos);
      return canAttachTo(level, directionTowardsNeighbour, neighbourPos, blockState);
   }

   public static boolean canAttachTo(
      final BlockGetter level, final Direction directionTowardsNeighbour, final BlockPos neighbourPos, final BlockState neighbourState
   ) {
      return Block.isFaceFull(neighbourState.getBlockSupportShape(level, neighbourPos), directionTowardsNeighbour.getOpposite())
         || Block.isFaceFull(neighbourState.getCollisionShape(level, neighbourPos), directionTowardsNeighbour.getOpposite());
   }

   private static BlockState removeFace(final BlockState state, final BooleanProperty property) {
      BlockState newState = state.setValue(property, false);
      return hasAnyFace(newState) ? newState : Blocks.AIR.defaultBlockState();
   }

   public static BooleanProperty getFaceProperty(final Direction faceDirection) {
      return PROPERTY_BY_DIRECTION.get(faceDirection);
   }

   private static BlockState getDefaultMultifaceState(final StateDefinition<Block, BlockState> stateDefinition) {
      BlockState state = stateDefinition.any().setValue(WATERLOGGED, false);

      for (BooleanProperty faceProperty : PROPERTY_BY_DIRECTION.values()) {
         state = state.trySetValue(faceProperty, false);
      }

      return state;
   }

   protected static boolean hasAnyFace(final BlockState state) {
      for (Direction direction : DIRECTIONS) {
         if (hasFace(state, direction)) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasAnyVacantFace(final BlockState state) {
      for (Direction direction : DIRECTIONS) {
         if (!hasFace(state, direction)) {
            return true;
         }
      }

      return false;
   }
}
