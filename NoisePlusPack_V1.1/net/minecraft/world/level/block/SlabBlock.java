package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SlabBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<SlabBlock> CODEC = simpleCodec(SlabBlock::new);
   public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE_BOTTOM = Block.column(16.0, 0.0, 8.0);
   private static final VoxelShape SHAPE_TOP = Block.column(16.0, 8.0, 16.0);

   @Override
   public MapCodec<? extends SlabBlock> codec() {
      return CODEC;
   }

   public SlabBlock(BlockBehaviour.Properties p_56359_) {
      super(p_56359_);
      this.registerDefaultState(this.defaultBlockState().setValue(TYPE, SlabType.BOTTOM).setValue(WATERLOGGED, false));
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState p_56395_) {
      return p_56395_.getValue(TYPE) != SlabType.DOUBLE;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56388_) {
      p_56388_.add(TYPE, WATERLOGGED);
   }

   @Override
   protected VoxelShape getShape(BlockState p_56390_, BlockGetter p_56391_, BlockPos p_56392_, CollisionContext p_56393_) {
      return switch ((SlabType)p_56390_.getValue(TYPE)) {
         case TOP -> SHAPE_TOP;
         case BOTTOM -> SHAPE_BOTTOM;
         case DOUBLE -> Shapes.block();
      };
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_56361_) {
      BlockPos blockpos = p_56361_.getClickedPos();
      BlockState blockstate = p_56361_.getLevel().getBlockState(blockpos);
      if (blockstate.is(this)) {
         return blockstate.setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false);
      }

      FluidState fluidstate = p_56361_.getLevel().getFluidState(blockpos);
      BlockState blockstate1 = this.defaultBlockState().setValue(TYPE, SlabType.BOTTOM).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
      Direction direction = p_56361_.getClickedFace();
      return direction == Direction.DOWN || direction != Direction.UP && p_56361_.getClickLocation().y - blockpos.getY() > 0.5
         ? blockstate1.setValue(TYPE, SlabType.TOP)
         : blockstate1;
   }

   @Override
   protected boolean canBeReplaced(BlockState p_56373_, BlockPlaceContext p_56374_) {
      ItemStack itemstack = p_56374_.getItemInHand();
      SlabType slabtype = p_56373_.getValue(TYPE);
      if (slabtype == SlabType.DOUBLE || !itemstack.is(this.asItem())) {
         return false;
      } else if (p_56374_.replacingClickedOnBlock()) {
         boolean flag = p_56374_.getClickLocation().y - p_56374_.getClickedPos().getY() > 0.5;
         Direction direction = p_56374_.getClickedFace();
         return slabtype == SlabType.BOTTOM
            ? direction == Direction.UP || flag && direction.getAxis().isHorizontal()
            : direction == Direction.DOWN || !flag && direction.getAxis().isHorizontal();
      } else {
         return true;
      }
   }

   @Override
   protected FluidState getFluidState(BlockState p_56397_) {
      return p_56397_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_56397_);
   }

   @Override
   public boolean placeLiquid(LevelAccessor p_56368_, BlockPos p_56369_, BlockState p_56370_, FluidState p_56371_) {
      return p_56370_.getValue(TYPE) != SlabType.DOUBLE ? SimpleWaterloggedBlock.super.placeLiquid(p_56368_, p_56369_, p_56370_, p_56371_) : false;
   }

   @Override
   public boolean canPlaceLiquid(@Nullable LivingEntity p_396170_, BlockGetter p_56363_, BlockPos p_56364_, BlockState p_56365_, Fluid p_56366_) {
      return p_56365_.getValue(TYPE) != SlabType.DOUBLE
         ? SimpleWaterloggedBlock.super.canPlaceLiquid(p_396170_, p_56363_, p_56364_, p_56365_, p_56366_)
         : false;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_56381_,
      LevelReader p_369337_,
      ScheduledTickAccess p_365524_,
      BlockPos p_56385_,
      Direction p_56382_,
      BlockPos p_56386_,
      BlockState p_56383_,
      RandomSource p_369077_
   ) {
      if (p_56381_.getValue(WATERLOGGED)) {
         p_365524_.scheduleTick(p_56385_, Fluids.WATER, Fluids.WATER.getTickDelay(p_369337_));
      }

      return super.updateShape(p_56381_, p_369337_, p_365524_, p_56385_, p_56382_, p_56386_, p_56383_, p_369077_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_56376_, PathComputationType p_56379_) {
      switch (p_56379_) {
         case LAND:
            return false;
         case WATER:
            return p_56376_.getFluidState().is(FluidTags.WATER);
         case AIR:
            return false;
         default:
            return false;
      }
   }
}
