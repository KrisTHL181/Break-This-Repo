package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StairBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<StairBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422127_ -> p_422127_.group(BlockState.CODEC.fieldOf("base_state").forGetter(p_309296_ -> p_309296_.baseState), propertiesCodec())
         .apply(p_422127_, StairBlock::new)
   );
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
   public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE_OUTER = Shapes.or(Block.column(16.0, 0.0, 8.0), Block.box(0.0, 8.0, 0.0, 8.0, 16.0, 8.0));
   private static final VoxelShape SHAPE_STRAIGHT = Shapes.or(SHAPE_OUTER, Shapes.rotate(SHAPE_OUTER, OctahedralGroup.BLOCK_ROT_Y_90));
   private static final VoxelShape SHAPE_INNER = Shapes.or(SHAPE_STRAIGHT, Shapes.rotate(SHAPE_STRAIGHT, OctahedralGroup.BLOCK_ROT_Y_90));
   private static final Map<Direction, VoxelShape> SHAPE_BOTTOM_OUTER = Shapes.rotateHorizontal(SHAPE_OUTER);
   private static final Map<Direction, VoxelShape> SHAPE_BOTTOM_STRAIGHT = Shapes.rotateHorizontal(SHAPE_STRAIGHT);
   private static final Map<Direction, VoxelShape> SHAPE_BOTTOM_INNER = Shapes.rotateHorizontal(SHAPE_INNER);
   private static final Map<Direction, VoxelShape> SHAPE_TOP_OUTER = Shapes.rotateHorizontal(SHAPE_OUTER, OctahedralGroup.INVERT_Y);
   private static final Map<Direction, VoxelShape> SHAPE_TOP_STRAIGHT = Shapes.rotateHorizontal(SHAPE_STRAIGHT, OctahedralGroup.INVERT_Y);
   private static final Map<Direction, VoxelShape> SHAPE_TOP_INNER = Shapes.rotateHorizontal(SHAPE_INNER, OctahedralGroup.INVERT_Y);
   private final Block base;
   protected final BlockState baseState;

   @Override
   public MapCodec<? extends StairBlock> codec() {
      return CODEC;
   }

   protected StairBlock(BlockState p_56862_, BlockBehaviour.Properties p_56863_) {
      super(p_56863_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HALF, Half.BOTTOM)
            .setValue(SHAPE, StairsShape.STRAIGHT)
            .setValue(WATERLOGGED, false)
      );
      this.base = p_56862_.getBlock();
      this.baseState = p_56862_;
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState p_56967_) {
      return true;
   }

   @Override
   protected VoxelShape getShape(BlockState p_56956_, BlockGetter p_56957_, BlockPos p_56958_, CollisionContext p_56959_) {
      boolean flag = p_56956_.getValue(HALF) == Half.BOTTOM;
      Direction direction = p_56956_.getValue(FACING);

      Map map = switch ((StairsShape)p_56956_.getValue(SHAPE)) {
         case STRAIGHT -> flag ? SHAPE_BOTTOM_STRAIGHT : SHAPE_TOP_STRAIGHT;
         case OUTER_LEFT, OUTER_RIGHT -> flag ? SHAPE_BOTTOM_OUTER : SHAPE_TOP_OUTER;
         case INNER_RIGHT, INNER_LEFT -> flag ? SHAPE_BOTTOM_INNER : SHAPE_TOP_INNER;
      };

      return (VoxelShape)map.get(switch ((StairsShape)p_56956_.getValue(SHAPE)) {
         case STRAIGHT, OUTER_LEFT, INNER_RIGHT -> direction;
         case OUTER_RIGHT -> direction.getClockWise();
         case INNER_LEFT -> direction.getCounterClockWise();
      });
   }

   @Override
   public float getExplosionResistance() {
      return this.base.getExplosionResistance();
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_56872_) {
      Direction direction = p_56872_.getClickedFace();
      BlockPos blockpos = p_56872_.getClickedPos();
      FluidState fluidstate = p_56872_.getLevel().getFluidState(blockpos);
      BlockState blockstate = this.defaultBlockState()
         .setValue(FACING, p_56872_.getHorizontalDirection())
         .setValue(
            HALF, direction == Direction.DOWN || direction != Direction.UP && p_56872_.getClickLocation().y - blockpos.getY() > 0.5 ? Half.TOP : Half.BOTTOM
         )
         .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
      return blockstate.setValue(SHAPE, getStairsShape(blockstate, p_56872_.getLevel(), blockpos));
   }

   @Override
   protected BlockState updateShape(
      BlockState p_56925_,
      LevelReader p_369543_,
      ScheduledTickAccess p_369679_,
      BlockPos p_56929_,
      Direction p_56926_,
      BlockPos p_56930_,
      BlockState p_56927_,
      RandomSource p_367682_
   ) {
      if (p_56925_.getValue(WATERLOGGED)) {
         p_369679_.scheduleTick(p_56929_, Fluids.WATER, Fluids.WATER.getTickDelay(p_369543_));
      }

      return p_56926_.getAxis().isHorizontal()
         ? p_56925_.setValue(SHAPE, getStairsShape(p_56925_, p_369543_, p_56929_))
         : super.updateShape(p_56925_, p_369543_, p_369679_, p_56929_, p_56926_, p_56930_, p_56927_, p_367682_);
   }

   private static StairsShape getStairsShape(BlockState p_56977_, BlockGetter p_56978_, BlockPos p_56979_) {
      Direction direction = p_56977_.getValue(FACING);
      BlockState blockstate = p_56978_.getBlockState(p_56979_.relative(direction));
      if (isStairs(blockstate) && p_56977_.getValue(HALF) == blockstate.getValue(HALF)) {
         Direction direction1 = blockstate.getValue(FACING);
         if (direction1.getAxis() != p_56977_.getValue(FACING).getAxis() && canTakeShape(p_56977_, p_56978_, p_56979_, direction1.getOpposite())) {
            if (direction1 == direction.getCounterClockWise()) {
               return StairsShape.OUTER_LEFT;
            }

            return StairsShape.OUTER_RIGHT;
         }
      }

      BlockState blockstate1 = p_56978_.getBlockState(p_56979_.relative(direction.getOpposite()));
      if (isStairs(blockstate1) && p_56977_.getValue(HALF) == blockstate1.getValue(HALF)) {
         Direction direction2 = blockstate1.getValue(FACING);
         if (direction2.getAxis() != p_56977_.getValue(FACING).getAxis() && canTakeShape(p_56977_, p_56978_, p_56979_, direction2)) {
            if (direction2 == direction.getCounterClockWise()) {
               return StairsShape.INNER_LEFT;
            }

            return StairsShape.INNER_RIGHT;
         }
      }

      return StairsShape.STRAIGHT;
   }

   private static boolean canTakeShape(BlockState p_56971_, BlockGetter p_56972_, BlockPos p_56973_, Direction p_56974_) {
      BlockState blockstate = p_56972_.getBlockState(p_56973_.relative(p_56974_));
      return !isStairs(blockstate) || blockstate.getValue(FACING) != p_56971_.getValue(FACING) || blockstate.getValue(HALF) != p_56971_.getValue(HALF);
   }

   public static boolean isStairs(BlockState p_56981_) {
      return p_56981_.getBlock() instanceof StairBlock;
   }

   @Override
   protected BlockState rotate(BlockState p_56922_, Rotation p_56923_) {
      return p_56922_.setValue(FACING, p_56923_.rotate(p_56922_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_56919_, Mirror p_56920_) {
      Direction direction = p_56919_.getValue(FACING);
      StairsShape stairsshape = p_56919_.getValue(SHAPE);
      switch (p_56920_) {
         case LEFT_RIGHT:
            if (direction.getAxis() == Direction.Axis.Z) {
               switch (stairsshape) {
                  case OUTER_LEFT:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                  case OUTER_RIGHT:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                  case INNER_RIGHT:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                  case INNER_LEFT:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                  default:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180);
               }
            }
            break;
         case FRONT_BACK:
            if (direction.getAxis() == Direction.Axis.X) {
               switch (stairsshape) {
                  case STRAIGHT:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180);
                  case OUTER_LEFT:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                  case OUTER_RIGHT:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                  case INNER_RIGHT:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                  case INNER_LEFT:
                     return p_56919_.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
               }
            }
      }

      return super.mirror(p_56919_, p_56920_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56932_) {
      p_56932_.add(FACING, HALF, SHAPE, WATERLOGGED);
   }

   @Override
   protected FluidState getFluidState(BlockState p_56969_) {
      return p_56969_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_56969_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_56891_, PathComputationType p_56894_) {
      return false;
   }
}
