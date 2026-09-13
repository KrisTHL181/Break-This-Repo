package net.minecraft.world.level.block;

import com.google.common.base.MoreObjects;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TripWireHookBlock extends Block {
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
   protected static final int WIRE_DIST_MIN = 1;
   protected static final int WIRE_DIST_MAX = 42;
   private static final int RECHECK_PERIOD = 10;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, 10.0, 10.0, 16.0));

   public TripWireHookBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(ATTACHED, false));
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES.get(state.getValue(FACING));
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      Direction direction = state.getValue(FACING);
      BlockPos relative = pos.relative(direction.getOpposite());
      BlockState blockState = level.getBlockState(relative);
      return direction.getAxis().isHorizontal() && blockState.isFaceSturdy(level, relative, direction);
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
      return directionToNeighbour.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
      BlockState state = this.defaultBlockState().setValue(POWERED, false).setValue(ATTACHED, false);
      LevelReader level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      Direction[] directions = context.getNearestLookingDirections();

      for (Direction direction : directions) {
         if (direction.getAxis().isHorizontal()) {
            Direction facing = direction.getOpposite();
            state = state.setValue(FACING, facing);
            if (state.canSurvive(level, pos)) {
               return state;
            }
         }
      }

      return null;
   }

   @Override
   public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
      calculateState(level, pos, state, false, false, -1, null);
   }

   public static void calculateState(
      final Level level,
      final BlockPos pos,
      final BlockState state,
      final boolean isBeingDestroyed,
      final boolean canUpdate,
      final int wireSource,
      final @Nullable BlockState wireSourceState
   ) {
      Optional<Direction> facingOptional = state.getOptionalValue(FACING);
      if (facingOptional.isPresent()) {
         Direction direction = facingOptional.get();
         boolean wasAttached = state.getOptionalValue(ATTACHED).orElse(false);
         boolean wasPowered = state.getOptionalValue(POWERED).orElse(false);
         Block block = state.getBlock();
         boolean attached = !isBeingDestroyed;
         boolean powered = false;
         int receiverPos = 0;
         BlockState[] wireStates = new BlockState[42];

         for (int i = 1; i < 42; i++) {
            BlockPos testPos = pos.relative(direction, i);
            BlockState wireState = level.getBlockState(testPos);
            if (wireState.is(Blocks.TRIPWIRE_HOOK)) {
               if (wireState.getValue(FACING) == direction.getOpposite()) {
                  receiverPos = i;
               }
               break;
            }

            if (!wireState.is(Blocks.TRIPWIRE) && i != wireSource) {
               wireStates[i] = null;
               attached = false;
            } else {
               if (i == wireSource) {
                  wireState = (BlockState)MoreObjects.firstNonNull(wireSourceState, wireState);
               }

               boolean wireArmed = !wireState.getValue(TripWireBlock.DISARMED);
               boolean wirePowered = wireState.getValue(TripWireBlock.POWERED);
               powered |= wireArmed && wirePowered;
               wireStates[i] = wireState;
               if (i == wireSource) {
                  level.scheduleTick(pos, block, 10);
                  attached &= wireArmed;
               }
            }
         }

         attached &= receiverPos > 1;
         powered &= attached;
         BlockState newState = block.defaultBlockState().trySetValue(ATTACHED, attached).trySetValue(POWERED, powered);
         if (receiverPos > 0) {
            BlockPos testPos = pos.relative(direction, receiverPos);
            Direction opposite = direction.getOpposite();
            level.setBlockAndUpdate(testPos, newState.setValue(FACING, opposite));
            notifyNeighbors(block, level, testPos, opposite);
            if (!level.getBlockState(pos).is(Blocks.TRIPWIRE_HOOK)) {
               onRemoved(newState, level, pos);
               return;
            }

            emitState(level, testPos, attached, powered, wasAttached, wasPowered);
         }

         emitState(level, pos, attached, powered, wasAttached, wasPowered);
         if (!isBeingDestroyed) {
            level.setBlockAndUpdate(pos, newState.setValue(FACING, direction));
            if (canUpdate) {
               notifyNeighbors(block, level, pos, direction);
            }
         }

         if (wasAttached != attached) {
            for (int i = 1; i < receiverPos; i++) {
               BlockPos testPos = pos.relative(direction, i);
               BlockState wireData = wireStates[i];
               if (wireData != null) {
                  BlockState testPosState = level.getBlockState(testPos);
                  if (testPosState.is(Blocks.TRIPWIRE) || testPosState.is(Blocks.TRIPWIRE_HOOK)) {
                     level.setBlockAndUpdate(testPos, wireData.trySetValue(ATTACHED, attached));
                  }
               }
            }
         }
      }
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      calculateState(level, pos, state, false, true, -1, null);
   }

   private static void emitState(
      final Level level, final BlockPos pos, final boolean attached, final boolean powered, final boolean wasAttached, final boolean wasPowered
   ) {
      if (powered && !wasPowered) {
         level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.6F);
         level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pos);
      } else if (!powered && wasPowered) {
         level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 0.5F);
         level.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, pos);
      } else if (attached && !wasAttached) {
         level.playSound(null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.4F, 0.7F);
         level.gameEvent(null, GameEvent.BLOCK_ATTACH, pos);
      } else if (!attached && wasAttached) {
         level.playSound(null, pos, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.4F, 1.2F / (level.getRandom().nextFloat() * 0.2F + 0.9F));
         level.gameEvent(null, GameEvent.BLOCK_DETACH, pos);
      }
   }

   private static void notifyNeighbors(final Block block, final Level level, final BlockPos pos, final Direction direction) {
      Direction front = direction.getOpposite();
      Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, front, Direction.UP);
      level.updateNeighborsAt(pos, block, orientation);
      level.updateNeighborsAt(pos.relative(front), block, orientation);
   }

   @Override
   protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
      if (!movedByPiston) {
         onRemoved(state, level, pos);
      }
   }

   private static void onRemoved(final BlockState state, final Level level, final BlockPos pos) {
      boolean attached = state.getValue(ATTACHED);
      boolean powered = state.getValue(POWERED);
      if (attached || powered) {
         calculateState(level, pos, state, true, false, -1, null);
      }

      if (powered) {
         notifyNeighbors(state.getBlock(), level, pos, state.getValue(FACING));
      }
   }

   @Override
   protected int ownSignal(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return state.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos, final Direction direction) {
      if (!state.getValue(POWERED)) {
         return 0;
      } else {
         return state.getValue(FACING) == direction ? 15 : 0;
      }
   }

   @Override
   protected boolean isSignalSource(final BlockState state) {
      return true;
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      return state.rotate(mirror.getRotation(state.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, POWERED, ATTACHED);
   }
}
