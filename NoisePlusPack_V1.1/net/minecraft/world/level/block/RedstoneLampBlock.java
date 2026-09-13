package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class RedstoneLampBlock extends Block {
   public static final MapCodec<RedstoneLampBlock> CODEC = simpleCodec(RedstoneLampBlock::new);
   public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

   @Override
   public MapCodec<RedstoneLampBlock> codec() {
      return CODEC;
   }

   public RedstoneLampBlock(BlockBehaviour.Properties p_55657_) {
      super(p_55657_);
      this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_55659_) {
      return this.defaultBlockState().setValue(LIT, p_55659_.getLevel().hasNeighborSignal(p_55659_.getClickedPos()));
   }

   @Override
   protected void neighborChanged(BlockState p_55666_, Level p_55667_, BlockPos p_55668_, Block p_55669_, @Nullable Orientation p_369944_, boolean p_55671_) {
      if (!p_55667_.isClientSide()) {
         boolean flag = p_55666_.getValue(LIT);
         if (flag != p_55667_.hasNeighborSignal(p_55668_)) {
            if (flag) {
               p_55667_.scheduleTick(p_55668_, this, 4);
            } else {
               p_55667_.setBlock(p_55668_, p_55666_.cycle(LIT), 2);
            }
         }
      }
   }

   @Override
   protected void tick(BlockState p_221937_, ServerLevel p_221938_, BlockPos p_221939_, RandomSource p_221940_) {
      if (p_221937_.getValue(LIT) && !p_221938_.hasNeighborSignal(p_221939_)) {
         p_221938_.setBlock(p_221939_, p_221937_.cycle(LIT), 2);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55673_) {
      p_55673_.add(LIT);
   }
}
