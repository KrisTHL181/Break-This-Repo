package net.minecraft.world.level.block;

import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LeavesBlock extends Block implements SimpleWaterloggedBlock {
   public static final int DECAY_DISTANCE = 7;
   public static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
   public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected final AmbientLeavesBlockSoundPlayer ambientLeavesBlockSoundPlayer;
   private static final int TICK_DELAY = 1;
   private static volatile boolean cutoutLeaves = true;

   public LeavesBlock(final AmbientLeavesBlockSoundPlayer ambientLeavesBlockSoundPlayer, final BlockBehaviour.Properties properties) {
      super(properties);
      this.ambientLeavesBlockSoundPlayer = ambientLeavesBlockSoundPlayer;
      this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, 7).setValue(PERSISTENT, false).setValue(WATERLOGGED, false));
   }

   @Override
   protected boolean skipRendering(final BlockState state, final BlockState neighborState, final Direction direction) {
      return !cutoutLeaves && neighborState.getBlock() instanceof LeavesBlock ? true : super.skipRendering(state, neighborState, direction);
   }

   public static void setCutoutLeaves(final boolean cutoutLeaves) {
      LeavesBlock.cutoutLeaves = cutoutLeaves;
   }

   @Override
   protected VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return Shapes.empty();
   }

   @Override
   protected boolean isRandomlyTicking(final BlockState state) {
      return state.getValue(DISTANCE) == 7 && !state.getValue(PERSISTENT);
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (this.decaying(state)) {
         dropResources(state, level, pos);
         level.removeBlock(pos, false);
      }
   }

   protected boolean decaying(final BlockState state) {
      return !state.getValue(PERSISTENT) && state.getValue(DISTANCE) == 7;
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      level.setBlockAndUpdate(pos, updateDistance(state, level, pos));
   }

   @Override
   protected int getLightDampening(final BlockState state) {
      return 1;
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
      if (state.getValue(WATERLOGGED)) {
         ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      int distanceFromNeighbor = getDistanceAt(neighbourState) + 1;
      if (distanceFromNeighbor != 1 || state.getValue(DISTANCE) != distanceFromNeighbor) {
         ticks.scheduleTick(pos, this, 1);
      }

      return state;
   }

   private static BlockState updateDistance(final BlockState state, final LevelAccessor level, final BlockPos pos) {
      int newDistance = 7;
      BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.values()) {
         neighborPos.setWithOffset(pos, direction);
         newDistance = Math.min(newDistance, getDistanceAt(level.getBlockState(neighborPos)) + 1);
         if (newDistance == 1) {
            break;
         }
      }

      return state.setValue(DISTANCE, newDistance);
   }

   private static int getDistanceAt(final BlockState state) {
      return getOptionalDistanceAt(state).orElse(7);
   }

   public static OptionalInt getOptionalDistanceAt(final BlockState state) {
      if (state.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY)) {
         return OptionalInt.of(0);
      } else {
         return state.hasProperty(DISTANCE) ? OptionalInt.of(state.getValue(DISTANCE)) : OptionalInt.empty();
      }
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      super.animateTick(state, level, pos, random);
      BlockPos below = pos.below();
      BlockState belowState = level.getBlockState(below);
      makeDrippingWaterParticles(level, pos, random, belowState, below);
      this.ambientLeavesBlockSoundPlayer.playAmbientLeavesSounds(level, pos, this, random);
   }

   private static void makeDrippingWaterParticles(
      final Level level, final BlockPos pos, final RandomSource random, final BlockState belowState, final BlockPos below
   ) {
      if (level.isRainingAt(pos.above())) {
         if (random.nextInt(15) == 1) {
            if (!belowState.canOcclude() || !belowState.isFaceSturdy(level, below, Direction.UP)) {
               ParticleUtils.spawnParticleBelow(level, pos, random, ParticleTypes.DRIPPING_WATER);
            }
         }
      }
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(DISTANCE, PERSISTENT, WATERLOGGED);
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      BlockState state = this.defaultBlockState().setValue(PERSISTENT, true).setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
      return updateDistance(state, context.getLevel(), context.getClickedPos());
   }
}
