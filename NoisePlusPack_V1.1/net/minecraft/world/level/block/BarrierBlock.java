package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class BarrierBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<BarrierBlock> CODEC = simpleCodec(BarrierBlock::new);
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

   @Override
   public MapCodec<BarrierBlock> codec() {
      return CODEC;
   }

   protected BarrierBlock(BlockBehaviour.Properties p_49092_) {
      super(p_49092_);
      this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_49100_) {
      return p_49100_.getFluidState().isEmpty();
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_49098_) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected float getShadeBrightness(BlockState p_49094_, BlockGetter p_49095_, BlockPos p_49096_) {
      return 1.0F;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_298183_,
      LevelReader p_368824_,
      ScheduledTickAccess p_364556_,
      BlockPos p_297885_,
      Direction p_298685_,
      BlockPos p_299701_,
      BlockState p_298648_,
      RandomSource p_365489_
   ) {
      if (p_298183_.getValue(WATERLOGGED)) {
         p_364556_.scheduleTick(p_297885_, Fluids.WATER, Fluids.WATER.getTickDelay(p_368824_));
      }

      return super.updateShape(p_298183_, p_368824_, p_364556_, p_297885_, p_298685_, p_299701_, p_298648_, p_365489_);
   }

   @Override
   protected FluidState getFluidState(BlockState p_301306_) {
      return p_301306_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_301306_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_298919_) {
      return this.defaultBlockState().setValue(WATERLOGGED, p_298919_.getLevel().getFluidState(p_298919_.getClickedPos()).getType() == Fluids.WATER);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_297868_) {
      p_297868_.add(WATERLOGGED);
   }

   @Override
   public ItemStack pickupBlock(@Nullable LivingEntity p_394107_, LevelAccessor p_299225_, BlockPos p_298270_, BlockState p_298275_) {
      return p_394107_ instanceof Player player && player.isCreative()
         ? SimpleWaterloggedBlock.super.pickupBlock(p_394107_, p_299225_, p_298270_, p_298275_)
         : ItemStack.EMPTY;
   }

   @Override
   public boolean canPlaceLiquid(@Nullable LivingEntity p_396046_, BlockGetter p_299765_, BlockPos p_297382_, BlockState p_299344_, Fluid p_299153_) {
      return p_396046_ instanceof Player player && player.isCreative()
         ? SimpleWaterloggedBlock.super.canPlaceLiquid(p_396046_, p_299765_, p_297382_, p_299344_, p_299153_)
         : false;
   }
}
