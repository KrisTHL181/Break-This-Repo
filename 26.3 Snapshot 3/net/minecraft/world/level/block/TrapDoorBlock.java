package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TrapDoorBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Block.boxZ(16.0, 13.0, 16.0));
   private final BlockSetType type;

   protected TrapDoorBlock(final BlockSetType type, final BlockBehaviour.Properties properties) {
      super(properties.sound(type.soundType()));
      this.type = type;
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false)
            .setValue(HALF, Half.BOTTOM)
            .setValue(POWERED, false)
            .setValue(WATERLOGGED, false)
      );
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES.get(state.getValue(OPEN) ? state.getValue(FACING) : (state.getValue(HALF) == Half.TOP ? Direction.DOWN : Direction.UP));
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return switch (type) {
         case LAND -> state.getValue(OPEN);
         case WATER -> state.getValue(WATERLOGGED);
         case AIR -> state.getValue(OPEN);
         default -> false;
      };
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (!this.type.canOpenByHand()) {
         return InteractionResult.PASS;
      }

      this.toggle(state, level, pos, player);
      return InteractionResult.SUCCESS;
   }

   @Override
   protected void onExplosionHit(
      final BlockState state, final ServerLevel level, final BlockPos pos, final Explosion explosion, final BiConsumer<ItemStack, BlockPos> onHit
   ) {
      if (explosion.canTriggerBlocks() && this.type.canOpenByWindCharge() && !state.getValue(POWERED)) {
         this.toggle(state, level, pos, null);
      }

      super.onExplosionHit(state, level, pos, explosion, onHit);
   }

   private void toggle(final BlockState state, final Level level, final BlockPos pos, final @Nullable Player player) {
      BlockState updated = state.cycle(OPEN);
      level.setBlock(pos, updated, 2);
      if (updated.getValue(WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      this.playSound(player, level, pos, updated.getValue(OPEN));
   }

   protected void playSound(final @Nullable Player player, final Level level, final BlockPos pos, final boolean opening) {
      level.playSound(
         player, pos, opening ? this.type.trapdoorOpen() : this.type.trapdoorClose(), SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F
      );
      level.gameEvent(player, opening ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
   }

   @Override
   protected void neighborChanged(
      BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      if (!level.isClientSide()) {
         boolean signal = level.hasNeighborSignal(pos);
         if (signal != state.getValue(POWERED)) {
            if (state.getValue(OPEN) != signal) {
               state = state.setValue(OPEN, signal);
               this.playSound(null, level, pos, signal);
            }

            level.setBlock(pos, state.setValue(POWERED, signal), 2);
            if (state.getValue(WATERLOGGED)) {
               level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      BlockState state = this.defaultBlockState();
      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      Direction clickedFace = context.getClickedFace();
      if (!context.replacingClickedOnBlock() && clickedFace.getAxis().isHorizontal()) {
         state = state.setValue(FACING, clickedFace)
            .setValue(HALF, context.getClickLocation().y - context.getClickedPos().getY() > 0.5 ? Half.TOP : Half.BOTTOM);
      } else {
         state = state.setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HALF, clickedFace == Direction.UP ? Half.BOTTOM : Half.TOP);
      }

      if (context.getLevel().hasNeighborSignal(context.getClickedPos())) {
         state = state.setValue(OPEN, true).setValue(POWERED, true);
      }

      return state.setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, OPEN, HALF, POWERED, WATERLOGGED);
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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

   protected BlockSetType getType() {
      return this.type;
   }
}
