package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class VineBlock extends Block {
   public static final MapCodec<VineBlock> CODEC = simpleCodec(VineBlock::new);
   public static final BooleanProperty UP = PipeBlock.UP;
   public static final BooleanProperty NORTH = PipeBlock.NORTH;
   public static final BooleanProperty EAST = PipeBlock.EAST;
   public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
   public static final BooleanProperty WEST = PipeBlock.WEST;
   public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
      .entrySet()
      .stream()
      .filter(p_57886_ -> p_57886_.getKey() != Direction.DOWN)
      .collect(Util.toMap());
   private final Function<BlockState, VoxelShape> shapes;

   @Override
   public MapCodec<VineBlock> codec() {
      return CODEC;
   }

   public VineBlock(BlockBehaviour.Properties p_57847_) {
      super(p_57847_);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(UP, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
      );
      this.shapes = this.makeShapes();
   }

   private Function<BlockState, VoxelShape> makeShapes() {
      Map<Direction, VoxelShape> map = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
      return this.getShapeForEachState(p_390956_ -> {
         VoxelShape voxelshape = Shapes.empty();

         for (Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            if (p_390956_.getValue(entry.getValue())) {
               voxelshape = Shapes.or(voxelshape, map.get(entry.getKey()));
            }
         }

         return voxelshape.isEmpty() ? Shapes.block() : voxelshape;
      });
   }

   @Override
   protected VoxelShape getShape(BlockState p_57897_, BlockGetter p_57898_, BlockPos p_57899_, CollisionContext p_57900_) {
      return this.shapes.apply(p_57897_);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_181239_) {
      return true;
   }

   @Override
   protected boolean canSurvive(BlockState p_57861_, LevelReader p_57862_, BlockPos p_57863_) {
      return this.hasFaces(this.getUpdatedState(p_57861_, p_57862_, p_57863_));
   }

   private boolean hasFaces(BlockState p_57908_) {
      return this.countFaces(p_57908_) > 0;
   }

   private int countFaces(BlockState p_57910_) {
      int i = 0;

      for (BooleanProperty booleanproperty : PROPERTY_BY_DIRECTION.values()) {
         if (p_57910_.getValue(booleanproperty)) {
            i++;
         }
      }

      return i;
   }

   private boolean canSupportAtFace(BlockGetter p_57888_, BlockPos p_57889_, Direction p_57890_) {
      if (p_57890_ == Direction.DOWN) {
         return false;
      }

      BlockPos blockpos = p_57889_.relative(p_57890_);
      if (isAcceptableNeighbour(p_57888_, blockpos, p_57890_)) {
         return true;
      }

      if (p_57890_.getAxis() == Direction.Axis.Y) {
         return false;
      }

      BooleanProperty booleanproperty = PROPERTY_BY_DIRECTION.get(p_57890_);
      BlockState blockstate = p_57888_.getBlockState(p_57889_.above());
      return blockstate.is(this) && blockstate.getValue(booleanproperty);
   }

   public static boolean isAcceptableNeighbour(BlockGetter p_57854_, BlockPos p_57855_, Direction p_57856_) {
      return MultifaceBlock.canAttachTo(p_57854_, p_57856_, p_57855_, p_57854_.getBlockState(p_57855_));
   }

   private BlockState getUpdatedState(BlockState p_57902_, BlockGetter p_57903_, BlockPos p_57904_) {
      BlockPos blockpos = p_57904_.above();
      if (p_57902_.getValue(UP)) {
         p_57902_ = p_57902_.setValue(UP, isAcceptableNeighbour(p_57903_, blockpos, Direction.DOWN));
      }

      BlockState blockstate = null;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BooleanProperty booleanproperty = getPropertyForFace(direction);
         if (p_57902_.getValue(booleanproperty)) {
            boolean flag = this.canSupportAtFace(p_57903_, p_57904_, direction);
            if (!flag) {
               if (blockstate == null) {
                  blockstate = p_57903_.getBlockState(blockpos);
               }

               flag = blockstate.is(this) && blockstate.getValue(booleanproperty);
            }

            p_57902_ = p_57902_.setValue(booleanproperty, flag);
         }
      }

      return p_57902_;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_57875_,
      LevelReader p_367171_,
      ScheduledTickAccess p_369591_,
      BlockPos p_57879_,
      Direction p_57876_,
      BlockPos p_57880_,
      BlockState p_57877_,
      RandomSource p_364384_
   ) {
      if (p_57876_ == Direction.DOWN) {
         return super.updateShape(p_57875_, p_367171_, p_369591_, p_57879_, p_57876_, p_57880_, p_57877_, p_364384_);
      }

      BlockState blockstate = this.getUpdatedState(p_57875_, p_367171_, p_57879_);
      return !this.hasFaces(blockstate) ? Blocks.AIR.defaultBlockState() : blockstate;
   }

   @Override
   protected void randomTick(BlockState p_222655_, ServerLevel p_222656_, BlockPos p_222657_, RandomSource p_222658_) {
      if (p_222656_.getGameRules().get(GameRules.SPREAD_VINES) && p_222658_.nextInt(4) == 0) {
         Direction direction = Direction.getRandom(p_222658_);
         BlockPos blockpos = p_222657_.above();
         if (direction.getAxis().isHorizontal() && !p_222655_.getValue(getPropertyForFace(direction))) {
            if (this.canSpread(p_222656_, p_222657_)) {
               BlockPos blockpos4 = p_222657_.relative(direction);
               BlockState blockstate4 = p_222656_.getBlockState(blockpos4);
               if (blockstate4.isAir()) {
                  Direction direction3 = direction.getClockWise();
                  Direction direction4 = direction.getCounterClockWise();
                  boolean flag = p_222655_.getValue(getPropertyForFace(direction3));
                  boolean flag1 = p_222655_.getValue(getPropertyForFace(direction4));
                  BlockPos blockpos2 = blockpos4.relative(direction3);
                  BlockPos blockpos3 = blockpos4.relative(direction4);
                  if (flag && isAcceptableNeighbour(p_222656_, blockpos2, direction3)) {
                     p_222656_.setBlock(blockpos4, this.defaultBlockState().setValue(getPropertyForFace(direction3), true), 2);
                  } else if (flag1 && isAcceptableNeighbour(p_222656_, blockpos3, direction4)) {
                     p_222656_.setBlock(blockpos4, this.defaultBlockState().setValue(getPropertyForFace(direction4), true), 2);
                  } else {
                     Direction direction1 = direction.getOpposite();
                     if (flag && p_222656_.isEmptyBlock(blockpos2) && isAcceptableNeighbour(p_222656_, p_222657_.relative(direction3), direction1)) {
                        p_222656_.setBlock(blockpos2, this.defaultBlockState().setValue(getPropertyForFace(direction1), true), 2);
                     } else if (flag1 && p_222656_.isEmptyBlock(blockpos3) && isAcceptableNeighbour(p_222656_, p_222657_.relative(direction4), direction1)) {
                        p_222656_.setBlock(blockpos3, this.defaultBlockState().setValue(getPropertyForFace(direction1), true), 2);
                     } else if (p_222658_.nextFloat() < 0.05 && isAcceptableNeighbour(p_222656_, blockpos4.above(), Direction.UP)) {
                        p_222656_.setBlock(blockpos4, this.defaultBlockState().setValue(UP, true), 2);
                     }
                  }
               } else if (isAcceptableNeighbour(p_222656_, blockpos4, direction)) {
                  p_222656_.setBlock(p_222657_, p_222655_.setValue(getPropertyForFace(direction), true), 2);
               }
            }
         } else {
            if (direction == Direction.UP && p_222657_.getY() < p_222656_.getMaxY()) {
               if (this.canSupportAtFace(p_222656_, p_222657_, direction)) {
                  p_222656_.setBlock(p_222657_, p_222655_.setValue(UP, true), 2);
                  return;
               }

               if (p_222656_.isEmptyBlock(blockpos)) {
                  if (!this.canSpread(p_222656_, p_222657_)) {
                     return;
                  }

                  BlockState blockstate3 = p_222655_;

                  for (Direction direction2 : Direction.Plane.HORIZONTAL) {
                     if (p_222658_.nextBoolean() || !isAcceptableNeighbour(p_222656_, blockpos.relative(direction2), direction2)) {
                        blockstate3 = blockstate3.setValue(getPropertyForFace(direction2), false);
                     }
                  }

                  if (this.hasHorizontalConnection(blockstate3)) {
                     p_222656_.setBlock(blockpos, blockstate3, 2);
                  }

                  return;
               }
            }

            if (p_222657_.getY() > p_222656_.getMinY()) {
               BlockPos blockpos1 = p_222657_.below();
               BlockState blockstate = p_222656_.getBlockState(blockpos1);
               if (blockstate.isAir() || blockstate.is(this)) {
                  BlockState blockstate1 = blockstate.isAir() ? this.defaultBlockState() : blockstate;
                  BlockState blockstate2 = this.copyRandomFaces(p_222655_, blockstate1, p_222658_);
                  if (blockstate1 != blockstate2 && this.hasHorizontalConnection(blockstate2)) {
                     p_222656_.setBlock(blockpos1, blockstate2, 2);
                  }
               }
            }
         }
      }
   }

   private BlockState copyRandomFaces(BlockState p_222651_, BlockState p_222652_, RandomSource p_222653_) {
      for (Direction direction : Direction.Plane.HORIZONTAL) {
         if (p_222653_.nextBoolean()) {
            BooleanProperty booleanproperty = getPropertyForFace(direction);
            if (p_222651_.getValue(booleanproperty)) {
               p_222652_ = p_222652_.setValue(booleanproperty, true);
            }
         }
      }

      return p_222652_;
   }

   private boolean hasHorizontalConnection(BlockState p_57912_) {
      return p_57912_.getValue(NORTH) || p_57912_.getValue(EAST) || p_57912_.getValue(SOUTH) || p_57912_.getValue(WEST);
   }

   private boolean canSpread(BlockGetter p_57851_, BlockPos p_57852_) {
      int i = 4;
      Iterable<BlockPos> iterable = BlockPos.betweenClosed(
         p_57852_.getX() - 4, p_57852_.getY() - 1, p_57852_.getZ() - 4, p_57852_.getX() + 4, p_57852_.getY() + 1, p_57852_.getZ() + 4
      );
      int j = 5;

      for (BlockPos blockpos : iterable) {
         if (p_57851_.getBlockState(blockpos).is(this)) {
            if (--j <= 0) {
               return false;
            }
         }
      }

      return true;
   }

   @Override
   protected boolean canBeReplaced(BlockState p_57858_, BlockPlaceContext p_57859_) {
      BlockState blockstate = p_57859_.getLevel().getBlockState(p_57859_.getClickedPos());
      return blockstate.is(this) ? this.countFaces(blockstate) < PROPERTY_BY_DIRECTION.size() : super.canBeReplaced(p_57858_, p_57859_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_57849_) {
      BlockState blockstate = p_57849_.getLevel().getBlockState(p_57849_.getClickedPos());
      boolean flag = blockstate.is(this);
      BlockState blockstate1 = flag ? blockstate : this.defaultBlockState();

      for (Direction direction : p_57849_.getNearestLookingDirections()) {
         if (direction != Direction.DOWN) {
            BooleanProperty booleanproperty = getPropertyForFace(direction);
            boolean flag1 = flag && blockstate.getValue(booleanproperty);
            if (!flag1 && this.canSupportAtFace(p_57849_.getLevel(), p_57849_.getClickedPos(), direction)) {
               return blockstate1.setValue(booleanproperty, true);
            }
         }
      }

      return flag ? blockstate1 : null;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57882_) {
      p_57882_.add(UP, NORTH, EAST, SOUTH, WEST);
   }

   @Override
   protected BlockState rotate(BlockState p_57868_, Rotation p_57869_) {
      switch (p_57869_) {
         case CLOCKWISE_180:
            return p_57868_.setValue(NORTH, p_57868_.getValue(SOUTH))
               .setValue(EAST, p_57868_.getValue(WEST))
               .setValue(SOUTH, p_57868_.getValue(NORTH))
               .setValue(WEST, p_57868_.getValue(EAST));
         case COUNTERCLOCKWISE_90:
            return p_57868_.setValue(NORTH, p_57868_.getValue(EAST))
               .setValue(EAST, p_57868_.getValue(SOUTH))
               .setValue(SOUTH, p_57868_.getValue(WEST))
               .setValue(WEST, p_57868_.getValue(NORTH));
         case CLOCKWISE_90:
            return p_57868_.setValue(NORTH, p_57868_.getValue(WEST))
               .setValue(EAST, p_57868_.getValue(NORTH))
               .setValue(SOUTH, p_57868_.getValue(EAST))
               .setValue(WEST, p_57868_.getValue(SOUTH));
         default:
            return p_57868_;
      }
   }

   @Override
   protected BlockState mirror(BlockState p_57865_, Mirror p_57866_) {
      switch (p_57866_) {
         case LEFT_RIGHT:
            return p_57865_.setValue(NORTH, p_57865_.getValue(SOUTH)).setValue(SOUTH, p_57865_.getValue(NORTH));
         case FRONT_BACK:
            return p_57865_.setValue(EAST, p_57865_.getValue(WEST)).setValue(WEST, p_57865_.getValue(EAST));
         default:
            return super.mirror(p_57865_, p_57866_);
      }
   }

   public static BooleanProperty getPropertyForFace(Direction p_57884_) {
      return PROPERTY_BY_DIRECTION.get(p_57884_);
   }
}
