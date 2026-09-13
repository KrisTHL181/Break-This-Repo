package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BaseCoralFanBlock extends BaseCoralPlantTypeBlock {
   public static final MapCodec<BaseCoralFanBlock> CODEC = simpleCodec(BaseCoralFanBlock::new);
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 4.0);

   @Override
   public MapCodec<? extends BaseCoralFanBlock> codec() {
      return CODEC;
   }

   protected BaseCoralFanBlock(BlockBehaviour.Properties p_49106_) {
      super(p_49106_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_49108_, BlockGetter p_49109_, BlockPos p_49110_, CollisionContext p_49111_) {
      return SHAPE;
   }
}
