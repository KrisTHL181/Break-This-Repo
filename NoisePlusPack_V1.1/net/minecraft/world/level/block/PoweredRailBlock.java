package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class PoweredRailBlock extends BaseRailBlock {
   public static final MapCodec<PoweredRailBlock> CODEC = simpleCodec(PoweredRailBlock::new);
   public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   @Override
   public MapCodec<PoweredRailBlock> codec() {
      return CODEC;
   }

   protected PoweredRailBlock(BlockBehaviour.Properties p_55218_) {
      super(true, p_55218_);
      this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(POWERED, false).setValue(WATERLOGGED, false));
   }

   protected boolean findPoweredRailSignal(Level p_55220_, BlockPos p_55221_, BlockState p_55222_, boolean p_55223_, int p_55224_) {
      if (p_55224_ >= 8) {
         return false;
      }

      int i = p_55221_.getX();
      int j = p_55221_.getY();
      int k = p_55221_.getZ();
      boolean flag = true;
      RailShape railshape = p_55222_.getValue(SHAPE);
      switch (railshape) {
         case NORTH_SOUTH:
            if (p_55223_) {
               k++;
            } else {
               k--;
            }
            break;
         case EAST_WEST:
            if (p_55223_) {
               i--;
            } else {
               i++;
            }
            break;
         case ASCENDING_EAST:
            if (p_55223_) {
               i--;
            } else {
               i++;
               j++;
               flag = false;
            }

            railshape = RailShape.EAST_WEST;
            break;
         case ASCENDING_WEST:
            if (p_55223_) {
               i--;
               j++;
               flag = false;
            } else {
               i++;
            }

            railshape = RailShape.EAST_WEST;
            break;
         case ASCENDING_NORTH:
            if (p_55223_) {
               k++;
            } else {
               k--;
               j++;
               flag = false;
            }

            railshape = RailShape.NORTH_SOUTH;
            break;
         case ASCENDING_SOUTH:
            if (p_55223_) {
               k++;
               j++;
               flag = false;
            } else {
               k--;
            }

            railshape = RailShape.NORTH_SOUTH;
      }

      return this.isSameRailWithPower(p_55220_, new BlockPos(i, j, k), p_55223_, p_55224_, railshape)
         ? true
         : flag && this.isSameRailWithPower(p_55220_, new BlockPos(i, j - 1, k), p_55223_, p_55224_, railshape);
   }

   protected boolean isSameRailWithPower(Level p_55226_, BlockPos p_55227_, boolean p_55228_, int p_55229_, RailShape p_55230_) {
      BlockState blockstate = p_55226_.getBlockState(p_55227_);
      if (!blockstate.is(this)) {
         return false;
      }

      RailShape railshape = blockstate.getValue(SHAPE);
      if (p_55230_ != RailShape.EAST_WEST
         || railshape != RailShape.NORTH_SOUTH && railshape != RailShape.ASCENDING_NORTH && railshape != RailShape.ASCENDING_SOUTH) {
         if (p_55230_ != RailShape.NORTH_SOUTH
            || railshape != RailShape.EAST_WEST && railshape != RailShape.ASCENDING_EAST && railshape != RailShape.ASCENDING_WEST) {
            if (!blockstate.getValue(POWERED)) {
               return false;
            } else {
               return p_55226_.hasNeighborSignal(p_55227_) ? true : this.findPoweredRailSignal(p_55226_, p_55227_, blockstate, p_55228_, p_55229_ + 1);
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   protected void updateState(BlockState p_55232_, Level p_55233_, BlockPos p_55234_, Block p_55235_) {
      boolean flag = p_55232_.getValue(POWERED);
      boolean flag1 = p_55233_.hasNeighborSignal(p_55234_)
         || this.findPoweredRailSignal(p_55233_, p_55234_, p_55232_, true, 0)
         || this.findPoweredRailSignal(p_55233_, p_55234_, p_55232_, false, 0);
      if (flag1 != flag) {
         p_55233_.setBlock(p_55234_, p_55232_.setValue(POWERED, flag1), 3);
         p_55233_.updateNeighborsAt(p_55234_.below(), this);
         if (p_55232_.getValue(SHAPE).isSlope()) {
            p_55233_.updateNeighborsAt(p_55234_.above(), this);
         }
      }
   }

   @Override
   public Property<RailShape> getShapeProperty() {
      return SHAPE;
   }

   @Override
   protected BlockState rotate(BlockState p_55240_, Rotation p_55241_) {
      RailShape railshape = p_55240_.getValue(SHAPE);
      RailShape railshape1 = this.rotate(railshape, p_55241_);
      return p_55240_.setValue(SHAPE, railshape1);
   }

   @Override
   protected BlockState mirror(BlockState p_55237_, Mirror p_55238_) {
      RailShape railshape = p_55237_.getValue(SHAPE);
      RailShape railshape1 = this.mirror(railshape, p_55238_);
      return p_55237_.setValue(SHAPE, railshape1);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55243_) {
      p_55243_.add(SHAPE, POWERED, WATERLOGGED);
   }
}
