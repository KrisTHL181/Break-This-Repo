package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BambooStalkBlock extends Block implements BonemealableBlock {
   private static final VoxelShape SHAPE_SMALL = Block.column(6.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_LARGE = Block.column(10.0, 0.0, 16.0);
   private static final VoxelShape SHAPE_COLLISION = Block.column(3.0, 0.0, 16.0);
   public static final IntegerProperty AGE = BlockStateProperties.AGE_1;
   public static final EnumProperty<BambooLeaves> LEAVES = BlockStateProperties.BAMBOO_LEAVES;
   public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
   public static final int MAX_HEIGHT = 16;
   public static final int STAGE_GROWING = 0;
   public static final int STAGE_DONE_GROWING = 1;
   public static final int AGE_THIN_BAMBOO = 0;
   public static final int AGE_THICK_BAMBOO = 1;

   public BambooStalkBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(LEAVES, BambooLeaves.NONE).setValue(STAGE, 0));
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(AGE, LEAVES, STAGE);
   }

   @Override
   protected boolean propagatesSkylightDown(final BlockState state) {
      return true;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      VoxelShape shape = state.getValue(LEAVES) == BambooLeaves.LARGE ? SHAPE_LARGE : SHAPE_SMALL;
      return shape.move(state.getOffset(pos));
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   @Override
   protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE_COLLISION.move(state.getOffset(pos));
   }

   @Override
   protected boolean isCollisionShapeFullBlock(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return false;
   }

   @Override
   public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
      FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
      if (!fluidState.isEmpty()) {
         return null;
      }

      BlockState belowState = context.getLevel().getBlockState(context.getClickedPos().below());
      if (belowState.is(BlockTags.SUPPORTS_BAMBOO)) {
         if (belowState.is(Blocks.BAMBOO_SAPLING)) {
            return this.defaultBlockState().setValue(AGE, 0);
         } else if (belowState.is(Blocks.BAMBOO)) {
            int age = belowState.getValue(AGE) > 0 ? 1 : 0;
            return this.defaultBlockState().setValue(AGE, age);
         } else {
            BlockState aboveState = context.getLevel().getBlockState(context.getClickedPos().above());
            return aboveState.is(Blocks.BAMBOO) ? this.defaultBlockState().setValue(AGE, aboveState.getValue(AGE)) : Blocks.BAMBOO_SAPLING.defaultBlockState();
         }
      } else {
         return null;
      }
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (!state.canSurvive(level, pos)) {
         level.destroyBlock(pos, true);
      }
   }

   @Override
   protected boolean isRandomlyTicking(final BlockState state) {
      return state.getValue(STAGE) == 0;
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (state.getValue(STAGE) == 0) {
         if (random.nextInt(3) == 0 && level.isEmptyBlock(pos.above()) && level.getRawBrightness(pos.above(), 0) >= 9) {
            int height = this.getHeightBelowUpToMax(level, pos) + 1;
            if (height < 16) {
               this.growBamboo(state, level, pos, random, height);
            }
         }
      }
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      return level.getBlockState(pos.below()).is(BlockTags.SUPPORTS_BAMBOO);
   }

   @Override
   protected BlockState updateShape(
      final BlockState state,
      final LevelReader level,
      final ScheduledTickAccess ticks,
      final BlockPos pos,
      final Direction directionToNeighbour,
      final BlockPos neighbourPos,
      final BlockState neighbourState,
      final RandomSource random
   ) {
      if (!state.canSurvive(level, pos)) {
         ticks.scheduleTick(pos, this, 1);
      }

      return directionToNeighbour == Direction.UP && neighbourState.is(Blocks.BAMBOO) && neighbourState.getValue(AGE) > state.getValue(AGE)
         ? state.cycle(AGE)
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      int heightAbove = this.getHeightAboveUpToMax(level, pos);
      int heightBelow = this.getHeightBelowUpToMax(level, pos);
      BlockPos growthPos = pos.above(heightAbove + 1);
      return heightAbove + heightBelow + 1 < 16
         && level.getBlockState(pos.above(heightAbove)).getValue(STAGE) != 1
         && level.isInsideBuildHeight(growthPos)
         && level.isEmptyBlock(growthPos);
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      int heightAbove = this.getHeightAboveUpToMax(level, pos);
      int heightBelow = this.getHeightBelowUpToMax(level, pos);
      int totalHeight = heightAbove + heightBelow + 1;
      int newBamboo = 1 + random.nextInt(2);

      for (int i = 0; i < newBamboo; i++) {
         BlockPos topPos = pos.above(heightAbove);
         BlockState topState = level.getBlockState(topPos);
         BlockPos growthPos = topPos.above();
         if (totalHeight >= 16 || topState.getValue(STAGE) == 1 || !level.isEmptyBlock(growthPos) || level.isOutsideBuildHeight(growthPos)) {
            return;
         }

         this.growBamboo(topState, level, topPos, random, totalHeight);
         heightAbove++;
         totalHeight++;
      }
   }

   protected void growBamboo(final BlockState state, final Level level, final BlockPos pos, final RandomSource random, final int height) {
      BlockState belowState = level.getBlockState(pos.below());
      BlockPos twoBelowPos = pos.below(2);
      BlockState twoBelowState = level.getBlockState(twoBelowPos);
      BambooLeaves leaves = BambooLeaves.NONE;
      if (height >= 1) {
         if (!belowState.is(Blocks.BAMBOO) || belowState.getValue(LEAVES) == BambooLeaves.NONE) {
            leaves = BambooLeaves.SMALL;
         } else if (belowState.is(Blocks.BAMBOO) && belowState.getValue(LEAVES) != BambooLeaves.NONE) {
            leaves = BambooLeaves.LARGE;
            if (twoBelowState.is(Blocks.BAMBOO)) {
               level.setBlockAndUpdate(pos.below(), belowState.setValue(LEAVES, BambooLeaves.SMALL));
               level.setBlockAndUpdate(twoBelowPos, twoBelowState.setValue(LEAVES, BambooLeaves.NONE));
            }
         }
      }

      int age = state.getValue(AGE) != 1 && !twoBelowState.is(Blocks.BAMBOO) ? 0 : 1;
      int stage = (height < 11 || !(random.nextFloat() < 0.25F)) && height != 15 ? 0 : 1;
      level.setBlockAndUpdate(pos.above(), this.defaultBlockState().setValue(AGE, age).setValue(LEAVES, leaves).setValue(STAGE, stage));
   }

   protected int getHeightAboveUpToMax(final BlockGetter level, final BlockPos pos) {
      int height = 0;

      while (height < 16 && level.getBlockState(pos.above(height + 1)).is(Blocks.BAMBOO)) {
         height++;
      }

      return height;
   }

   protected int getHeightBelowUpToMax(final BlockGetter level, final BlockPos pos) {
      int height = 0;

      while (height < 16 && level.getBlockState(pos.below(height + 1)).is(Blocks.BAMBOO)) {
         height++;
      }

      return height;
   }
}
