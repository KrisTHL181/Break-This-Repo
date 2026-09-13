package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class RodBlock extends DirectionalBlock {
   private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateAllAxis(Block.cube(4.0, 4.0, 16.0));

   protected RodBlock(BlockBehaviour.Properties p_154339_) {
      super(p_154339_);
   }

   @Override
   protected abstract MapCodec<? extends RodBlock> codec();

   @Override
   protected VoxelShape getShape(BlockState p_154346_, BlockGetter p_154347_, BlockPos p_154348_, CollisionContext p_154349_) {
      return SHAPES.get(p_154346_.getValue(FACING).getAxis());
   }

   @Override
   protected BlockState rotate(BlockState p_154354_, Rotation p_154355_) {
      return p_154354_.setValue(FACING, p_154355_.rotate(p_154354_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_154351_, Mirror p_154352_) {
      return p_154351_.setValue(FACING, p_154352_.mirror(p_154351_.getValue(FACING)));
   }

   @Override
   protected boolean isPathfindable(BlockState p_154341_, PathComputationType p_154344_) {
      return false;
   }
}
