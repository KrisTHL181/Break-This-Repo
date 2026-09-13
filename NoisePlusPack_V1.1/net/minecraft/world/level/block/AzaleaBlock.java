package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AzaleaBlock extends VegetationBlock implements BonemealableBlock {
   public static final MapCodec<AzaleaBlock> CODEC = simpleCodec(AzaleaBlock::new);
   private static final VoxelShape SHAPE = Shapes.or(Block.column(16.0, 8.0, 16.0), Block.column(4.0, 0.0, 8.0));

   @Override
   public MapCodec<AzaleaBlock> codec() {
      return CODEC;
   }

   protected AzaleaBlock(BlockBehaviour.Properties p_152067_) {
      super(p_152067_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_152084_, BlockGetter p_152085_, BlockPos p_152086_, CollisionContext p_152087_) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_152089_, BlockGetter p_152090_, BlockPos p_152091_) {
      return p_152089_.is(Blocks.CLAY) || super.mayPlaceOn(p_152089_, p_152090_, p_152091_);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256329_, BlockPos p_256107_, BlockState p_255771_) {
      return p_256329_.getFluidState(p_256107_.above()).isEmpty();
   }

   @Override
   public boolean isBonemealSuccess(Level p_220712_, RandomSource p_220713_, BlockPos p_220714_, BlockState p_220715_) {
      return p_220712_.random.nextFloat() < 0.45;
   }

   @Override
   public void performBonemeal(ServerLevel p_220707_, RandomSource p_220708_, BlockPos p_220709_, BlockState p_220710_) {
      TreeGrower.AZALEA.growTree(p_220707_, p_220707_.getChunkSource().getGenerator(), p_220709_, p_220710_, p_220708_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_370171_, PathComputationType p_369747_) {
      return false;
   }
}
