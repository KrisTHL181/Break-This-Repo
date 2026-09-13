package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class RailBlock extends BaseRailBlock {
   public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;

   protected RailBlock(final BlockBehaviour.Properties properties) {
      super(false, properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(WATERLOGGED, false));
   }

   @Override
   protected void updateState(final BlockState state, final Level level, final BlockPos pos, final Block block) {
      if (block.defaultBlockState().isSignalSource() && new RailState(level, pos, state).countPotentialConnections() == 3) {
         this.updateDir(level, pos, state, false);
      }
   }

   @Override
   public Property<RailShape> getShapeProperty() {
      return SHAPE;
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      RailShape currentShape = state.getValue(SHAPE);
      RailShape newShape = this.rotate(currentShape, rotation);
      return state.setValue(SHAPE, newShape);
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      RailShape currentShape = state.getValue(SHAPE);
      RailShape newShape = this.mirror(currentShape, mirror);
      return state.setValue(SHAPE, newShape);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(SHAPE, WATERLOGGED);
   }
}
