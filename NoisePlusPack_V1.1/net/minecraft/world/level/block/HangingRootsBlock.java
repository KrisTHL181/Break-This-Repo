package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class HangingRootsBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<HangingRootsBlock> CODEC = simpleCodec(HangingRootsBlock::new);
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE = Block.column(12.0, 10.0, 16.0);

   @Override
   public MapCodec<HangingRootsBlock> codec() {
      return CODEC;
   }

   protected HangingRootsBlock(BlockBehaviour.Properties p_153337_) {
      super(p_153337_);
      this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_153358_) {
      p_153358_.add(WATERLOGGED);
   }

   @Override
   protected FluidState getFluidState(BlockState p_153360_) {
      return p_153360_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_153360_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_153340_) {
      BlockState blockstate = super.getStateForPlacement(p_153340_);
      if (blockstate != null) {
         FluidState fluidstate = p_153340_.getLevel().getFluidState(p_153340_.getClickedPos());
         return blockstate.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
      } else {
         return null;
      }
   }

   @Override
   protected boolean canSurvive(BlockState p_153347_, LevelReader p_153348_, BlockPos p_153349_) {
      BlockPos blockpos = p_153349_.above();
      BlockState blockstate = p_153348_.getBlockState(blockpos);
      return blockstate.isFaceSturdy(p_153348_, blockpos, Direction.DOWN);
   }

   @Override
   protected VoxelShape getShape(BlockState p_153342_, BlockGetter p_153343_, BlockPos p_153344_, CollisionContext p_153345_) {
      return SHAPE;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_153351_,
      LevelReader p_360752_,
      ScheduledTickAccess p_361430_,
      BlockPos p_153355_,
      Direction p_153352_,
      BlockPos p_153356_,
      BlockState p_153353_,
      RandomSource p_363570_
   ) {
      if (p_153352_ == Direction.UP && !this.canSurvive(p_153351_, p_360752_, p_153355_)) {
         return Blocks.AIR.defaultBlockState();
      }

      if (p_153351_.getValue(WATERLOGGED)) {
         p_361430_.scheduleTick(p_153355_, Fluids.WATER, Fluids.WATER.getTickDelay(p_360752_));
      }

      return super.updateShape(p_153351_, p_360752_, p_361430_, p_153355_, p_153352_, p_153356_, p_153353_, p_363570_);
   }
}
