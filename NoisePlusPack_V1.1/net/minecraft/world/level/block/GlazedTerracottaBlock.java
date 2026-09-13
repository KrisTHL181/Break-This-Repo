package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class GlazedTerracottaBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<GlazedTerracottaBlock> CODEC = simpleCodec(GlazedTerracottaBlock::new);

   @Override
   public MapCodec<GlazedTerracottaBlock> codec() {
      return CODEC;
   }

   public GlazedTerracottaBlock(BlockBehaviour.Properties p_53677_) {
      super(p_53677_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_53681_) {
      p_53681_.add(FACING);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_53679_) {
      return this.defaultBlockState().setValue(FACING, p_53679_.getHorizontalDirection().getOpposite());
   }
}
