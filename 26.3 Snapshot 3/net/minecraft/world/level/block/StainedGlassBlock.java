package net.minecraft.world.level.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class StainedGlassBlock extends TransparentBlock implements BeaconBeamBlock {
   private final DyeColor color;

   public StainedGlassBlock(final DyeColor color, final BlockBehaviour.Properties properties) {
      super(properties);
      this.color = color;
   }

   @Override
   public DyeColor getColor() {
      return this.color;
   }
}
