package net.minecraft.world.level.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import org.jspecify.annotations.Nullable;

public class AmphibiousNodeEvaluator extends WalkNodeEvaluator {
   private final boolean prefersShallowSwimming;
   private float oldWalkableCost;
   private float oldWaterBorderCost;

   public AmphibiousNodeEvaluator(final boolean prefersShallowSwimming) {
      this.prefersShallowSwimming = prefersShallowSwimming;
   }

   @Override
   public void prepare(final PathNavigationRegion level, final Mob entity) {
      super.prepare(level, entity);
      entity.setPathfindingMalus(PathType.WATER, 0.0F);
      this.oldWalkableCost = entity.getPathfindingMalus(PathType.WALKABLE);
      entity.setPathfindingMalus(PathType.WALKABLE, 6.0F);
      this.oldWaterBorderCost = entity.getPathfindingMalus(PathType.WATER_BORDER);
      entity.setPathfindingMalus(PathType.WATER_BORDER, 4.0F);
   }

   @Override
   public void done() {
      this.mob.setPathfindingMalus(PathType.WALKABLE, this.oldWalkableCost);
      this.mob.setPathfindingMalus(PathType.WATER_BORDER, this.oldWaterBorderCost);
      super.done();
   }

   @Override
   public Node getStart() {
      return !this.mob.isInWater()
         ? super.getStart()
         : this.getStartNode(
            new BlockPos(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ))
         );
   }

   @Override
   public Target getTarget(final double x, final double y, final double z) {
      return this.getTargetNodeAt(x, y + 0.5, z);
   }

   @Override
   public int getNeighbors(final Node[] neighbors, final Node pos) {
      int numValidNeighbors = super.getNeighbors(neighbors, pos);
      PathType blockPathTypeAbove = this.getCachedPathType(pos.x, pos.y + 1, pos.z);
      PathType blockPathTypeCurrent = this.getCachedPathType(pos.x, pos.y, pos.z);
      int jumpSize;
      if (this.mob.getPathfindingMalus(blockPathTypeAbove) >= 0.0F && blockPathTypeCurrent != PathType.STICKY_HONEY) {
         jumpSize = Mth.floor(Math.max(1.0F, this.mob.maxUpStep()));
      } else {
         jumpSize = 0;
      }

      double posHeight = this.getFloorLevel(new BlockPos(pos.x, pos.y, pos.z));
      Node upNode = this.findAcceptedNode(pos.x, pos.y + 1, pos.z, Math.max(0, jumpSize - 1), posHeight, Direction.UP, blockPathTypeCurrent);
      Node downNode = this.findAcceptedNode(pos.x, pos.y - 1, pos.z, jumpSize, posHeight, Direction.DOWN, blockPathTypeCurrent);
      if (this.isVerticalNeighborValid(upNode, pos)) {
         neighbors[numValidNeighbors++] = upNode;
      }

      if (this.isVerticalNeighborValid(downNode, pos) && blockPathTypeCurrent != PathType.TRAPDOOR) {
         neighbors[numValidNeighbors++] = downNode;
      }

      for (int i = 0; i < numValidNeighbors; i++) {
         Node neighbor = neighbors[i];
         if (neighbor.type == PathType.WATER && this.prefersShallowSwimming && neighbor.y < this.mob.level().getSeaLevel() - 10) {
            neighbor.costMalus++;
         }
      }

      return numValidNeighbors;
   }

   private boolean isVerticalNeighborValid(final @Nullable Node verticalNode, final Node pos) {
      return this.isNeighborValid(verticalNode, pos) && verticalNode.type == PathType.WATER;
   }

   @Override
   protected boolean isAmphibious() {
      return true;
   }

   @Override
   public PathType getPathType(final PathfindingContext context, final int x, final int y, final int z) {
      PathType blockPathType = context.getPathTypeFromState(x, y, z);
      if (blockPathType == PathType.WATER) {
         BlockPos.MutableBlockPos reusablePos = new BlockPos.MutableBlockPos();

         for (Direction direction : Direction.values()) {
            reusablePos.set(x, y, z).move(direction);
            PathType pathType = context.getPathTypeFromState(reusablePos.getX(), reusablePos.getY(), reusablePos.getZ());
            if (pathType == PathType.BLOCKED) {
               return PathType.WATER_BORDER;
            }
         }

         return PathType.WATER;
      } else {
         return super.getPathType(context, x, y, z);
      }
   }
}
