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
   public void prepare(PathNavigationRegion p_77261_, Mob p_77262_) {
      super.prepare(p_77261_, p_77262_);
      this.pathTypeByPosCache.clear();
      p_77262_.onPathfindingStart();
   }

   @Override
   public void done() {
      this.mob.onPathfindingDone();
      this.pathTypeByPosCache.clear();
      super.done();
   }

   @Override
   public Node getStart() {
      int i;
      if (this.canFloat() && this.mob.isInWater()) {
         i = this.mob.getBlockY();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(this.mob.getX(), i, this.mob.getZ());

         for (BlockState blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos);
            blockstate.is(Blocks.WATER);
            blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos)
         ) {
            blockpos$mutableblockpos.set(this.mob.getX(), ++i, this.mob.getZ());
         }
      } else {
         i = Mth.floor(this.mob.getY() + 0.5);
      }

      BlockPos blockpos1 = BlockPos.containing(this.mob.getX(), i, this.mob.getZ());
      if (!this.canStartAt(blockpos1)) {
         for (BlockPos blockpos : this.iteratePathfindingStartNodeCandidatePositions(this.mob)) {
            if (this.canStartAt(blockpos)) {
               return super.getStartNode(blockpos);
            }
         }
      }

      return super.getStartNode(blockpos1);
   }

   @Override
   protected boolean canStartAt(BlockPos p_262645_) {
      PathType pathtype = this.getCachedPathType(p_262645_.getX(), p_262645_.getY(), p_262645_.getZ());
      return this.mob.getPathfindingMalus(pathtype) >= 0.0F;
   }

   @Override
   public Target getTarget(double p_328296_, double p_329795_, double p_334015_) {
      return this.getTargetNodeAt(p_328296_, p_329795_, p_334015_);
   }

   @Override
   public int getNeighbors(Node[] p_77266_, Node p_77267_) {
      int i = 0;
      Node node = this.findAcceptedNode(p_77267_.x, p_77267_.y, p_77267_.z + 1);
      if (this.isOpen(node)) {
         p_77266_[i++] = node;
      }

      Node node1 = this.findAcceptedNode(p_77267_.x - 1, p_77267_.y, p_77267_.z);
      if (this.isOpen(node1)) {
         p_77266_[i++] = node1;
      }

      Node node2 = this.findAcceptedNode(p_77267_.x + 1, p_77267_.y, p_77267_.z);
      if (this.isOpen(node2)) {
         p_77266_[i++] = node2;
      }

      Node node3 = this.findAcceptedNode(p_77267_.x, p_77267_.y, p_77267_.z - 1);
      if (this.isOpen(node3)) {
         p_77266_[i++] = node3;
      }

      Node node4 = this.findAcceptedNode(p_77267_.x, p_77267_.y + 1, p_77267_.z);
      if (this.isOpen(node4)) {
         p_77266_[i++] = node4;
      }

      Node node5 = this.findAcceptedNode(p_77267_.x, p_77267_.y - 1, p_77267_.z);
      if (this.isOpen(node5)) {
         p_77266_[i++] = node5;
      }

      Node node6 = this.findAcceptedNode(p_77267_.x, p_77267_.y + 1, p_77267_.z + 1);
      if (this.isOpen(node6) && this.hasMalus(node) && this.hasMalus(node4)) {
         p_77266_[i++] = node6;
      }

      Node node7 = this.findAcceptedNode(p_77267_.x - 1, p_77267_.y + 1, p_77267_.z);
      if (this.isOpen(node7) && this.hasMalus(node1) && this.hasMalus(node4)) {
         p_77266_[i++] = node7;
      }

      Node node8 = this.findAcceptedNode(p_77267_.x + 1, p_77267_.y + 1, p_77267_.z);
      if (this.isOpen(node8) && this.hasMalus(node2) && this.hasMalus(node4)) {
         p_77266_[i++] = node8;
      }

      Node node9 = this.findAcceptedNode(p_77267_.x, p_77267_.y + 1, p_77267_.z - 1);
      if (this.isOpen(node9) && this.hasMalus(node3) && this.hasMalus(node4)) {
         p_77266_[i++] = node9;
      }

      Node node10 = this.findAcceptedNode(p_77267_.x, p_77267_.y - 1, p_77267_.z + 1);
      if (this.isOpen(node10) && this.hasMalus(node) && this.hasMalus(node5)) {
         p_77266_[i++] = node10;
      }

      Node node11 = this.findAcceptedNode(p_77267_.x - 1, p_77267_.y - 1, p_77267_.z);
      if (this.isOpen(node11) && this.hasMalus(node1) && this.hasMalus(node5)) {
         p_77266_[i++] = node11;
      }

      Node node12 = this.findAcceptedNode(p_77267_.x + 1, p_77267_.y - 1, p_77267_.z);
      if (this.isOpen(node12) && this.hasMalus(node2) && this.hasMalus(node5)) {
         p_77266_[i++] = node12;
      }

      Node node13 = this.findAcceptedNode(p_77267_.x, p_77267_.y - 1, p_77267_.z - 1);
      if (this.isOpen(node13) && this.hasMalus(node3) && this.hasMalus(node5)) {
         p_77266_[i++] = node13;
      }

      Node node14 = this.findAcceptedNode(p_77267_.x + 1, p_77267_.y, p_77267_.z - 1);
      if (this.isOpen(node14) && this.hasMalus(node3) && this.hasMalus(node2)) {
         p_77266_[i++] = node14;
      }

      Node node15 = this.findAcceptedNode(p_77267_.x + 1, p_77267_.y, p_77267_.z + 1);
      if (this.isOpen(node15) && this.hasMalus(node) && this.hasMalus(node2)) {
         p_77266_[i++] = node15;
      }

      Node node16 = this.findAcceptedNode(p_77267_.x - 1, p_77267_.y, p_77267_.z - 1);
      if (this.isOpen(node16) && this.hasMalus(node3) && this.hasMalus(node1)) {
         p_77266_[i++] = node16;
      }

      Node node17 = this.findAcceptedNode(p_77267_.x - 1, p_77267_.y, p_77267_.z + 1);
      if (this.isOpen(node17) && this.hasMalus(node) && this.hasMalus(node1)) {
         p_77266_[i++] = node17;
      }

      Node node18 = this.findAcceptedNode(p_77267_.x + 1, p_77267_.y + 1, p_77267_.z - 1);
      if (this.isOpen(node18)
         && this.hasMalus(node14)
         && this.hasMalus(node3)
         && this.hasMalus(node2)
         && this.hasMalus(node4)
         && this.hasMalus(node9)
         && this.hasMalus(node8)) {
         p_77266_[i++] = node18;
      }

      Node node19 = this.findAcceptedNode(p_77267_.x + 1, p_77267_.y + 1, p_77267_.z + 1);
      if (this.isOpen(node19)
         && this.hasMalus(node15)
         && this.hasMalus(node)
         && this.hasMalus(node2)
         && this.hasMalus(node4)
         && this.hasMalus(node6)
         && this.hasMalus(node8)) {
         p_77266_[i++] = node19;
      }

      Node node20 = this.findAcceptedNode(p_77267_.x - 1, p_77267_.y + 1, p_77267_.z - 1);
      if (this.isOpen(node20)
         && this.hasMalus(node16)
         && this.hasMalus(node3)
         && this.hasMalus(node1)
         && this.hasMalus(node4)
         && this.hasMalus(node9)
         && this.hasMalus(node7)) {
         p_77266_[i++] = node20;
      }

      Node node21 = this.findAcceptedNode(p_77267_.x - 1, p_77267_.y + 1, p_77267_.z + 1);
      if (this.isOpen(node21)
         && this.hasMalus(node17)
         && this.hasMalus(node)
         && this.hasMalus(node1)
         && this.hasMalus(node4)
         && this.hasMalus(node6)
         && this.hasMalus(node7)) {
         p_77266_[i++] = node21;
      }

      Node node22 = this.findAcceptedNode(p_77267_.x + 1, p_77267_.y - 1, p_77267_.z - 1);
      if (this.isOpen(node22)
         && this.hasMalus(node14)
         && this.hasMalus(node3)
         && this.hasMalus(node2)
         && this.hasMalus(node5)
         && this.hasMalus(node13)
         && this.hasMalus(node12)) {
         p_77266_[i++] = node22;
      }

      Node node23 = this.findAcceptedNode(p_77267_.x + 1, p_77267_.y - 1, p_77267_.z + 1);
      if (this.isOpen(node23)
         && this.hasMalus(node15)
         && this.hasMalus(node)
         && this.hasMalus(node2)
         && this.hasMalus(node5)
         && this.hasMalus(node10)
         && this.hasMalus(node12)) {
         p_77266_[i++] = node23;
      }

      Node node24 = this.findAcceptedNode(p_77267_.x - 1, p_77267_.y - 1, p_77267_.z - 1);
      if (this.isOpen(node24)
         && this.hasMalus(node16)
         && this.hasMalus(node3)
         && this.hasMalus(node1)
         && this.hasMalus(node5)
         && this.hasMalus(node13)
         && this.hasMalus(node11)) {
         p_77266_[i++] = node24;
      }

      Node node25 = this.findAcceptedNode(p_77267_.x - 1, p_77267_.y - 1, p_77267_.z + 1);
      if (this.isOpen(node25)
         && this.hasMalus(node17)
         && this.hasMalus(node)
         && this.hasMalus(node1)
         && this.hasMalus(node5)
         && this.hasMalus(node10)
         && this.hasMalus(node11)) {
         p_77266_[i++] = node25;
      }

      return i;
   }

   private boolean hasMalus(@Nullable Node p_77264_) {
      return p_77264_ != null && p_77264_.costMalus >= 0.0F;
   }

   private boolean isOpen(@Nullable Node p_77270_) {
      return p_77270_ != null && !p_77270_.closed;
   }

   protected @Nullable Node findAcceptedNode(int p_262970_, int p_263018_, int p_262947_) {
      Node node = null;
      PathType pathtype = this.getCachedPathType(p_262970_, p_263018_, p_262947_);
      float f = this.mob.getPathfindingMalus(pathtype);
      if (f >= 0.0F) {
         node = this.getNode(p_262970_, p_263018_, p_262947_);
         node.type = pathtype;
         node.costMalus = Math.max(node.costMalus, f);
         if (pathtype == PathType.WALKABLE) {
            node.costMalus++;
         }
      }

      return node;
   }

   @Override
   protected PathType getCachedPathType(int p_332812_, int p_330135_, int p_330816_) {
      return (PathType)this.pathTypeByPosCache
         .computeIfAbsent(
            BlockPos.asLong(p_332812_, p_330135_, p_330816_),
            p_327510_ -> this.getPathTypeOfMob(this.currentContext, p_332812_, p_330135_, p_330816_, this.mob)
         );
   }

   @Override
   public PathType getPathType(PathfindingContext p_333778_, int p_332501_, int p_330191_, int p_335791_) {
      PathType pathtype = p_333778_.getPathTypeFromState(p_332501_, p_330191_, p_335791_);
      if (pathtype == PathType.OPEN && p_330191_ >= p_333778_.level().getMinY() + 1) {
         BlockPos blockpos = new BlockPos(p_332501_, p_330191_ - 1, p_335791_);
         PathType pathtype1 = p_333778_.getPathTypeFromState(blockpos.getX(), blockpos.getY(), blockpos.getZ());
         if (pathtype1 == PathType.DAMAGE_FIRE || pathtype1 == PathType.LAVA) {
            pathtype = PathType.DAMAGE_FIRE;
         } else if (pathtype1 == PathType.DAMAGE_OTHER) {
            pathtype = PathType.DAMAGE_OTHER;
         } else if (pathtype1 == PathType.COCOA) {
            pathtype = PathType.COCOA;
         } else if (pathtype1 == PathType.FENCE) {
            if (!blockpos.equals(p_333778_.mobPosition())) {
               pathtype = PathType.FENCE;
            }
         } else {
            pathtype = pathtype1 != PathType.WALKABLE && pathtype1 != PathType.OPEN && pathtype1 != PathType.WATER ? PathType.WALKABLE : PathType.OPEN;
         }
      }

      if (pathtype == PathType.WALKABLE || pathtype == PathType.OPEN) {
         pathtype = checkNeighbourBlocks(p_333778_, p_332501_, p_330191_, p_335791_, pathtype);
      }

      return pathtype;
   }

   private Iterable<BlockPos> iteratePathfindingStartNodeCandidatePositions(Mob p_263108_) {
      AABB aabb = p_263108_.getBoundingBox();
      boolean flag = aabb.getSize() < 1.0;
      if (!flag) {
         return List.of(
            BlockPos.containing(aabb.minX, p_263108_.getBlockY(), aabb.minZ),
            BlockPos.containing(aabb.minX, p_263108_.getBlockY(), aabb.maxZ),
            BlockPos.containing(aabb.maxX, p_263108_.getBlockY(), aabb.minZ),
            BlockPos.containing(aabb.maxX, p_263108_.getBlockY(), aabb.maxZ)
         );
      }

      double d0 = Math.max(0.0, 1.1F - aabb.getZsize());
      double d1 = Math.max(0.0, 1.1F - aabb.getXsize());
      double d2 = Math.max(0.0, 1.1F - aabb.getYsize());
      AABB aabb1 = aabb.inflate(d1, d2, d0);
      return BlockPos.randomBetweenClosed(
         p_263108_.getRandom(),
         10,
         Mth.floor(aabb1.minX),
         Mth.floor(aabb1.minY),
         Mth.floor(aabb1.minZ),
         Mth.floor(aabb1.maxX),
         Mth.floor(aabb1.maxY),
         Mth.floor(aabb1.maxZ)
      );
   }
}
