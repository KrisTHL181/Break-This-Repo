package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ConduitBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<ConduitBlock> CODEC = simpleCodec(ConduitBlock::new);
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE = Block.cube(6.0);

   @Override
   public MapCodec<ConduitBlock> codec() {
      return CODEC;
   }

   public ConduitBlock(BlockBehaviour.Properties p_52094_) {
      super(p_52094_);
      this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, true));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_52118_) {
      p_52118_.add(WATERLOGGED);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153098_, BlockState p_153099_) {
      return new ConduitBlockEntity(p_153098_, p_153099_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_153094_, BlockState p_153095_, BlockEntityType<T> p_153096_) {
      return createTickerHelper(p_153096_, BlockEntityType.CONDUIT, p_153094_.isClientSide() ? ConduitBlockEntity::clientTick : ConduitBlockEntity::serverTick);
   }

   @Override
   protected FluidState getFluidState(BlockState p_52127_) {
      return p_52127_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_52127_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_52111_,
      LevelReader p_365158_,
      ScheduledTickAccess p_367818_,
      BlockPos p_52115_,
      Direction p_52112_,
      BlockPos p_52116_,
      BlockState p_52113_,
      RandomSource p_361587_
   ) {
      if (p_52111_.getValue(WATERLOGGED)) {
         p_367818_.scheduleTick(p_52115_, Fluids.WATER, Fluids.WATER.getTickDelay(p_365158_));
      }

      return super.updateShape(p_52111_, p_365158_, p_367818_, p_52115_, p_52112_, p_52116_, p_52113_, p_361587_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_52122_, BlockGetter p_52123_, BlockPos p_52124_, CollisionContext p_52125_) {
      return SHAPE;
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_52096_) {
      FluidState fluidstate = p_52096_.getLevel().getFluidState(p_52096_.getClickedPos());
      return this.defaultBlockState().setValue(WATERLOGGED, fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8);
   }

   @Override
   protected boolean isPathfindable(BlockState p_52106_, PathComputationType p_52109_) {
      return false;
   }
}
