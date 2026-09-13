package net.minecraft.world.level.block;

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
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private final Function<BlockState, VoxelShape> shapes;

   public LeafLitterBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(this.getSegmentAmountProperty(), 1));
      this.shapes = this.makeShapes();
   }

   private Function<BlockState, VoxelShape> makeShapes() {
      return this.getShapeForEachState(this.getShapeCalculator(FACING, this.getSegmentAmountProperty()));
   }

   @Override
   public BlockState rotate(final BlockState state, final Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   public BlockState mirror(final BlockState state, final Mirror mirror) {
      return state.rotate(mirror.getRotation(state.getValue(FACING)));
   }

   @Override
   public boolean canBeReplaced(final BlockState state, final BlockPlaceContext context) {
      return this.canBeReplaced(state, context, this.getSegmentAmountProperty()) ? true : super.canBeReplaced(state, context);
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      BlockPos belowPos = pos.below();
      return level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP);
   }

   @Override
   public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return this.shapes.apply(state);
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      return this.getStateForPlacement(context, this, this.getSegmentAmountProperty(), FACING);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, this.getSegmentAmountProperty());
   }
}
