package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class WaterloggedTransparentBlock extends TransparentBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<WaterloggedTransparentBlock> CODEC = simpleCodec(WaterloggedTransparentBlock::new);
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

   @Override
   protected MapCodec<? extends WaterloggedTransparentBlock> codec() {
      return CODEC;
   }

   protected WaterloggedTransparentBlock(BlockBehaviour.Properties p_312891_) {
      super(p_312891_);
      this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_311201_) {
      FluidState fluidstate = p_311201_.getLevel().getFluidState(p_311201_.getClickedPos());
      return super.getStateForPlacement(p_311201_).setValue(WATERLOGGED, fluidstate.is(Fluids.WATER));
   }

   @Override
   protected BlockState updateShape(
      BlockState p_312220_,
      LevelReader p_365359_,
      ScheduledTickAccess p_361079_,
      BlockPos p_310038_,
      Direction p_310752_,
      BlockPos p_309617_,
      BlockState p_310063_,
      RandomSource p_360753_
   ) {
      if (p_312220_.getValue(WATERLOGGED)) {
         p_361079_.scheduleTick(p_310038_, Fluids.WATER, Fluids.WATER.getTickDelay(p_365359_));
      }

      return super.updateShape(p_312220_, p_365359_, p_361079_, p_310038_, p_310752_, p_309617_, p_310063_, p_360753_);
   }

   @Override
   protected FluidState getFluidState(BlockState p_312084_) {
      return p_312084_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(true) : super.getFluidState(p_312084_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_311516_) {
      p_311516_.add(WATERLOGGED);
   }
}
