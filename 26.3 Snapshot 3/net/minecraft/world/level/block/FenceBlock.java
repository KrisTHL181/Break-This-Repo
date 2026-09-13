package net.minecraft.world.level.block;

import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FenceBlock extends CrossCollisionBlock {
   private final Function<BlockState, VoxelShape> occlusionShapes;

   public FenceBlock(final BlockBehaviour.Properties properties) {
      super(4.0F, 16.0F, 4.0F, 16.0F, 24.0F, properties);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
      );
      this.occlusionShapes = this.makeShapes(4.0F, 16.0F, 2.0F, 6.0F, 15.0F);
   }

   @Override
   protected VoxelShape getOcclusionShape(final BlockState state) {
      return this.occlusionShapes.apply(state);
   }

   @Override
   protected VoxelShape getVisualShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return this.getShape(state, level, pos, context);
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   public boolean connectsTo(final BlockState state, final boolean faceSolid, final Direction direction) {
      Block block = state.getBlock();
      boolean sameFence = this.isSameFence(state);
      boolean gate = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, direction);
      return !isExceptionForConnection(state) && faceSolid || sameFence || gate;
   }

   private boolean isSameFence(final BlockState state) {
      return state.is(BlockTags.FENCES) && state.is(BlockTags.WOODEN_FENCES) == this.defaultBlockState().is(BlockTags.WOODEN_FENCES);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      return !level.isClientSide() ? LeadItem.bindPlayerMobs(player, level, pos) : InteractionResult.PASS;
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      BlockGetter level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      BlockPos north = pos.north();
      BlockPos east = pos.east();
      BlockPos south = pos.south();
      BlockPos west = pos.west();
      BlockState northState = level.getBlockState(north);
      BlockState eastState = level.getBlockState(east);
      BlockState southState = level.getBlockState(south);
      BlockState westState = level.getBlockState(west);
      return super.getStateForPlacement(context)
         .setValue(NORTH, this.connectsTo(northState, northState.isFaceSturdy(level, north, Direction.SOUTH), Direction.SOUTH))
         .setValue(EAST, this.connectsTo(eastState, eastState.isFaceSturdy(level, east, Direction.WEST), Direction.WEST))
         .setValue(SOUTH, this.connectsTo(southState, southState.isFaceSturdy(level, south, Direction.NORTH), Direction.NORTH))
         .setValue(WEST, this.connectsTo(westState, westState.isFaceSturdy(level, west, Direction.EAST), Direction.EAST))
         .setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
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

      return directionToNeighbour.getAxis().isHorizontal()
         ? state.setValue(
            PROPERTY_BY_DIRECTION.get(directionToNeighbour),
            this.connectsTo(
               neighbourState, neighbourState.isFaceSturdy(level, neighbourPos, directionToNeighbour.getOpposite()), directionToNeighbour.getOpposite()
            )
         )
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}
