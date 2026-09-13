package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
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

public abstract class BaseCoralPlantTypeBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 4.0);

   protected BaseCoralPlantTypeBlock(BlockBehaviour.Properties p_49161_) {
      super(p_49161_);
      this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, true));
   }

   @Override
   protected abstract MapCodec<? extends BaseCoralPlantTypeBlock> codec();

   protected void tryScheduleDieTick(BlockState p_49165_, BlockGetter p_369413_, ScheduledTickAccess p_368290_, RandomSource p_364394_, BlockPos p_49167_) {
      if (!scanForWater(p_49165_, p_369413_, p_49167_)) {
         p_368290_.scheduleTick(p_49167_, this, 60 + p_364394_.nextInt(40));
      }
   }

   protected static boolean scanForWater(BlockState p_49187_, BlockGetter p_49188_, BlockPos p_49189_) {
      if (p_49187_.getValue(WATERLOGGED)) {
         return true;
      }

      for (Direction direction : Direction.values()) {
         if (p_49188_.getFluidState(p_49189_.relative(direction)).is(FluidTags.WATER)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_49163_) {
      FluidState fluidstate = p_49163_.getLevel().getFluidState(p_49163_.getClickedPos());
      return this.defaultBlockState().setValue(WATERLOGGED, fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8);
   }

   @Override
   protected VoxelShape getShape(BlockState p_49182_, BlockGetter p_49183_, BlockPos p_49184_, CollisionContext p_49185_) {
      return SHAPE;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_49173_,
      LevelReader p_363497_,
      ScheduledTickAccess p_366125_,
      BlockPos p_49177_,
      Direction p_49174_,
      BlockPos p_49178_,
      BlockState p_49175_,
      RandomSource p_365580_
   ) {
      if (p_49173_.getValue(WATERLOGGED)) {
         p_366125_.scheduleTick(p_49177_, Fluids.WATER, Fluids.WATER.getTickDelay(p_363497_));
      }

      return p_49174_ == Direction.DOWN && !this.canSurvive(p_49173_, p_363497_, p_49177_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_49173_, p_363497_, p_366125_, p_49177_, p_49174_, p_49178_, p_49175_, p_365580_);
   }

   @Override
   protected boolean canSurvive(BlockState p_49169_, LevelReader p_49170_, BlockPos p_49171_) {
      BlockPos blockpos = p_49171_.below();
      return p_49170_.getBlockState(blockpos).isFaceSturdy(p_49170_, blockpos, Direction.UP);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_49180_) {
      p_49180_.add(WATERLOGGED);
   }

   @Override
   protected FluidState getFluidState(BlockState p_49191_) {
      return p_49191_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_49191_);
   }
}
