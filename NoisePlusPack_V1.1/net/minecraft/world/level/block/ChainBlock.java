package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChainBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<ChainBlock> CODEC = simpleCodec(ChainBlock::new);
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateAllAxis(Block.cube(3.0, 3.0, 16.0));

   @Override
   public MapCodec<? extends ChainBlock> codec() {
      return CODEC;
   }

   public ChainBlock(BlockBehaviour.Properties p_51452_) {
      super(p_51452_);
      this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(AXIS, Direction.Axis.Y));
   }

   @Override
   protected VoxelShape getShape(BlockState p_51470_, BlockGetter p_51471_, BlockPos p_51472_, CollisionContext p_51473_) {
      return SHAPES.get(p_51470_.getValue(AXIS));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_51454_) {
      FluidState fluidstate = p_51454_.getLevel().getFluidState(p_51454_.getClickedPos());
      boolean flag = fluidstate.getType() == Fluids.WATER;
      return super.getStateForPlacement(p_51454_).setValue(WATERLOGGED, flag);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_51461_,
      LevelReader p_361536_,
      ScheduledTickAccess p_364977_,
      BlockPos p_51465_,
      Direction p_51462_,
      BlockPos p_51466_,
      BlockState p_51463_,
      RandomSource p_367386_
   ) {
      if (p_51461_.getValue(WATERLOGGED)) {
         p_364977_.scheduleTick(p_51465_, Fluids.WATER, Fluids.WATER.getTickDelay(p_361536_));
      }

      return super.updateShape(p_51461_, p_361536_, p_364977_, p_51465_, p_51462_, p_51466_, p_51463_, p_367386_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51468_) {
      p_51468_.add(WATERLOGGED).add(AXIS);
   }

   @Override
   protected FluidState getFluidState(BlockState p_51475_) {
      return p_51475_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_51475_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_51456_, PathComputationType p_51459_) {
      return false;
   }
}
