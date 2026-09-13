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

public class TallGrassBlock extends VegetationBlock implements BonemealableBlock {
   public static final MapCodec<TallGrassBlock> CODEC = simpleCodec(TallGrassBlock::new);
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);

   @Override
   public MapCodec<TallGrassBlock> codec() {
      return CODEC;
   }

   protected TallGrassBlock(BlockBehaviour.Properties p_57318_) {
      super(p_57318_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_57336_, BlockGetter p_57337_, BlockPos p_57338_, CollisionContext p_57339_) {
      return SHAPE;
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_255692_, BlockPos p_57326_, BlockState p_57327_) {
      return getGrownBlock(p_57327_).defaultBlockState().canSurvive(p_255692_, p_57326_) && p_255692_.isEmptyBlock(p_57326_.above());
   }

   @Override
   public boolean isBonemealSuccess(Level p_222583_, RandomSource p_222584_, BlockPos p_222585_, BlockState p_222586_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_222578_, RandomSource p_222579_, BlockPos p_222580_, BlockState p_222581_) {
      DoublePlantBlock.placeAt(p_222578_, getGrownBlock(p_222581_).defaultBlockState(), p_222580_, 2);
   }

   private static DoublePlantBlock getGrownBlock(BlockState p_362227_) {
      return (DoublePlantBlock)(p_362227_.is(Blocks.FERN) ? Blocks.LARGE_FERN : Blocks.TALL_GRASS);
   }
}
