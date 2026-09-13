package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class FlyNodeEvaluator extends WalkNodeEvaluator {
   private final Long2ObjectMap<PathType> pathTypeByPosCache = new Long2ObjectOpenHashMap();
   private static final float SMALL_MOB_SIZE = 1.0F;
   private static final float SMALL_MOB_INFLATED_START_NODE_BOUNDING_BOX = 1.1F;
   private static final int MAX_START_NODE_CANDIDATES = 10;

   @Override
   public void prepare(final PathNavigationRegion level, final Mob entity) {
      super.prepare(level, entity);
      this.pathTypeByPosCache.clear();
      entity.onPathfindingStart();
   }

   @Override
   public void done() {
      this.mob.onPathfindingDone();
      this.pathTypeByPosCache.clear();
      super.done();
   }

   @Override
   public Node getStart() {
      int startY;
      if (this.canFloat() && this.mob.isInWater()) {
         startY = this.mob.getBlockY();
         BlockPos.MutableBlockPos reusableBlockPos = new BlockPos.MutableBlockPos(this.mob.getX(), startY, this.mob.getZ());

         for (BlockState state = this.currentContext.getBlockState(reusableBlockPos);
            state.is(Blocks.WATER);
            state = this.currentContext.getBlockState(reusableBlockPos)
         ) {
            reusableBlockPos.set(this.mob.getX(), ++startY, this.mob.getZ());
         }
      } else {
         startY = Mth.floor(this.mob.getY() + 0.5);
      }

      BlockPos startPos = BlockPos.containing(this.mob.getX(), startY, this.mob.getZ());
      if (!this.canStartAt(startPos)) {
         for (BlockPos testedPosition : this.iteratePathfindingStartNodeCandidatePositions(this.mob)) {
            if (this.canStartAt(testedPosition)) {
               return super.getStartNode(testedPosition);
            }
         }
      }

      return super.getStartNode(startPos);
   }

   @Override
   protected boolean canStartAt(final BlockPos pos) {
      PathType blockPathType = this.getCachedPathType(pos.getX(), pos.getY(), pos.getZ());
      return this.mob.getPathfindingMalus(blockPathType) >= 0.0F;
   }

   @Override
   public Target getTarget(final double x, final double y, final double z) {
      return this.getTargetNodeAt(x, y, z);
   }

   @Override
   public int getNeighbors(final Node[] neighbors, final Node pos) {
      int count = 0;
      Node south = this.findAcceptedNode(pos.x, pos.y, pos.z + 1);
      if (this.isOpen(south)) {
         neighbors[count++] = south;
      }

      Node west = this.findAcceptedNode(pos.x - 1, pos.y, pos.z);
      if (this.isOpen(west)) {
         neighbors[count++] = west;
      }

      Node east = this.findAcceptedNode(pos.x + 1, pos.y, pos.z);
      if (this.isOpen(east)) {
         neighbors[count++] = east;
      }

      Node north = this.findAcceptedNode(pos.x, pos.y, pos.z - 1);
      if (this.isOpen(north)) {
         neighbors[count++] = north;
      }

      Node up = this.findAcceptedNode(pos.x, pos.y + 1, pos.z);
      if (this.isOpen(up)) {
         neighbors[count++] = up;
      }

      Node down = this.findAcceptedNode(pos.x, pos.y - 1, pos.z);
      if (this.isOpen(down)) {
         neighbors[count++] = down;
      }

      Node southUp = this.findAcceptedNode(pos.x, pos.y + 1, pos.z + 1);
      if (this.isOpen(southUp) && this.hasMalus(south) && this.hasMalus(up)) {
         neighbors[count++] = southUp;
      }

      Node westUp = this.findAcceptedNode(pos.x - 1, pos.y + 1, pos.z);
      if (this.isOpen(westUp) && this.hasMalus(west) && this.hasMalus(up)) {
         neighbors[count++] = westUp;
      }

      Node eastUp = this.findAcceptedNode(pos.x + 1, pos.y + 1, pos.z);
      if (this.isOpen(eastUp) && this.hasMalus(east) && this.hasMalus(up)) {
         neighbors[count++] = eastUp;
      }

      Node northUp = this.findAcceptedNode(pos.x, pos.y + 1, pos.z - 1);
      if (this.isOpen(northUp) && this.hasMalus(north) && this.hasMalus(up)) {
         neighbors[count++] = northUp;
      }

      Node southDown = this.findAcceptedNode(pos.x, pos.y - 1, pos.z + 1);
      if (this.isOpen(southDown) && this.hasMalus(south) && this.hasMalus(down)) {
         neighbors[count++] = southDown;
      }

      Node westDown = this.findAcceptedNode(pos.x - 1, pos.y - 1, pos.z);
      if (this.isOpen(westDown) && this.hasMalus(west) && this.hasMalus(down)) {
         neighbors[count++] = westDown;
      }

      Node eastDown = this.findAcceptedNode(pos.x + 1, pos.y - 1, pos.z);
      if (this.isOpen(eastDown) && this.hasMalus(east) && this.hasMalus(down)) {
         neighbors[count++] = eastDown;
      }

      Node northDown = this.findAcceptedNode(pos.x, pos.y - 1, pos.z - 1);
      if (this.isOpen(northDown) && this.hasMalus(north) && this.hasMalus(down)) {
         neighbors[count++] = northDown;
      }

      Node northEast = this.findAcceptedNode(pos.x + 1, pos.y, pos.z - 1);
      if (this.isOpen(northEast) && this.hasMalus(north) && this.hasMalus(east)) {
         neighbors[count++] = northEast;
      }

      Node southEast = this.findAcceptedNode(pos.x + 1, pos.y, pos.z + 1);
      if (this.isOpen(southEast) && this.hasMalus(south) && this.hasMalus(east)) {
         neighbors[count++] = southEast;
      }

      Node northWest = this.findAcceptedNode(pos.x - 1, pos.y, pos.z - 1);
      if (this.isOpen(northWest) && this.hasMalus(north) && this.hasMalus(west)) {
         neighbors[count++] = northWest;
      }

      Node southWest = this.findAcceptedNode(pos.x - 1, pos.y, pos.z + 1);
      if (this.isOpen(southWest) && this.hasMalus(south) && this.hasMalus(west)) {
         neighbors[count++] = southWest;
      }

      Node northEastUp = this.findAcceptedNode(pos.x + 1, pos.y + 1, pos.z - 1);
      if (this.isOpen(northEastUp)
         && this.hasMalus(northEast)
         && this.hasMalus(north)
         && this.hasMalus(east)
         && this.hasMalus(up)
         && this.hasMalus(northUp)
         && this.hasMalus(eastUp)) {
         neighbors[count++] = northEastUp;
      }

      Node southEastUp = this.findAcceptedNode(pos.x + 1, pos.y + 1, pos.z + 1);
      if (this.isOpen(southEastUp)
         && this.hasMalus(southEast)
         && this.hasMalus(south)
         && this.hasMalus(east)
         && this.hasMalus(up)
         && this.hasMalus(southUp)
         && this.hasMalus(eastUp)) {
         neighbors[count++] = southEastUp;
      }

      Node northWestUp = this.findAcceptedNode(pos.x - 1, pos.y + 1, pos.z - 1);
      if (this.isOpen(northWestUp)
         && this.hasMalus(northWest)
         && this.hasMalus(north)
         && this.hasMalus(west)
         && this.hasMalus(up)
         && this.hasMalus(northUp)
         && this.hasMalus(westUp)) {
         neighbors[count++] = northWestUp;
      }

      Node southWestUp = this.findAcceptedNode(pos.x - 1, pos.y + 1, pos.z + 1);
      if (this.isOpen(southWestUp)
         && this.hasMalus(southWest)
         && this.hasMalus(south)
         && this.hasMalus(west)
         && this.hasMalus(up)
         && this.hasMalus(southUp)
         && this.hasMalus(westUp)) {
         neighbors[count++] = southWestUp;
      }

      Node northEastDown = this.findAcceptedNode(pos.x + 1, pos.y - 1, pos.z - 1);
      if (this.isOpen(northEastDown)
         && this.hasMalus(northEast)
         && this.hasMalus(north)
         && this.hasMalus(east)
         && this.hasMalus(down)
         && this.hasMalus(northDown)
         && this.hasMalus(eastDown)) {
         neighbors[count++] = northEastDown;
      }

      Node southEastDown = this.findAcceptedNode(pos.x + 1, pos.y - 1, pos.z + 1);
      if (this.isOpen(southEastDown)
         && this.hasMalus(southEast)
         && this.hasMalus(south)
         && this.hasMalus(east)
         && this.hasMalus(down)
         && this.hasMalus(southDown)
         && this.hasMalus(eastDown)) {
         neighbors[count++] = southEastDown;
      }

      Node northWestDown = this.findAcceptedNode(pos.x - 1, pos.y - 1, pos.z - 1);
      if (this.isOpen(northWestDown)
         && this.hasMalus(northWest)
         && this.hasMalus(north)
         && this.hasMalus(west)
         && this.hasMalus(down)
         && this.hasMalus(northDown)
         && this.hasMalus(westDown)) {
         neighbors[count++] = northWestDown;
      }

      Node southWestDown = this.findAcceptedNode(pos.x - 1, pos.y - 1, pos.z + 1);
      if (this.isOpen(southWestDown)
         && this.hasMalus(southWest)
         && this.hasMalus(south)
         && this.hasMalus(west)
         && this.hasMalus(down)
         && this.hasMalus(southDown)
         && this.hasMalus(westDown)) {
         neighbors[count++] = southWestDown;
      }

      return count;
   }

   private boolean hasMalus(final @Nullable Node node) {
      return node != null && node.costMalus >= 0.0F;
   }

   private boolean isOpen(final @Nullable Node node) {
      return node != null && !node.closed;
   }

   protected @Nullable Node findAcceptedNode(final int x, final int y, final int z) {
      Node best = null;
      PathType pathType = this.getCachedPathType(x, y, z);
      float pathCost = this.mob.getPathfindingMalus(pathType);
      if (pathCost >= 0.0F) {
         best = this.getNode(x, y, z);
         best.type = pathType;
         best.costMalus = Math.max(best.costMalus, pathCost);
         if (pathType == PathType.WALKABLE) {
            best.costMalus++;
         }
      }

      return best;
   }

   @Override
   protected PathType getCachedPathType(final int x, final int y, final int z) {
      return (PathType)this.pathTypeByPosCache.computeIfAbsent(BlockPos.asLong(x, y, z), key -> this.getPathTypeOfMob(this.currentContext, x, y, z, this.mob));
   }

   @Override
   public PathType getPathType(final PathfindingContext context, final int x, final int y, final int z) {
      PathType blockPathType = context.getPathTypeFromState(x, y, z);
      if (blockPathType == PathType.OPEN && y >= context.level().getMinY() + 1) {
         BlockPos belowPos = new BlockPos(x, y - 1, z);
         PathType belowType = context.getPathTypeFromState(belowPos.getX(), belowPos.getY(), belowPos.getZ());
         if (belowType == PathType.FIRE || belowType == PathType.LAVA) {
            blockPathType = PathType.FIRE;
         } else if (belowType == PathType.DAMAGING) {
            blockPathType = PathType.DAMAGING;
         } else if (belowType == PathType.COCOA) {
            blockPathType = PathType.COCOA;
         } else if (belowType == PathType.FENCE) {
            if (!belowPos.equals(context.mobPosition())) {
               blockPathType = PathType.FENCE;
            }
         } else {
            blockPathType = belowType != PathType.WALKABLE && belowType != PathType.OPEN && belowType != PathType.WATER ? PathType.WALKABLE : PathType.OPEN;
         }
      }

      if (blockPathType == PathType.WALKABLE || blockPathType == PathType.OPEN) {
         blockPathType = checkNeighbourBlocks(context, x, y, z, blockPathType);
      }

      return blockPathType;
   }

   private Iterable<BlockPos> iteratePathfindingStartNodeCandidatePositions(final Mob mob) {
      AABB boundingBox = mob.getBoundingBox();
      boolean isSmallMob = boundingBox.getSize() < 1.0;
      if (!isSmallMob) {
         return List.of(
            BlockPos.containing(boundingBox.minX, mob.getBlockY(), boundingBox.minZ),
            BlockPos.containing(boundingBox.minX, mob.getBlockY(), boundingBox.maxZ),
            BlockPos.containing(boundingBox.maxX, mob.getBlockY(), boundingBox.minZ),
            BlockPos.containing(boundingBox.maxX, mob.getBlockY(), boundingBox.maxZ)
         );
      }

      double zPadding = Math.max(0.0, 1.1F - boundingBox.getZsize());
      double xPadding = Math.max(0.0, 1.1F - boundingBox.getXsize());
      double yPadding = Math.max(0.0, 1.1F - boundingBox.getYsize());
      AABB inflatedBoundingBox = boundingBox.inflate(xPadding, yPadding, zPadding);
      return BlockPos.randomBetweenClosed(
         mob.getRandom(),
         10,
         Mth.floor(inflatedBoundingBox.minX),
         Mth.floor(inflatedBoundingBox.minY),
         Mth.floor(inflatedBoundingBox.minZ),
         Mth.floor(inflatedBoundingBox.maxX),
         Mth.floor(inflatedBoundingBox.maxY),
         Mth.floor(inflatedBoundingBox.maxZ)
      );
   }
}
