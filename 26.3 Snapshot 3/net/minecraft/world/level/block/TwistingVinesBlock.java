package net.minecraft.world.level.block;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TwistingVinesBlock extends GrowingPlantHeadBlock {
   private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 15.0);

   public TwistingVinesBlock(final BlockBehaviour.Properties properties) {
      super(properties, Direction.UP, SHAPE, false, 0.1);
   }

   @Override
   protected int getBlocksToGrowWhenBonemealed(final RandomSource random) {
      return NetherVines.getBlocksToGrowWhenBonemealed(random);
   }

   @Override
   protected Block getBodyBlock() {
      return Blocks.TWISTING_VINES_PLANT;
   }

   @Override
   protected boolean canGrowInto(final BlockState state) {
      return NetherVines.isValidGrowthState(state);
   }
}
