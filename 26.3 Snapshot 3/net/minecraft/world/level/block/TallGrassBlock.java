package net.minecraft.world.level.block;

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
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);

   protected TallGrassBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      return getGrownBlock(state).defaultBlockState().canSurvive(level, pos) && level.isEmptyBlock(pos.above()) && level.isInsideBuildHeight(pos.above());
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      DoublePlantBlock.placeAt(level, getGrownBlock(state).defaultBlockState(), pos, 2);
   }

   private static DoublePlantBlock getGrownBlock(final BlockState state) {
      return (DoublePlantBlock)(state.is(Blocks.FERN) ? Blocks.LARGE_FERN : Blocks.TALL_GRASS);
   }
}
