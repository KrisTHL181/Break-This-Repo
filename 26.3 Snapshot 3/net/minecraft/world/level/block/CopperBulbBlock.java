package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class CopperBulbBlock extends Block {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;

   public CopperBulbBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.defaultBlockState().setValue(LIT, false).setValue(POWERED, false));
   }

   @Override
   protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
      if (oldState.getBlock() != state.getBlock() && level instanceof ServerLevel serverLevel) {
         this.checkAndFlip(state, serverLevel, pos);
      }
   }

   @Override
   protected void neighborChanged(
      final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      if (level instanceof ServerLevel serverLevel) {
         this.checkAndFlip(state, serverLevel, pos);
      }
   }

   public void checkAndFlip(final BlockState state, final ServerLevel level, final BlockPos pos) {
      boolean signal = level.hasNeighborSignal(pos);
      if (signal != state.getValue(POWERED)) {
         BlockState newState = state;
         if (!state.getValue(POWERED)) {
            newState = newState.cycle(LIT);
            level.playSound(null, pos, newState.getValue(LIT) ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS);
         }

         level.setBlockAndUpdate(pos, newState.setValue(POWERED, signal));
      }
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(LIT, POWERED);
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
      return level.getBlockState(pos).getValue(LIT) ? 15 : 0;
   }
}
