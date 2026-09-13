package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PathBlock extends Block {
   private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 15.0);
   private final Block baseBlock;

   protected PathBlock(final Block baseBlock, final BlockBehaviour.Properties properties) {
      super(properties);
      this.baseBlock = baseBlock;
   }

   @Override
   protected boolean useShapeForLightOcclusion(final BlockState state) {
      return true;
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      return !this.defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
         ? Block.pushEntitiesUp(this.defaultBlockState(), Blocks.DIRT.defaultBlockState(), context.getLevel(), context.getClickedPos())
         : super.getStateForPlacement(context);
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
      if (directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)) {
         ticks.scheduleTick(pos, this, 1);
      }

      return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      this.turnToBaseBlock(null, state, level, pos);
   }

   public void turnToBaseBlock(final @Nullable Entity sourceEntity, final BlockState state, final Level level, final BlockPos pos) {
      BlockState newState = pushEntitiesUp(state, this.baseBlock.defaultBlockState(), level, pos);
      level.setBlockAndUpdate(pos, newState);
      level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
   }

   @Override
   protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
      BlockState aboveState = level.getBlockState(pos.above());
      return !aboveState.isSolid() || aboveState.getBlock() instanceof FenceGateBlock;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }
}
