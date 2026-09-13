package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SnowLayerBlock extends Block {
   public static final int MAX_HEIGHT = 8;
   public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
   private static final VoxelShape[] SHAPES = Block.boxes(8, height -> Block.column(16.0, 0.0, height * 2));
   public static final int HEIGHT_IMPASSABLE = 5;

   protected SnowLayerBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1));
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return type == PathComputationType.LAND ? state.getValue(LAYERS) < 5 : false;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[state.getValue(LAYERS)];
   }

   @Override
   protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[state.getValue(LAYERS) - 1];
   }

   @Override
   protected VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return SHAPES[state.getValue(LAYERS)];
   }

   @Override
   protected VoxelShape getVisualShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[state.getValue(LAYERS)];
   }

   @Override
   protected boolean useShapeForLightOcclusion(final BlockState state) {
      return true;
   }

   @Override
   protected float getShadeBrightness(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return state.getValue(LAYERS) == 8 ? 0.2F : 1.0F;
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      BlockState belowState = level.getBlockState(pos.below());
      if (belowState.is(BlockTags.CANNOT_SUPPORT_SNOW_LAYER)) {
         return false;
      } else {
         return belowState.is(BlockTags.SUPPORT_OVERRIDE_SNOW_LAYER)
            ? true
            : Block.isFaceFull(belowState.getCollisionShape(level, pos.below()), Direction.UP) || belowState.is(this) && belowState.getValue(LAYERS) == 8;
      }
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
      return !state.canSurvive(level, pos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (level.getBrightness(LightLayer.BLOCK, pos) > 11) {
         dropResources(state, level, pos);
         level.removeBlock(pos, false);
      }
   }

   @Override
   protected boolean canBeReplaced(final BlockState state, final BlockPlaceContext context) {
      int layers = state.getValue(LAYERS);
      if (!context.getItemInHand().is(this.asItem()) || layers >= 8) {
         return layers == 1;
      } else {
         return context.replacingClickedOnBlock() ? context.getClickedFace() == Direction.UP : true;
      }
   }

   @Override
   public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
      BlockState state = context.getLevel().getBlockState(context.getClickedPos());
      if (state.is(this)) {
         int layers = state.getValue(LAYERS);
         return state.setValue(LAYERS, Math.min(8, layers + 1));
      } else {
         return super.getStateForPlacement(context);
      }
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(LAYERS);
   }
}
