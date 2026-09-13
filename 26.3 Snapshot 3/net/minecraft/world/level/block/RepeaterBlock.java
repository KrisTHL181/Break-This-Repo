package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RepeaterBlock extends DiodeBlock {
   public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;
   public static final IntegerProperty DELAY = BlockStateProperties.DELAY;

   protected RepeaterBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DELAY, 1).setValue(LOCKED, false).setValue(POWERED, false)
      );
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (!player.getAbilities().mayBuild) {
         return InteractionResult.PASS;
      }

      level.setBlockAndUpdate(pos, state.cycle(DELAY));
      return InteractionResult.SUCCESS;
   }

   @Override
   protected int getDelay(final BlockState state) {
      return state.getValue(DELAY) * 2;
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      return state.setValue(LOCKED, this.isLocked(context.getLevel(), context.getClickedPos(), state));
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
      if (directionToNeighbour == Direction.DOWN && !this.canSurviveOn(level, neighbourPos, neighbourState)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         return !level.isClientSide() && directionToNeighbour.getAxis() != state.getValue(FACING).getAxis()
            ? state.setValue(LOCKED, this.isLocked(level, pos, state))
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
      }
   }

   @Override
   public boolean isLocked(final LevelReader level, final BlockPos pos, final BlockState state) {
      return this.getAlternateSignal(level, pos, state) > 0;
   }

   @Override
   protected boolean sideInputDiodesOnly() {
      return true;
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      if (state.getValue(POWERED)) {
         Direction direction = state.getValue(FACING);
         double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
         double y = pos.getY() + 0.4 + (random.nextDouble() - 0.5) * 0.2;
         double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
         float offset = -5.0F;
         if (random.nextBoolean()) {
            offset = state.getValue(DELAY) * 2 - 1;
         }

         offset /= 16.0F;
         double xo = offset * direction.getStepX();
         double zo = offset * direction.getStepZ();
         level.addParticle(DustParticleOptions.REDSTONE, x + xo, y, z + zo, 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, DELAY, LOCKED, POWERED);
   }
}
