package net.minecraft.world.level.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TwistingVinesPlantBlock extends GrowingPlantBodyBlock {
   private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);

   public TwistingVinesPlantBlock(final BlockBehaviour.Properties properties) {
      super(properties, Direction.UP, SHAPE, false);
   }

   @Override
   protected GrowingPlantHeadBlock getHeadBlock() {
      return (GrowingPlantHeadBlock)Blocks.TWISTING_VINES;
   }
}
