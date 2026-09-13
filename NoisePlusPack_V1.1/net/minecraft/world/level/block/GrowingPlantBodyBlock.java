package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantBodyBlock extends GrowingPlantBlock implements BonemealableBlock {
   protected GrowingPlantBodyBlock(BlockBehaviour.Properties p_53886_, Direction p_53887_, VoxelShape p_53888_, boolean p_53889_) {
      super(p_53886_, p_53887_, p_53888_, p_53889_);
   }

   @Override
   protected abstract MapCodec<? extends GrowingPlantBodyBlock> codec();

   protected BlockState updateHeadAfterConvertedFromBody(BlockState p_153326_, BlockState p_153327_) {
      return p_153327_;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_53913_,
      LevelReader p_364320_,
      ScheduledTickAccess p_363381_,
      BlockPos p_53917_,
      Direction p_53914_,
      BlockPos p_53918_,
      BlockState p_53915_,
      RandomSource p_365042_
   ) {
      if (p_53914_ == this.growthDirection.getOpposite() && !p_53913_.canSurvive(p_364320_, p_53917_)) {
         p_363381_.scheduleTick(p_53917_, this, 1);
      }

      GrowingPlantHeadBlock growingplantheadblock = this.getHeadBlock();
      if (p_53914_ == this.growthDirection && !p_53915_.is(this) && !p_53915_.is(growingplantheadblock)) {
         return this.updateHeadAfterConvertedFromBody(p_53913_, growingplantheadblock.getStateForPlacement(p_365042_));
      }

      if (this.scheduleFluidTicks) {
         p_363381_.scheduleTick(p_53917_, Fluids.WATER, Fluids.WATER.getTickDelay(p_364320_));
      }

      return super.updateShape(p_53913_, p_364320_, p_363381_, p_53917_, p_53914_, p_53918_, p_53915_, p_365042_);
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_312726_, BlockPos p_53897_, BlockState p_53898_, boolean p_377882_) {
      return new ItemStack(this.getHeadBlock());
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256221_, BlockPos p_255647_, BlockState p_256117_) {
      Optional<BlockPos> optional = this.getHeadPos(p_256221_, p_255647_, p_256117_.getBlock());
      return optional.isPresent() && this.getHeadBlock().canGrowInto(p_256221_.getBlockState(optional.get().relative(this.growthDirection)));
   }

   @Override
   public boolean isBonemealSuccess(Level p_221290_, RandomSource p_221291_, BlockPos p_221292_, BlockState p_221293_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_221285_, RandomSource p_221286_, BlockPos p_221287_, BlockState p_221288_) {
      Optional<BlockPos> optional = this.getHeadPos(p_221285_, p_221287_, p_221288_.getBlock());
      if (optional.isPresent()) {
         BlockState blockstate = p_221285_.getBlockState(optional.get());
         ((GrowingPlantHeadBlock)blockstate.getBlock()).performBonemeal(p_221285_, p_221286_, optional.get(), blockstate);
      }
   }

   private Optional<BlockPos> getHeadPos(BlockGetter p_153323_, BlockPos p_153324_, Block p_153325_) {
      return BlockUtil.getTopConnectedBlock(p_153323_, p_153324_, p_153325_, this.growthDirection, this.getHeadBlock());
   }

   @Override
   protected boolean canBeReplaced(BlockState p_53910_, BlockPlaceContext p_53911_) {
      boolean flag = super.canBeReplaced(p_53910_, p_53911_);
      return flag && p_53911_.getItemInHand().is(this.getHeadBlock().asItem()) ? false : flag;
   }

   @Override
   protected Block getBodyBlock() {
      return this;
   }
}
