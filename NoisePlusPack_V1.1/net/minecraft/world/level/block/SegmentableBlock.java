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

   default Function<BlockState, VoxelShape> getShapeCalculator(EnumProperty<Direction> p_395991_, IntegerProperty p_393332_) {
      Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.box(0.0, 0.0, 0.0, 8.0, this.getShapeHeight(), 8.0));
      return p_392138_ -> {
         VoxelShape voxelshape = Shapes.empty();
         Direction direction = p_392138_.getValue(p_395991_);
         int i = p_392138_.getValue(p_393332_);

         for (int j = 0; j < i; j++) {
            voxelshape = Shapes.or(voxelshape, map.get(direction));
            direction = direction.getCounterClockWise();
         }

         return voxelshape.singleEncompassing();
      };
   }

   default IntegerProperty getSegmentAmountProperty() {
      return AMOUNT;
   }

   default double getShapeHeight() {
      return 1.0;
   }

   default boolean canBeReplaced(BlockState p_392242_, BlockPlaceContext p_393421_, IntegerProperty p_396239_) {
      return !p_393421_.isSecondaryUseActive() && p_393421_.getItemInHand().is(p_392242_.getBlock().asItem()) && p_392242_.getValue(p_396239_) < 4;
   }

   default BlockState getStateForPlacement(BlockPlaceContext p_392984_, Block p_395181_, IntegerProperty p_393679_, EnumProperty<Direction> p_397059_) {
      BlockState blockstate = p_392984_.getLevel().getBlockState(p_392984_.getClickedPos());
      return blockstate.is(p_395181_)
         ? blockstate.setValue(p_393679_, Math.min(4, blockstate.getValue(p_393679_) + 1))
         : p_395181_.defaultBlockState().setValue(p_397059_, p_392984_.getHorizontalDirection().getOpposite());
   }
}
