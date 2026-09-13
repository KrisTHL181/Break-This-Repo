package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<WallBlock> CODEC = simpleCodec(WallBlock::new);
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final EnumProperty<WallSide> EAST = BlockStateProperties.EAST_WALL;
   public static final EnumProperty<WallSide> NORTH = BlockStateProperties.NORTH_WALL;
   public static final EnumProperty<WallSide> SOUTH = BlockStateProperties.SOUTH_WALL;
   public static final EnumProperty<WallSide> WEST = BlockStateProperties.WEST_WALL;
   public static final Map<Direction, EnumProperty<WallSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
      Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
   );
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private final Function<BlockState, VoxelShape> shapes;
   private final Function<BlockState, VoxelShape> collisionShapes;
   private static final VoxelShape TEST_SHAPE_POST = Block.column(2.0, 0.0, 16.0);
   private static final Map<Direction, VoxelShape> TEST_SHAPES_WALL = Shapes.rotateHorizontal(Block.boxZ(2.0, 16.0, 0.0, 9.0));

   @Override
   public MapCodec<WallBlock> codec() {
      return CODEC;
   }

   public WallBlock(BlockBehaviour.Properties p_57964_) {
      super(p_57964_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(UP, true)
            .setValue(NORTH, WallSide.NONE)
            .setValue(EAST, WallSide.NONE)
            .setValue(SOUTH, WallSide.NONE)
            .setValue(WEST, WallSide.NONE)
            .setValue(WATERLOGGED, false)
      );
      this.shapes = this.makeShapes(16.0F, 14.0F);
      this.collisionShapes = this.makeShapes(24.0F, 24.0F);
   }

   private Function<BlockState, VoxelShape> makeShapes(float p_57966_, float p_57967_) {
      VoxelShape voxelshape = Block.column(8.0, 0.0, p_57966_);
      int i = 6;
      Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, p_57967_, 0.0, 11.0));
      Map<Direction, VoxelShape> map1 = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, p_57966_, 0.0, 11.0));
      return this.getShapeForEachState(p_394482_ -> {
         VoxelShape voxelshape1 = p_394482_.getValue(UP) ? voxelshape : Shapes.empty();

         for (Entry<Direction, EnumProperty<WallSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            voxelshape1 = Shapes.or(voxelshape1, switch ((WallSide)p_394482_.getValue(entry.getValue())) {
               case NONE -> Shapes.empty();
               case LOW -> (VoxelShape)map.get(entry.getKey());
               case TALL -> (VoxelShape)map1.get(entry.getKey());
            });
         }

         return voxelshape1;
      }, WATERLOGGED);
   }

   @Override
   protected VoxelShape getShape(BlockState p_58050_, BlockGetter p_58051_, BlockPos p_58052_, CollisionContext p_58053_) {
      return this.shapes.apply(p_58050_);
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_58055_, BlockGetter p_58056_, BlockPos p_58057_, CollisionContext p_58058_) {
      return this.collisionShapes.apply(p_58055_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_57996_, PathComputationType p_57999_) {
      return false;
   }

   private boolean connectsTo(BlockState p_58021_, boolean p_58022_, Direction p_58023_) {
      Block block = p_58021_.getBlock();
      boolean flag = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(p_58021_, p_58023_);
      return p_58021_.is(BlockTags.WALLS) || !isExceptionForConnection(p_58021_) && p_58022_ || block instanceof IronBarsBlock || flag;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_57973_) {
      LevelReader levelreader = p_57973_.getLevel();
      BlockPos blockpos = p_57973_.getClickedPos();
      FluidState fluidstate = p_57973_.getLevel().getFluidState(p_57973_.getClickedPos());
      BlockPos blockpos1 = blockpos.north();
      BlockPos blockpos2 = blockpos.east();
      BlockPos blockpos3 = blockpos.south();
      BlockPos blockpos4 = blockpos.west();
      BlockPos blockpos5 = blockpos.above();
      BlockState blockstate = levelreader.getBlockState(blockpos1);
      BlockState blockstate1 = levelreader.getBlockState(blockpos2);
      BlockState blockstate2 = levelreader.getBlockState(blockpos3);
      BlockState blockstate3 = levelreader.getBlockState(blockpos4);
      BlockState blockstate4 = levelreader.getBlockState(blockpos5);
      boolean flag = this.connectsTo(blockstate, blockstate.isFaceSturdy(levelreader, blockpos1, Direction.SOUTH), Direction.SOUTH);
      boolean flag1 = this.connectsTo(blockstate1, blockstate1.isFaceSturdy(levelreader, blockpos2, Direction.WEST), Direction.WEST);
      boolean flag2 = this.connectsTo(blockstate2, blockstate2.isFaceSturdy(levelreader, blockpos3, Direction.NORTH), Direction.NORTH);
      boolean flag3 = this.connectsTo(blockstate3, blockstate3.isFaceSturdy(levelreader, blockpos4, Direction.EAST), Direction.EAST);
      BlockState blockstate5 = this.defaultBlockState().setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
      return this.updateShape(levelreader, blockstate5, blockpos5, blockstate4, flag, flag1, flag2, flag3);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_58014_,
      LevelReader p_363038_,
      ScheduledTickAccess p_368345_,
      BlockPos p_58018_,
      Direction p_58015_,
      BlockPos p_58019_,
      BlockState p_58016_,
      RandomSource p_364621_
   ) {
      if (p_58014_.getValue(WATERLOGGED)) {
         p_368345_.scheduleTick(p_58018_, Fluids.WATER, Fluids.WATER.getTickDelay(p_363038_));
      }

      if (p_58015_ == Direction.DOWN) {
         return super.updateShape(p_58014_, p_363038_, p_368345_, p_58018_, p_58015_, p_58019_, p_58016_, p_364621_);
      } else {
         return p_58015_ == Direction.UP
            ? this.topUpdate(p_363038_, p_58014_, p_58019_, p_58016_)
            : this.sideUpdate(p_363038_, p_58018_, p_58014_, p_58019_, p_58016_, p_58015_);
      }
   }

   private static boolean isConnected(BlockState p_58011_, Property<WallSide> p_58012_) {
      return p_58011_.getValue(p_58012_) != WallSide.NONE;
   }

   private static boolean isCovered(VoxelShape p_58039_, VoxelShape p_58040_) {
      return !Shapes.joinIsNotEmpty(p_58040_, p_58039_, BooleanOp.ONLY_FIRST);
   }

   private BlockState topUpdate(LevelReader p_57975_, BlockState p_57976_, BlockPos p_57977_, BlockState p_57978_) {
      boolean flag = isConnected(p_57976_, NORTH);
      boolean flag1 = isConnected(p_57976_, EAST);
      boolean flag2 = isConnected(p_57976_, SOUTH);
      boolean flag3 = isConnected(p_57976_, WEST);
      return this.updateShape(p_57975_, p_57976_, p_57977_, p_57978_, flag, flag1, flag2, flag3);
   }

   private BlockState sideUpdate(LevelReader p_57989_, BlockPos p_57990_, BlockState p_57991_, BlockPos p_57992_, BlockState p_57993_, Direction p_57994_) {
      Direction direction = p_57994_.getOpposite();
      boolean flag = p_57994_ == Direction.NORTH
         ? this.connectsTo(p_57993_, p_57993_.isFaceSturdy(p_57989_, p_57992_, direction), direction)
         : isConnected(p_57991_, NORTH);
      boolean flag1 = p_57994_ == Direction.EAST
         ? this.connectsTo(p_57993_, p_57993_.isFaceSturdy(p_57989_, p_57992_, direction), direction)
         : isConnected(p_57991_, EAST);
      boolean flag2 = p_57994_ == Direction.SOUTH
         ? this.connectsTo(p_57993_, p_57993_.isFaceSturdy(p_57989_, p_57992_, direction), direction)
         : isConnected(p_57991_, SOUTH);
      boolean flag3 = p_57994_ == Direction.WEST
         ? this.connectsTo(p_57993_, p_57993_.isFaceSturdy(p_57989_, p_57992_, direction), direction)
         : isConnected(p_57991_, WEST);
      BlockPos blockpos = p_57990_.above();
      BlockState blockstate = p_57989_.getBlockState(blockpos);
      return this.updateShape(p_57989_, p_57991_, blockpos, blockstate, flag, flag1, flag2, flag3);
   }

   private BlockState updateShape(
      LevelReader p_57980_, BlockState p_57981_, BlockPos p_57982_, BlockState p_57983_, boolean p_57984_, boolean p_57985_, boolean p_57986_, boolean p_57987_
   ) {
      VoxelShape voxelshape = p_57983_.getCollisionShape(p_57980_, p_57982_).getFaceShape(Direction.DOWN);
      BlockState blockstate = this.updateSides(p_57981_, p_57984_, p_57985_, p_57986_, p_57987_, voxelshape);
      return blockstate.setValue(UP, this.shouldRaisePost(blockstate, p_57983_, voxelshape));
   }

   private boolean shouldRaisePost(BlockState p_58007_, BlockState p_58008_, VoxelShape p_58009_) {
      boolean flag = p_58008_.getBlock() instanceof WallBlock && p_58008_.getValue(UP);
      if (flag) {
         return true;
      }

      WallSide wallside = p_58007_.getValue(NORTH);
      WallSide wallside1 = p_58007_.getValue(SOUTH);
      WallSide wallside2 = p_58007_.getValue(EAST);
      WallSide wallside3 = p_58007_.getValue(WEST);
      boolean flag1 = wallside1 == WallSide.NONE;
      boolean flag2 = wallside3 == WallSide.NONE;
      boolean flag3 = wallside2 == WallSide.NONE;
      boolean flag4 = wallside == WallSide.NONE;
      boolean flag5 = flag4 && flag1 && flag2 && flag3 || flag4 != flag1 || flag2 != flag3;
      if (flag5) {
         return true;
      }

      boolean flag6 = wallside == WallSide.TALL && wallside1 == WallSide.TALL || wallside2 == WallSide.TALL && wallside3 == WallSide.TALL;
      return flag6 ? false : p_58008_.is(BlockTags.WALL_POST_OVERRIDE) || isCovered(p_58009_, TEST_SHAPE_POST);
   }

   private BlockState updateSides(BlockState p_58025_, boolean p_58026_, boolean p_58027_, boolean p_58028_, boolean p_58029_, VoxelShape p_58030_) {
      return p_58025_.setValue(NORTH, this.makeWallState(p_58026_, p_58030_, TEST_SHAPES_WALL.get(Direction.NORTH)))
         .setValue(EAST, this.makeWallState(p_58027_, p_58030_, TEST_SHAPES_WALL.get(Direction.EAST)))
         .setValue(SOUTH, this.makeWallState(p_58028_, p_58030_, TEST_SHAPES_WALL.get(Direction.SOUTH)))
         .setValue(WEST, this.makeWallState(p_58029_, p_58030_, TEST_SHAPES_WALL.get(Direction.WEST)));
   }

   private WallSide makeWallState(boolean p_58042_, VoxelShape p_58043_, VoxelShape p_58044_) {
      if (p_58042_) {
         return isCovered(p_58043_, p_58044_) ? WallSide.TALL : WallSide.LOW;
      } else {
         return WallSide.NONE;
      }
   }

   @Override
   protected FluidState getFluidState(BlockState p_58060_) {
      return p_58060_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_58060_);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_58046_) {
      return !p_58046_.getValue(WATERLOGGED);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_58032_) {
      p_58032_.add(UP, NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }

   @Override
   protected BlockState rotate(BlockState p_58004_, Rotation p_58005_) {
      switch (p_58005_) {
         case CLOCKWISE_180:
            return p_58004_.setValue(NORTH, p_58004_.getValue(SOUTH))
               .setValue(EAST, p_58004_.getValue(WEST))
               .setValue(SOUTH, p_58004_.getValue(NORTH))
               .setValue(WEST, p_58004_.getValue(EAST));
         case COUNTERCLOCKWISE_90:
            return p_58004_.setValue(NORTH, p_58004_.getValue(EAST))
               .setValue(EAST, p_58004_.getValue(SOUTH))
               .setValue(SOUTH, p_58004_.getValue(WEST))
               .setValue(WEST, p_58004_.getValue(NORTH));
         case CLOCKWISE_90:
            return p_58004_.setValue(NORTH, p_58004_.getValue(WEST))
               .setValue(EAST, p_58004_.getValue(NORTH))
               .setValue(SOUTH, p_58004_.getValue(EAST))
               .setValue(WEST, p_58004_.getValue(SOUTH));
         default:
            return p_58004_;
      }
   }

   @Override
   protected BlockState mirror(BlockState p_58001_, Mirror p_58002_) {
      switch (p_58002_) {
         case LEFT_RIGHT:
            return p_58001_.setValue(NORTH, p_58001_.getValue(SOUTH)).setValue(SOUTH, p_58001_.getValue(NORTH));
         case FRONT_BACK:
            return p_58001_.setValue(EAST, p_58001_.getValue(WEST)).setValue(WEST, p_58001_.getValue(EAST));
         default:
            return super.mirror(p_58001_, p_58002_);
      }
   }
}
