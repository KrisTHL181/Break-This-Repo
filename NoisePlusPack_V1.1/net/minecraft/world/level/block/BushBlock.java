package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BushBlock extends VegetationBlock implements BonemealableBlock {
   public static final MapCodec<BushBlock> CODEC = simpleCodec(BushBlock::new);
   private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 13.0);

   @Override
   public MapCodec<BushBlock> codec() {
      return CODEC;
   }

   protected BushBlock(BlockBehaviour.Properties p_51021_) {
      super(p_51021_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_395143_, BlockGetter p_397492_, BlockPos p_393430_, CollisionContext p_397643_) {
      return SHAPE;
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_392634_, BlockPos p_396036_, BlockState p_395891_) {
      return BonemealableBlock.hasSpreadableNeighbourPos(p_392634_, p_396036_, p_395891_);
   }

   @Override
   public boolean isBonemealSuccess(Level p_395941_, RandomSource p_397459_, BlockPos p_396412_, BlockState p_394635_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_391278_, RandomSource p_394044_, BlockPos p_393939_, BlockState p_395840_) {
      BonemealableBlock.findSpreadableNeighbourPos(p_391278_, p_393939_, p_395840_)
         .ifPresent(p_405678_ -> p_391278_.setBlockAndUpdate(p_405678_, this.defaultBlockState()));
   }
}
