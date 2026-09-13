package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CactusFlowerBlock extends VegetationBlock {
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 12.0);

   public CactusFlowerBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(final BlockState state, final BlockGetter level, final BlockPos pos) {
      BlockState blockBelow = level.getBlockState(pos);
      return blockBelow.is(BlockTags.SUPPORT_OVERRIDE_CACTUS_FLOWER) || blockBelow.isFaceSturdy(level, pos, Direction.UP, SupportType.CENTER);
   }
}
