package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LeafLitterBlock extends VegetationBlock implements SegmentableBlock {
   public static final MapCodec<LeafLitterBlock> CODEC = simpleCodec(LeafLitterBlock::new);
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private final Function<BlockState, VoxelShape> shapes;

   public LeafLitterBlock(BlockBehaviour.Properties p_395316_) {
      super(p_395316_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(this.getSegmentAmountProperty(), 1));
      this.shapes = this.makeShapes();
   }

   private Function<BlockState, VoxelShape> makeShapes() {
      return this.getShapeForEachState(this.getShapeCalculator(FACING, this.getSegmentAmountProperty()));
   }

   @Override
   protected MapCodec<LeafLitterBlock> codec() {
      return CODEC;
   }

   @Override
   public BlockState rotate(BlockState p_395016_, Rotation p_395981_) {
      return p_395016_.setValue(FACING, p_395981_.rotate(p_395016_.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState p_395508_, Mirror p_397194_) {
      return p_395508_.rotate(p_397194_.getRotation(p_395508_.getValue(FACING)));
   }

   @Override
   public boolean canBeReplaced(BlockState p_397082_, BlockPlaceContext p_396781_) {
      return this.canBeReplaced(p_397082_, p_396781_, this.getSegmentAmountProperty()) ? true : super.canBeReplaced(p_397082_, p_396781_);
   }

   @Override
   protected boolean canSurvive(BlockState p_395618_, LevelReader p_391636_, BlockPos p_391950_) {
      BlockPos blockpos = p_391950_.below();
      return p_391636_.getBlockState(blockpos).isFaceSturdy(p_391636_, blockpos, Direction.UP);
   }

   @Override
   public VoxelShape getShape(BlockState p_395469_, BlockGetter p_393305_, BlockPos p_393404_, CollisionContext p_392068_) {
      return this.shapes.apply(p_395469_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_397461_) {
      return this.getStateForPlacement(p_397461_, this, this.getSegmentAmountProperty(), FACING);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_397592_) {
      p_397592_.add(FACING, this.getSegmentAmountProperty());
   }
}
