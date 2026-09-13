package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DirtPathBlock extends Block {
   public static final MapCodec<DirtPathBlock> CODEC = simpleCodec(DirtPathBlock::new);
   private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 15.0);

   @Override
   public MapCodec<DirtPathBlock> codec() {
      return CODEC;
   }

   protected DirtPathBlock(BlockBehaviour.Properties p_153129_) {
      super(p_153129_);
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState p_153159_) {
      return true;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_153131_) {
      return !this.defaultBlockState().canSurvive(p_153131_.getLevel(), p_153131_.getClickedPos())
         ? Block.pushEntitiesUp(this.defaultBlockState(), Blocks.DIRT.defaultBlockState(), p_153131_.getLevel(), p_153131_.getClickedPos())
         : super.getStateForPlacement(p_153131_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_153152_,
      LevelReader p_366770_,
      ScheduledTickAccess p_367951_,
      BlockPos p_153156_,
      Direction p_153153_,
      BlockPos p_153157_,
      BlockState p_153154_,
      RandomSource p_363364_
   ) {
      if (p_153153_ == Direction.UP && !p_153152_.canSurvive(p_366770_, p_153156_)) {
         p_367951_.scheduleTick(p_153156_, this, 1);
      }

      return super.updateShape(p_153152_, p_366770_, p_367951_, p_153156_, p_153153_, p_153157_, p_153154_, p_363364_);
   }

   @Override
   protected void tick(BlockState p_221070_, ServerLevel p_221071_, BlockPos p_221072_, RandomSource p_221073_) {
      FarmBlock.turnToDirt(null, p_221070_, p_221071_, p_221072_);
   }

   @Override
   protected boolean canSurvive(BlockState p_153148_, LevelReader p_153149_, BlockPos p_153150_) {
      BlockState blockstate = p_153149_.getBlockState(p_153150_.above());
      return !blockstate.isSolid() || blockstate.getBlock() instanceof FenceGateBlock;
   }

   @Override
   protected VoxelShape getShape(BlockState p_153143_, BlockGetter p_153144_, BlockPos p_153145_, CollisionContext p_153146_) {
      return SHAPE;
   }

   @Override
   protected boolean isPathfindable(BlockState p_153138_, PathComputationType p_153141_) {
      return false;
   }
}
