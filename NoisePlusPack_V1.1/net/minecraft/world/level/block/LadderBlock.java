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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class LadderBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<LadderBlock> CODEC = simpleCodec(LadderBlock::new);
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(16.0, 13.0, 16.0));

   @Override
   public MapCodec<LadderBlock> codec() {
      return CODEC;
   }

   protected LadderBlock(BlockBehaviour.Properties p_54345_) {
      super(p_54345_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
   }

   @Override
   protected VoxelShape getShape(BlockState p_54372_, BlockGetter p_54373_, BlockPos p_54374_, CollisionContext p_54375_) {
      return SHAPES.get(p_54372_.getValue(FACING));
   }

   private boolean canAttachTo(BlockGetter p_54349_, BlockPos p_54350_, Direction p_54351_) {
      BlockState blockstate = p_54349_.getBlockState(p_54350_);
      return blockstate.isFaceSturdy(p_54349_, p_54350_, p_54351_);
   }

   @Override
   protected boolean canSurvive(BlockState p_54353_, LevelReader p_54354_, BlockPos p_54355_) {
      Direction direction = p_54353_.getValue(FACING);
      return this.canAttachTo(p_54354_, p_54355_.relative(direction.getOpposite()), direction);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_54363_,
      LevelReader p_363162_,
      ScheduledTickAccess p_369244_,
      BlockPos p_54367_,
      Direction p_54364_,
      BlockPos p_54368_,
      BlockState p_54365_,
      RandomSource p_368750_
   ) {
      if (p_54364_.getOpposite() == p_54363_.getValue(FACING) && !p_54363_.canSurvive(p_363162_, p_54367_)) {
         return Blocks.AIR.defaultBlockState();
      }

      if (p_54363_.getValue(WATERLOGGED)) {
         p_369244_.scheduleTick(p_54367_, Fluids.WATER, Fluids.WATER.getTickDelay(p_363162_));
      }

      return super.updateShape(p_54363_, p_363162_, p_369244_, p_54367_, p_54364_, p_54368_, p_54365_, p_368750_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_54347_) {
      if (!p_54347_.replacingClickedOnBlock()) {
         BlockState blockstate = p_54347_.getLevel().getBlockState(p_54347_.getClickedPos().relative(p_54347_.getClickedFace().getOpposite()));
         if (blockstate.is(this) && blockstate.getValue(FACING) == p_54347_.getClickedFace()) {
            return null;
         }
      }

      BlockState blockstate1 = this.defaultBlockState();
      LevelReader levelreader = p_54347_.getLevel();
      BlockPos blockpos = p_54347_.getClickedPos();
      FluidState fluidstate = p_54347_.getLevel().getFluidState(p_54347_.getClickedPos());

      for (Direction direction : p_54347_.getNearestLookingDirections()) {
         if (direction.getAxis().isHorizontal()) {
            blockstate1 = blockstate1.setValue(FACING, direction.getOpposite());
            if (blockstate1.canSurvive(levelreader, blockpos)) {
               return blockstate1.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
            }
         }
      }

      return null;
   }

   @Override
   protected BlockState rotate(BlockState p_54360_, Rotation p_54361_) {
      return p_54360_.setValue(FACING, p_54361_.rotate(p_54360_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_54357_, Mirror p_54358_) {
      return p_54357_.rotate(p_54358_.getRotation(p_54357_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54370_) {
      p_54370_.add(FACING, WATERLOGGED);
   }

   @Override
   protected FluidState getFluidState(BlockState p_54377_) {
      return p_54377_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_54377_);
   }
}
