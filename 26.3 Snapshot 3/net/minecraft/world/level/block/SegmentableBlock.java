package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface SegmentableBlock {
   int MIN_SEGMENT = 1;
   int MAX_SEGMENT = 4;
   IntegerProperty AMOUNT = BlockStateProperties.SEGMENT_AMOUNT;

   default Function<BlockState, VoxelShape> getShapeCalculator(final EnumProperty<Direction> facing, final IntegerProperty amount) {
      Map<Direction, VoxelShape> shapes = Shapes.rotateHorizontal(Block.box(0.0, 0.0, 0.0, 8.0, this.getShapeHeight(), 8.0));
      return state -> {
         VoxelShape shape = Shapes.empty();
         Direction direction = state.getValue(facing);
         int count = state.getValue(amount);

         for (int i = 0; i < count; i++) {
            shape = Shapes.or(shape, shapes.get(direction));
            direction = direction.getCounterClockWise();
         }

         return shape.singleEncompassing();
      };
   }

   default IntegerProperty getSegmentAmountProperty() {
      return AMOUNT;
   }

   default double getShapeHeight() {
      return 1.0;
   }

   default boolean canBeReplaced(final BlockState state, final BlockPlaceContext context, final IntegerProperty segment) {
      return !context.isSecondaryUseActive() && context.getItemInHand().is(state.getBlock().asItem()) && state.getValue(segment) < 4;
   }

   default BlockState getStateForPlacement(
      final BlockPlaceContext context, final Block block, final IntegerProperty segment, final EnumProperty<Direction> facing
   ) {
      BlockState state = context.getLevel().getBlockState(context.getClickedPos());
      return state.is(block)
         ? state.setValue(segment, Math.min(4, state.getValue(segment) + 1))
         : block.defaultBlockState().setValue(facing, context.getHorizontalDirection().getOpposite());
   }
}
