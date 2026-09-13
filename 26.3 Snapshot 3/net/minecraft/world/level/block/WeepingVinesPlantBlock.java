package net.minecraft.world.level.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WeepingVinesPlantBlock extends GrowingPlantBodyBlock {
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

   public WeepingVinesPlantBlock(final BlockBehaviour.Properties properties) {
      super(properties, Direction.DOWN, SHAPE, false);
   }

   @Override
   protected GrowingPlantHeadBlock getHeadBlock() {
      return (GrowingPlantHeadBlock)Blocks.WEEPING_VINES;
   }
}
