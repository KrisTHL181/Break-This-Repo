package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TintedGlassBlock extends TransparentBlock {
   public static final MapCodec<TintedGlassBlock> CODEC = simpleCodec(TintedGlassBlock::new);

   @Override
   public MapCodec<TintedGlassBlock> codec() {
      return CODEC;
   }

   public TintedGlassBlock(BlockBehaviour.Properties p_154822_) {
      super(p_154822_);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_154824_) {
      return false;
   }

   @Override
   protected int getLightBlock(BlockState p_154828_) {
      return 15;
   }
}
