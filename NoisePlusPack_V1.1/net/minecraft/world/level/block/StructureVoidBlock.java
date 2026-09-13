package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StructureVoidBlock extends Block {
   public static final MapCodec<StructureVoidBlock> CODEC = simpleCodec(StructureVoidBlock::new);
   private static final VoxelShape SHAPE = Block.cube(6.0);

   @Override
   public MapCodec<StructureVoidBlock> codec() {
      return CODEC;
   }

   protected StructureVoidBlock(BlockBehaviour.Properties p_57150_) {
      super(p_57150_);
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_57156_) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected VoxelShape getShape(BlockState p_57158_, BlockGetter p_57159_, BlockPos p_57160_, CollisionContext p_57161_) {
      return SHAPE;
   }

   @Override
   protected float getShadeBrightness(BlockState p_57152_, BlockGetter p_57153_, BlockPos p_57154_) {
      return 1.0F;
   }
}
