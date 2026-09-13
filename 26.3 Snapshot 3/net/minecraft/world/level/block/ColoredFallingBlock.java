package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ColoredFallingBlock extends FallingBlock {
   protected final ColorRGBA dustColor;

   public ColoredFallingBlock(final ColorRGBA dustColor, final BlockBehaviour.Properties properties) {
      super(properties);
      this.dustColor = dustColor;
   }

   @Override
   public int getDustColor(final BlockState blockState, final BlockGetter level, final BlockPos pos) {
      return this.dustColor.rgba();
   }
}
