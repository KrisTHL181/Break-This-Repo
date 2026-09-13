package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BellBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<BellAttachType> ATTACHMENT = BlockStateProperties.BELL_ATTACHMENT;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final VoxelShape BELL_SHAPE = Shapes.or(Block.column(6.0, 6.0, 13.0), Block.column(8.0, 4.0, 6.0));
   private static final VoxelShape SHAPE_CEILING = Shapes.or(BELL_SHAPE, Block.column(2.0, 13.0, 16.0));
   private static final Map<Direction.Axis, VoxelShape> SHAPE_FLOOR = Shapes.rotateHorizontalAxis(Block.cube(16.0, 16.0, 8.0));
   private static final Map<Direction.Axis, VoxelShape> SHAPE_DOUBLE_WALL = Shapes.rotateHorizontalAxis(
      Shapes.or(BELL_SHAPE, Block.column(2.0, 16.0, 13.0, 15.0))
   );
   private static final Map<Direction, VoxelShape> SHAPE_SINGLE_WALL = Shapes.rotateHorizontal(Shapes.or(BELL_SHAPE, Block.boxZ(2.0, 13.0, 15.0, 0.0, 13.0)));
   public static final int EVENT_BELL_RING = 1;

   public BellBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ATTACHMENT, BellAttachType.FLOOR).setValue(POWERED, false)
      );
   }

   @Override
   protected void neighborChanged(
      final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      boolean signal = level.hasNeighborSignal(pos);
      if (signal != state.getValue(POWERED)) {
         if (signal) {
            this.attemptToRing(level, pos, null);
         }

         level.setBlockAndUpdate(pos, state.setValue(POWERED, signal));
      }
   }

   @Override
   protected void onProjectileHit(final Level level, final BlockState state, final BlockHitResult hitResult, final Projectile projectile) {
      Player playerOwner = projectile.getOwner() instanceof Player player ? player : null;
      this.onHit(level, state, hitResult, playerOwner, true);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      return this.onHit(level, state, hitResult, player, true) ? InteractionResult.SUCCESS : InteractionResult.PASS;
   }

   public boolean onHit(
      final Level level, final BlockState state, final BlockHitResult hitResult, final @Nullable Player player, final boolean requireHitFromCorrectSide
   ) {
      Direction direction = hitResult.getDirection();
      BlockPos blockPos = hitResult.getBlockPos();
      boolean properHit = !requireHitFromCorrectSide || this.isProperHit(state, direction, hitResult.getLocation().y - blockPos.getY());
      if (properHit) {
         boolean didRing = this.attemptToRing(player, level, blockPos, direction);
         if (didRing && player != null) {
            player.awardStat(Stats.BELL_RING);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isProperHit(final BlockState state, final Direction clickedDirection, final double clickY) {
      if (clickedDirection.getAxis() != Direction.Axis.Y && !(clickY > 0.8124F)) {
         Direction facing = state.getValue(FACING);
         BellAttachType attachType = state.getValue(ATTACHMENT);

         return switch (attachType) {
            case FLOOR -> facing.getAxis() == clickedDirection.getAxis();
            case SINGLE_WALL, DOUBLE_WALL -> facing.getAxis() != clickedDirection.getAxis();
            case CEILING -> true;
            default -> false;
         };
      } else {
         return false;
      }
   }

   public boolean attemptToRing(final Level level, final BlockPos pos, final @Nullable Direction direction) {
      return this.attemptToRing(null, level, pos, direction);
   }

   public boolean attemptToRing(final @Nullable Entity ringingEntity, final Level level, final BlockPos pos, @Nullable Direction direction) {
      if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BellBlockEntity bellBlockEntity) {
         if (direction == null) {
            direction = level.getBlockState(pos).getValue(FACING);
         }

         bellBlockEntity.onHit(direction);
         level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
         level.gameEvent(ringingEntity, GameEvent.BLOCK_CHANGE, pos);
         return true;
      } else {
         return false;
      }
   }

   private VoxelShape getVoxelShape(final BlockState state) {
      Direction facing = state.getValue(FACING);

      return switch ((BellAttachType)state.getValue(ATTACHMENT)) {
         case FLOOR -> (VoxelShape)SHAPE_FLOOR.get(facing.getAxis());
         case SINGLE_WALL -> (VoxelShape)SHAPE_SINGLE_WALL.get(facing);
         case DOUBLE_WALL -> (VoxelShape)SHAPE_DOUBLE_WALL.get(facing.getAxis());
         case CEILING -> SHAPE_CEILING;
      };
   }

   @Override
   protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return this.getVoxelShape(state);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return this.getVoxelShape(state);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
      Direction clickedFace = context.getClickedFace();
      BlockPos pos = context.getClickedPos();
      Level level = context.getLevel();
      Direction.Axis axis = clickedFace.getAxis();
      if (axis == Direction.Axis.Y) {
         BlockState state = this.defaultBlockState()
            .setValue(ATTACHMENT, clickedFace == Direction.DOWN ? BellAttachType.CEILING : BellAttachType.FLOOR)
            .setValue(FACING, context.getHorizontalDirection());
         if (state.canSurvive(context.getLevel(), pos)) {
            return state;
         }
      } else {
         boolean doubleAttached = axis == Direction.Axis.X
               && level.getBlockState(pos.west()).isFaceSturdy(level, pos.west(), Direction.EAST)
               && level.getBlockState(pos.east()).isFaceSturdy(level, pos.east(), Direction.WEST)
            || axis == Direction.Axis.Z
               && level.getBlockState(pos.north()).isFaceSturdy(level, pos.north(), Direction.SOUTH)
               && level.getBlockState(pos.south()).isFaceSturdy(level, pos.south(), Direction.NORTH);
         BlockState state = this.defaultBlockState()
            .setValue(FACING, clickedFace.getOpposite())
            .setValue(ATTACHMENT, doubleAttached ? BellAttachType.DOUBLE_WALL : BellAttachType.SINGLE_WALL);
         if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
            return state;
         }

         boolean canAttachBelow = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
         state = state.setValue(ATTACHMENT, canAttachBelow ? BellAttachType.FLOOR : BellAttachType.CEILING);
         if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
            return state;
         }
      }

      return null;
   }

   @Override
   protected void onExplosionHit(
      final BlockState state, final ServerLevel level, final BlockPos pos, final Explosion explosion, final BiConsumer<ItemStack, BlockPos> onHit
   ) {
      if (explosion.canTriggerBlocks()) {
         this.attemptToRing(level, pos, null);
      }

      super.onExplosionHit(state, level, pos, explosion, onHit);
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
      BellAttachType attachment = state.getValue(ATTACHMENT);
      Direction connectedDirection = getConnectedDirection(state).getOpposite();
      if (connectedDirection == directionToNeighbour && !state.canSurvive(level, pos) && attachment != BellAttachType.DOUBLE_WALL) {
         return Blocks.AIR.defaultBlockState();
      }

      if (directionToNeighbour.getAxis() == state.getValue(FACING).getAxis()) {
         if (attachment == BellAttachType.DOUBLE_WALL && !neighbourState.isFaceSturdy(level, neighbourPos, directionToNeighbour)) {
            return state.setValue(ATTACHMENT, BellAttachType.SINGLE_WALL).setValue(FACING, directionToNeighbour.getOpposite());
         }

         if (attachment == BellAttachType.SINGLE_WALL
            && connectedDirection.getOpposite() == directionToNeighbour
            && neighbourState.isFaceSturdy(level, neighbourPos, state.getValue(FACING))) {
            return state.setValue(ATTACHMENT, BellAttachType.DOUBLE_WALL);
         }
      }

      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      Direction connectionDir = getConnectedDirection(state).getOpposite();
      return connectionDir == Direction.UP
         ? Block.canSupportCenter(level, pos.above(), Direction.DOWN)
         : FaceAttachedHorizontalDirectionalBlock.canAttach(level, pos, connectionDir);
   }

   private static Direction getConnectedDirection(final BlockState state) {
      return switch ((BellAttachType)state.getValue(ATTACHMENT)) {
         case FLOOR -> Direction.UP;
         case CEILING -> Direction.DOWN;
         default -> state.getValue(FACING).getOpposite();
      };
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, ATTACHMENT, POWERED);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new BellBlockEntity(worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      return createTickerHelper(type, BlockEntityTypes.BELL, level.isClientSide() ? BellBlockEntity::clientTick : BellBlockEntity::serverTick);
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   @Override
   public BlockState rotate(final BlockState state, final Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   public BlockState mirror(final BlockState state, final Mirror mirror) {
      return state.rotate(mirror.getRotation(state.getValue(FACING)));
   }
}
