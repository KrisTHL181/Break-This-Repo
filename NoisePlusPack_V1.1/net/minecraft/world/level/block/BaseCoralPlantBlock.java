package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BaseCoralPlantBlock extends BaseCoralPlantTypeBlock {
   public static final MapCodec<BaseCoralPlantBlock> CODEC = simpleCodec(BaseCoralPlantBlock::new);
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 15.0);

   @Override
   public MapCodec<BaseCoralPlantBlock> codec() {
      return CODEC;
   }

   protected BaseCoralPlantBlock(BlockBehaviour.Properties p_49151_) {
      super(p_49151_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_49153_, BlockGetter p_49154_, BlockPos p_49155_, CollisionContext p_49156_) {
      return SHAPE;
   }
}
