package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class CrossCollisionBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty NORTH = PipeBlock.NORTH;
   public static final BooleanProperty EAST = PipeBlock.EAST;
   public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
   public static final BooleanProperty WEST = PipeBlock.WEST;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
      .entrySet()
      .stream()
      .filter(p_52346_ -> p_52346_.getKey().getAxis().isHorizontal())
      .collect(Util.toMap());
   private final Function<BlockState, VoxelShape> collisionShapes;
   private final Function<BlockState, VoxelShape> shapes;

   protected CrossCollisionBlock(float p_52320_, float p_52321_, float p_52322_, float p_52323_, float p_52324_, BlockBehaviour.Properties p_52325_) {
      super(p_52325_);
      this.collisionShapes = this.makeShapes(p_52320_, p_52324_, p_52322_, 0.0F, p_52324_);
      this.shapes = this.makeShapes(p_52320_, p_52321_, p_52322_, 0.0F, p_52323_);
   }

   @Override
   protected abstract MapCodec<? extends CrossCollisionBlock> codec();

   protected Function<BlockState, VoxelShape> makeShapes(float p_52327_, float p_52328_, float p_52329_, float p_52330_, float p_52331_) {
      VoxelShape voxelshape = Block.column(p_52327_, 0.0, p_52328_);
      Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.boxZ(p_52329_, p_52330_, p_52331_, 0.0, 8.0));
      return this.getShapeForEachState(p_390934_ -> {
         VoxelShape voxelshape1 = voxelshape;

         for (Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            if (p_390934_.getValue(entry.getValue())) {
               voxelshape1 = Shapes.or(voxelshape1, map.get(entry.getKey()));
            }
         }

         return voxelshape1;
      }, WATERLOGGED);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_52348_) {
      return !p_52348_.getValue(WATERLOGGED);
   }

   @Override
   protected VoxelShape getShape(BlockState p_52352_, BlockGetter p_52353_, BlockPos p_52354_, CollisionContext p_52355_) {
      return this.shapes.apply(p_52352_);
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_52357_, BlockGetter p_52358_, BlockPos p_52359_, CollisionContext p_52360_) {
      return this.collisionShapes.apply(p_52357_);
   }

   @Override
   protected FluidState getFluidState(BlockState p_52362_) {
      return p_52362_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_52362_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_52333_, PathComputationType p_52336_) {
      return false;
   }

   @Override
   protected BlockState rotate(BlockState p_52341_, Rotation p_52342_) {
      switch (p_52342_) {
         case CLOCKWISE_180:
            return p_52341_.setValue(NORTH, p_52341_.getValue(SOUTH))
               .setValue(EAST, p_52341_.getValue(WEST))
               .setValue(SOUTH, p_52341_.getValue(NORTH))
               .setValue(WEST, p_52341_.getValue(EAST));
         case COUNTERCLOCKWISE_90:
            return p_52341_.setValue(NORTH, p_52341_.getValue(EAST))
               .setValue(EAST, p_52341_.getValue(SOUTH))
               .setValue(SOUTH, p_52341_.getValue(WEST))
               .setValue(WEST, p_52341_.getValue(NORTH));
         case CLOCKWISE_90:
            return p_52341_.setValue(NORTH, p_52341_.getValue(WEST))
               .setValue(EAST, p_52341_.getValue(NORTH))
               .setValue(SOUTH, p_52341_.getValue(EAST))
               .setValue(WEST, p_52341_.getValue(SOUTH));
         default:
            return p_52341_;
      }
   }

   @Override
   protected BlockState mirror(BlockState p_52338_, Mirror p_52339_) {
      switch (p_52339_) {
         case LEFT_RIGHT:
            return p_52338_.setValue(NORTH, p_52338_.getValue(SOUTH)).setValue(SOUTH, p_52338_.getValue(NORTH));
         case FRONT_BACK:
            return p_52338_.setValue(EAST, p_52338_.getValue(WEST)).setValue(WEST, p_52338_.getValue(EAST));
         default:
            return super.mirror(p_52338_, p_52339_);
      }
   }
}
