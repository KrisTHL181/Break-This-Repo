package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TintedGlassBlock extends TransparentBlock {
   public TintedGlassBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected boolean propagatesSkylightDown(final BlockState state) {
      return false;
   }

   @Override
   protected int getLightDampening(final BlockState state) {
      return 15;
   }
}
