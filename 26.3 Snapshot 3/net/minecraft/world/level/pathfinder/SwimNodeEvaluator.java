package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public class SwimNodeEvaluator extends NodeEvaluator {
   private final boolean allowBreaching;
   private final Long2ObjectMap<PathType> pathTypesByPosCache = new Long2ObjectOpenHashMap();

   public SwimNodeEvaluator(final boolean allowBreaching) {
      this.allowBreaching = allowBreaching;
   }

   @Override
   public void prepare(final PathNavigationRegion level, final Mob entity) {
      super.prepare(level, entity);
      this.pathTypesByPosCache.clear();
   }

   @Override
   public void done() {
      super.done();
      this.pathTypesByPosCache.clear();
   }

   @Override
   public Node getStart() {
      return this.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ));
   }

   @Override
   public Target getTarget(final double x, final double y, final double z) {
      return this.getTargetNodeAt(x, y, z);
   }

   @Override
   public int getNeighbors(final Node[] neighbors, final Node pos) {
      int count = 0;
      Map<Direction, Node> nodes = Maps.newEnumMap(Direction.class);

      for (Direction direction : Direction.values()) {
         Node node = this.findAcceptedNode(pos.x + direction.getStepX(), pos.y + direction.getStepY(), pos.z + direction.getStepZ());
         nodes.put(direction, node);
         if (this.isNodeValid(node)) {
            neighbors[count++] = node;
         }
      }

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         Direction secondDirection = direction.getClockWise();
         if (hasMalus(nodes.get(direction)) && hasMalus(nodes.get(secondDirection))) {
            Node diagonalNode = this.findAcceptedNode(
               pos.x + direction.getStepX() + secondDirection.getStepX(), pos.y, pos.z + direction.getStepZ() + secondDirection.getStepZ()
            );
            if (this.isNodeValid(diagonalNode)) {
               neighbors[count++] = diagonalNode;
            }
         }
      }

      return count;
   }

   protected boolean isNodeValid(final @Nullable Node node) {
      return node != null && !node.closed;
   }

   private static boolean hasMalus(final @Nullable Node node) {
      return node != null && node.costMalus >= 0.0F;
   }

   protected @Nullable Node findAcceptedNode(final int x, final int y, final int z) {
      Node best = null;
      PathType pathType = this.getCachedBlockType(x, y, z);
      if (this.allowBreaching && pathType == PathType.BREACH || pathType == PathType.WATER) {
         float pathCost = this.mob.getPathfindingMalus(pathType);
         if (pathCost >= 0.0F) {
            best = this.getNode(x, y, z);
            best.type = pathType;
            best.costMalus = Math.max(best.costMalus, pathCost);
            if (this.currentContext.level().getFluidState(new BlockPos(x, y, z)).isEmpty()) {
               best.costMalus += 8.0F;
            }
         }
      }

      return best;
   }

   protected PathType getCachedBlockType(final int x, final int y, final int z) {
      return (PathType)this.pathTypesByPosCache.computeIfAbsent(BlockPos.asLong(x, y, z), k -> this.getPathType(this.currentContext, x, y, z));
   }

   @Override
   public PathType getPathType(final PathfindingContext context, final int x, final int y, final int z) {
      return this.getPathTypeOfMob(context, x, y, z, this.mob);
   }

   @Override
   public PathType getPathTypeOfMob(final PathfindingContext context, final int x, final int y, final int z, final Mob mob) {
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int xx = x; xx < x + this.entityWidth; xx++) {
         for (int yy = y; yy < y + this.entityHeight; yy++) {
            for (int zz = z; zz < z + this.entityDepth; zz++) {
               BlockState blockState = context.getBlockState(pos.set(xx, yy, zz));
               FluidState fluidState = blockState.getFluidState();
               BlockState belowState = context.getBlockState(pos.below());
               if (fluidState.isEmpty() && belowState.isPathfindable(PathComputationType.WATER) && blockState.isAir()) {
                  return PathType.BREACH;
               }

               if (!fluidState.is(FluidTags.WATER)) {
                  return PathType.BLOCKED;
               }
            }
         }
      }

      BlockState blockState = context.getBlockState(pos);
      return blockState.isPathfindable(PathComputationType.WATER) ? PathType.WATER : PathType.BLOCKED;
   }
}
