package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ScaffoldingBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<ScaffoldingBlock> CODEC = simpleCodec(ScaffoldingBlock::new);
   private static final int TICK_DELAY = 1;
   private static final VoxelShape SHAPE_STABLE = Shapes.or(
      Block.column(16.0, 14.0, 16.0), Shapes.rotateHorizontal(Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 2.0)).values().stream().reduce(Shapes.empty(), Shapes::or)
   );
   private static final VoxelShape SHAPE_UNSTABLE_BOTTOM = Block.column(16.0, 0.0, 2.0);
   private static final VoxelShape SHAPE_UNSTABLE = Shapes.or(
      SHAPE_STABLE, SHAPE_UNSTABLE_BOTTOM, Shapes.rotateHorizontal(Block.boxZ(16.0, 0.0, 2.0, 0.0, 2.0)).values().stream().reduce(Shapes.empty(), Shapes::or)
   );
   private static final VoxelShape SHAPE_BELOW_BLOCK = Shapes.block().move(0.0, -1.0, 0.0).optimize();
   public static final int STABILITY_MAX_DISTANCE = 7;
   public static final IntegerProperty DISTANCE = BlockStateProperties.STABILITY_DISTANCE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;

   @Override
   public MapCodec<ScaffoldingBlock> codec() {
      return CODEC;
   }

   protected ScaffoldingBlock(BlockBehaviour.Properties p_56021_) {
      super(p_56021_);
      this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, 7).setValue(WATERLOGGED, false).setValue(BOTTOM, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56051_) {
      p_56051_.add(DISTANCE, WATERLOGGED, BOTTOM);
   }

   @Override
   protected VoxelShape getShape(BlockState p_56057_, BlockGetter p_56058_, BlockPos p_56059_, CollisionContext p_56060_) {
      if (!p_56060_.isHoldingItem(p_56057_.getBlock().asItem())) {
         return p_56057_.getValue(BOTTOM) ? SHAPE_UNSTABLE : SHAPE_STABLE;
      } else {
         return Shapes.block();
      }
   }

   @Override
   protected VoxelShape getInteractionShape(BlockState p_56053_, BlockGetter p_56054_, BlockPos p_56055_) {
      return Shapes.block();
   }

   @Override
   protected boolean canBeReplaced(BlockState p_56037_, BlockPlaceContext p_56038_) {
      return p_56038_.getItemInHand().is(this.asItem());
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_56023_) {
      BlockPos blockpos = p_56023_.getClickedPos();
      Level level = p_56023_.getLevel();
      int i = getDistance(level, blockpos);
      return this.defaultBlockState()
         .setValue(WATERLOGGED, level.getFluidState(blockpos).getType() == Fluids.WATER)
         .setValue(DISTANCE, i)
         .setValue(BOTTOM, this.isBottom(level, blockpos, i));
   }

   @Override
   protected void onPlace(BlockState p_56062_, Level p_56063_, BlockPos p_56064_, BlockState p_56065_, boolean p_56066_) {
      if (!p_56063_.isClientSide()) {
         p_56063_.scheduleTick(p_56064_, this, 1);
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_56044_,
      LevelReader p_365588_,
      ScheduledTickAccess p_361394_,
      BlockPos p_56048_,
      Direction p_56045_,
      BlockPos p_56049_,
      BlockState p_56046_,
      RandomSource p_369734_
   ) {
      if (p_56044_.getValue(WATERLOGGED)) {
         p_361394_.scheduleTick(p_56048_, Fluids.WATER, Fluids.WATER.getTickDelay(p_365588_));
      }

      if (!p_365588_.isClientSide()) {
         p_361394_.scheduleTick(p_56048_, this, 1);
      }

      return p_56044_;
   }

   @Override
   protected void tick(BlockState p_222019_, ServerLevel p_222020_, BlockPos p_222021_, RandomSource p_222022_) {
      int i = getDistance(p_222020_, p_222021_);
      BlockState blockstate = p_222019_.setValue(DISTANCE, i).setValue(BOTTOM, this.isBottom(p_222020_, p_222021_, i));
      if (blockstate.getValue(DISTANCE) == 7) {
         if (p_222019_.getValue(DISTANCE) == 7) {
            FallingBlockEntity.fall(p_222020_, p_222021_, blockstate);
         } else {
            p_222020_.destroyBlock(p_222021_, true);
         }
      } else if (p_222019_ != blockstate) {
         p_222020_.setBlock(p_222021_, blockstate, 3);
      }
   }

   @Override
   protected boolean canSurvive(BlockState p_56040_, LevelReader p_56041_, BlockPos p_56042_) {
      return getDistance(p_56041_, p_56042_) < 7;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_56068_, BlockGetter p_56069_, BlockPos p_56070_, CollisionContext p_56071_) {
      if (p_56071_.isPlacement()) {
         return Shapes.empty();
      } else if (p_56071_.isAbove(Shapes.block(), p_56070_, true) && !p_56071_.isDescending()) {
         return SHAPE_STABLE;
      } else {
         return p_56068_.getValue(DISTANCE) != 0 && p_56068_.getValue(BOTTOM) && p_56071_.isAbove(SHAPE_BELOW_BLOCK, p_56070_, true)
            ? SHAPE_UNSTABLE_BOTTOM
            : Shapes.empty();
      }
   }

   @Override
   protected FluidState getFluidState(BlockState p_56073_) {
      return p_56073_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_56073_);
   }

   private boolean isBottom(BlockGetter p_56028_, BlockPos p_56029_, int p_56030_) {
      return p_56030_ > 0 && !p_56028_.getBlockState(p_56029_.below()).is(this);
   }

   public static int getDistance(BlockGetter p_56025_, BlockPos p_56026_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_56026_.mutable().move(Direction.DOWN);
      BlockState blockstate = p_56025_.getBlockState(blockpos$mutableblockpos);
      int i = 7;
      if (blockstate.is(Blocks.SCAFFOLDING)) {
         i = blockstate.getValue(DISTANCE);
      } else if (blockstate.isFaceSturdy(p_56025_, blockpos$mutableblockpos, Direction.UP)) {
         return 0;
      }

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockState blockstate1 = p_56025_.getBlockState(blockpos$mutableblockpos.setWithOffset(p_56026_, direction));
         if (blockstate1.is(Blocks.SCAFFOLDING)) {
            i = Math.min(i, blockstate1.getValue(DISTANCE) + 1);
            if (i == 1) {
               break;
            }
         }
      }

      return i;
   }
}
