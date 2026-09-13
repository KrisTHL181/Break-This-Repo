package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SmallDripleafBlock extends DoublePlantBlock implements BonemealableBlock, SimpleWaterloggedBlock {
   public static final MapCodec<SmallDripleafBlock> CODEC = simpleCodec(SmallDripleafBlock::new);
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);

   @Override
   public MapCodec<SmallDripleafBlock> codec() {
      return CODEC;
   }

   public SmallDripleafBlock(BlockBehaviour.Properties p_154583_) {
      super(p_154583_);
      this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH));
   }

   @Override
   protected VoxelShape getShape(BlockState p_154610_, BlockGetter p_154611_, BlockPos p_154612_, CollisionContext p_154613_) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_154636_, BlockGetter p_154637_, BlockPos p_154638_) {
      return p_154636_.is(BlockTags.SMALL_DRIPLEAF_PLACEABLE)
         || p_154637_.getFluidState(p_154638_.above()).isSourceOfType(Fluids.WATER) && super.mayPlaceOn(p_154636_, p_154637_, p_154638_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_154592_) {
      BlockState blockstate = super.getStateForPlacement(p_154592_);
      return blockstate != null
         ? copyWaterloggedFrom(p_154592_.getLevel(), p_154592_.getClickedPos(), blockstate.setValue(FACING, p_154592_.getHorizontalDirection().getOpposite()))
         : null;
   }

   @Override
   public void setPlacedBy(Level p_154599_, BlockPos p_154600_, BlockState p_154601_, @Nullable LivingEntity p_154602_, ItemStack p_154603_) {
      if (!p_154599_.isClientSide()) {
         BlockPos blockpos = p_154600_.above();
         BlockState blockstate = DoublePlantBlock.copyWaterloggedFrom(
            p_154599_, blockpos, this.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(FACING, p_154601_.getValue(FACING))
         );
         p_154599_.setBlock(blockpos, blockstate, 3);
      }
   }

   @Override
   protected FluidState getFluidState(BlockState p_154634_) {
      return p_154634_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_154634_);
   }

   @Override
   protected boolean canSurvive(BlockState p_154615_, LevelReader p_154616_, BlockPos p_154617_) {
      if (p_154615_.getValue(HALF) == DoubleBlockHalf.UPPER) {
         return super.canSurvive(p_154615_, p_154616_, p_154617_);
      }

      BlockPos blockpos = p_154617_.below();
      BlockState blockstate = p_154616_.getBlockState(blockpos);
      return this.mayPlaceOn(blockstate, p_154616_, blockpos);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_154625_,
      LevelReader p_362955_,
      ScheduledTickAccess p_364504_,
      BlockPos p_154629_,
      Direction p_154626_,
      BlockPos p_154630_,
      BlockState p_154627_,
      RandomSource p_361772_
   ) {
      if (p_154625_.getValue(WATERLOGGED)) {
         p_364504_.scheduleTick(p_154629_, Fluids.WATER, Fluids.WATER.getTickDelay(p_362955_));
      }

      return super.updateShape(p_154625_, p_362955_, p_364504_, p_154629_, p_154626_, p_154630_, p_154627_, p_361772_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_154632_) {
      p_154632_.add(HALF, WATERLOGGED, FACING);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_255772_, BlockPos p_154595_, BlockState p_154596_) {
      return true;
   }

   @Override
   public boolean isBonemealSuccess(Level p_222438_, RandomSource p_222439_, BlockPos p_222440_, BlockState p_222441_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_222433_, RandomSource p_222434_, BlockPos p_222435_, BlockState p_222436_) {
      if (p_222436_.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
         BlockPos blockpos = p_222435_.above();
         p_222433_.setBlock(blockpos, p_222433_.getFluidState(blockpos).createLegacyBlock(), 18);
         BigDripleafBlock.placeWithRandomHeight(p_222433_, p_222434_, p_222435_, p_222436_.getValue(FACING));
      } else {
         BlockPos blockpos1 = p_222435_.below();
         this.performBonemeal(p_222433_, p_222434_, blockpos1, p_222433_.getBlockState(blockpos1));
      }
   }

   @Override
   protected BlockState rotate(BlockState p_154622_, Rotation p_154623_) {
      return p_154622_.setValue(FACING, p_154623_.rotate(p_154622_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_154619_, Mirror p_154620_) {
      return p_154619_.rotate(p_154620_.getRotation(p_154619_.getValue(FACING)));
   }

   @Override
   protected float getMaxVerticalOffset() {
      return 0.1F;
   }
}
