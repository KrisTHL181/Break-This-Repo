package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NetherRootsBlock extends VegetationBlock {
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);
   private final TagKey<Block> supportBlocks;

   protected NetherRootsBlock(final TagKey<Block> supportBlocks, final BlockBehaviour.Properties properties) {
      super(properties);
      this.supportBlocks = supportBlocks;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return state.is(this.supportBlocks);
   }
}
