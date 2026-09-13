package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
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
   public static final MapCodec<RailBlock> CODEC = simpleCodec(RailBlock::new);
   public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;

   @Override
   public MapCodec<RailBlock> codec() {
      return CODEC;
   }

   protected RailBlock(BlockBehaviour.Properties p_55395_) {
      super(false, p_55395_);
      this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(WATERLOGGED, false));
   }

   @Override
   protected void updateState(BlockState p_55397_, Level p_55398_, BlockPos p_55399_, Block p_55400_) {
      if (p_55400_.defaultBlockState().isSignalSource() && new RailState(p_55398_, p_55399_, p_55397_).countPotentialConnections() == 3) {
         this.updateDir(p_55398_, p_55399_, p_55397_, false);
      }
   }

   @Override
   public Property<RailShape> getShapeProperty() {
      return SHAPE;
   }

   @Override
   protected BlockState rotate(BlockState p_55405_, Rotation p_55406_) {
      RailShape railshape = p_55405_.getValue(SHAPE);
      RailShape railshape1 = this.rotate(railshape, p_55406_);
      return p_55405_.setValue(SHAPE, railshape1);
   }

   @Override
   protected BlockState mirror(BlockState p_55402_, Mirror p_55403_) {
      RailShape railshape = p_55402_.getValue(SHAPE);
      RailShape railshape1 = this.mirror(railshape, p_55403_);
      return p_55402_.setValue(SHAPE, railshape1);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55408_) {
      p_55408_.add(SHAPE, WATERLOGGED);
   }
}
