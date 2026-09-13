package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class DoublePlantBlock extends VegetationBlock {
   public static final MapCodec<DoublePlantBlock> CODEC = simpleCodec(DoublePlantBlock::new);
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

   @Override
   public MapCodec<? extends DoublePlantBlock> codec() {
      return CODEC;
   }

   public DoublePlantBlock(BlockBehaviour.Properties p_52861_) {
      super(p_52861_);
      this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
   }

   @Override
   protected BlockState updateShape(
      BlockState p_52894_,
      LevelReader p_365947_,
      ScheduledTickAccess p_362394_,
      BlockPos p_52898_,
      Direction p_52895_,
      BlockPos p_52899_,
      BlockState p_52896_,
      RandomSource p_360763_
   ) {
      DoubleBlockHalf doubleblockhalf = p_52894_.getValue(HALF);
      if (p_52895_.getAxis() != Direction.Axis.Y
         || doubleblockhalf == DoubleBlockHalf.LOWER != (p_52895_ == Direction.UP)
         || p_52896_.is(this) && p_52896_.getValue(HALF) != doubleblockhalf) {
         return doubleblockhalf == DoubleBlockHalf.LOWER && p_52895_ == Direction.DOWN && !p_52894_.canSurvive(p_365947_, p_52898_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_52894_, p_365947_, p_362394_, p_52898_, p_52895_, p_52899_, p_52896_, p_360763_);
      } else {
         return Blocks.AIR.defaultBlockState();
      }
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_52863_) {
      BlockPos blockpos = p_52863_.getClickedPos();
      Level level = p_52863_.getLevel();
      return blockpos.getY() < level.getMaxY() && level.getBlockState(blockpos.above()).canBeReplaced(p_52863_) ? super.getStateForPlacement(p_52863_) : null;
   }

   @Override
   public void setPlacedBy(Level p_52872_, BlockPos p_52873_, BlockState p_52874_, @Nullable LivingEntity p_52875_, ItemStack p_52876_) {
      BlockPos blockpos = p_52873_.above();
      p_52872_.setBlock(blockpos, copyWaterloggedFrom(p_52872_, blockpos, this.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER)), 3);
   }

   @Override
   protected boolean canSurvive(BlockState p_52887_, LevelReader p_52888_, BlockPos p_52889_) {
      if (p_52887_.getValue(HALF) != DoubleBlockHalf.UPPER) {
         return super.canSurvive(p_52887_, p_52888_, p_52889_);
      }

      BlockState blockstate = p_52888_.getBlockState(p_52889_.below());
      return blockstate.is(this) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER;
   }

   public static void placeAt(LevelAccessor p_153174_, BlockState p_153175_, BlockPos p_153176_, @Block.UpdateFlags int p_153177_) {
      BlockPos blockpos = p_153176_.above();
      p_153174_.setBlock(p_153176_, copyWaterloggedFrom(p_153174_, p_153176_, p_153175_.setValue(HALF, DoubleBlockHalf.LOWER)), p_153177_);
      p_153174_.setBlock(blockpos, copyWaterloggedFrom(p_153174_, blockpos, p_153175_.setValue(HALF, DoubleBlockHalf.UPPER)), p_153177_);
   }

   public static BlockState copyWaterloggedFrom(LevelReader p_182454_, BlockPos p_182455_, BlockState p_182456_) {
      return p_182456_.hasProperty(BlockStateProperties.WATERLOGGED)
         ? p_182456_.setValue(BlockStateProperties.WATERLOGGED, p_182454_.isWaterAt(p_182455_))
         : p_182456_;
   }

   @Override
   public BlockState playerWillDestroy(Level p_52878_, BlockPos p_52879_, BlockState p_52880_, Player p_52881_) {
      if (!p_52878_.isClientSide()) {
         if (p_52881_.preventsBlockDrops()) {
            preventDropFromBottomPart(p_52878_, p_52879_, p_52880_, p_52881_);
         } else {
            dropResources(p_52880_, p_52878_, p_52879_, null, p_52881_, p_52881_.getMainHandItem());
         }
      }

      return super.playerWillDestroy(p_52878_, p_52879_, p_52880_, p_52881_);
   }

   @Override
   public void playerDestroy(Level p_52865_, Player p_52866_, BlockPos p_52867_, BlockState p_52868_, @Nullable BlockEntity p_52869_, ItemStack p_52870_) {
      super.playerDestroy(p_52865_, p_52866_, p_52867_, Blocks.AIR.defaultBlockState(), p_52869_, p_52870_);
   }

   protected static void preventDropFromBottomPart(Level p_52904_, BlockPos p_52905_, BlockState p_52906_, Player p_52907_) {
      DoubleBlockHalf doubleblockhalf = p_52906_.getValue(HALF);
      if (doubleblockhalf == DoubleBlockHalf.UPPER) {
         BlockPos blockpos = p_52905_.below();
         BlockState blockstate = p_52904_.getBlockState(blockpos);
         if (blockstate.is(p_52906_.getBlock()) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockState blockstate1 = blockstate.getFluidState().is(Fluids.WATER) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            p_52904_.setBlock(blockpos, blockstate1, 35);
            p_52904_.levelEvent(p_52907_, 2001, blockpos, Block.getId(blockstate));
         }
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_52901_) {
      p_52901_.add(HALF);
   }

   @Override
   protected long getSeed(BlockState p_52891_, BlockPos p_52892_) {
      return Mth.getSeed(p_52892_.getX(), p_52892_.below(p_52891_.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), p_52892_.getZ());
   }
}
