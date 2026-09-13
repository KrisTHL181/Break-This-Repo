package net.minecraft.world.level.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class StainedGlassPaneBlock extends IronBarsBlock implements BeaconBeamBlock {
   private final DyeColor color;

   public StainedGlassPaneBlock(final DyeColor color, final BlockBehaviour.Properties properties) {
      super(properties);
      this.color = color;
      this.registerDefaultState(
         this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
      );
   }

   @Override
   public DyeColor getColor() {
      return this.color;
   }
}
