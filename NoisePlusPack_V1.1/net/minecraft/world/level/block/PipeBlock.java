package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class PipeBlock extends Block {
   public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
   public static final BooleanProperty EAST = BlockStateProperties.EAST;
   public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
   public static final BooleanProperty WEST = BlockStateProperties.WEST;
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
   public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
      Maps.newEnumMap(
         Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST, Direction.UP, UP, Direction.DOWN, DOWN)
      )
   );
   private final Function<BlockState, VoxelShape> shapes;

   protected PipeBlock(float p_55159_, BlockBehaviour.Properties p_55160_) {
      super(p_55160_);
      this.shapes = this.makeShapes(p_55159_);
   }

   @Override
   protected abstract MapCodec<? extends PipeBlock> codec();

   private Function<BlockState, VoxelShape> makeShapes(float p_55162_) {
      VoxelShape voxelshape = Block.cube(p_55162_);
      Map<Direction, VoxelShape> map = Shapes.rotateAll(Block.boxZ(p_55162_, 0.0, 8.0));
      return this.getShapeForEachState(p_390949_ -> {
         VoxelShape voxelshape1 = voxelshape;

         for (Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            if (p_390949_.getValue(entry.getValue())) {
               voxelshape1 = Shapes.or(map.get(entry.getKey()), voxelshape1);
            }
         }

         return voxelshape1;
      });
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_55166_) {
      return false;
   }

   @Override
   protected VoxelShape getShape(BlockState p_55170_, BlockGetter p_55171_, BlockPos p_55172_, CollisionContext p_55173_) {
      return this.shapes.apply(p_55170_);
   }
}
