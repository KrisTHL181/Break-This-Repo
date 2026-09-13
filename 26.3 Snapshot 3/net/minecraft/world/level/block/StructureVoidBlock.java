package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StructureVoidBlock extends Block {
   private static final VoxelShape SHAPE = Block.cube(6.0);

   protected StructureVoidBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected RenderShape getRenderShape(final BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected float getShadeBrightness(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return 1.0F;
   }
}
