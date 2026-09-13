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

   public SwimNodeEvaluator(boolean p_77457_) {
      this.allowBreaching = p_77457_;
   }

   @Override
   public void prepare(PathNavigationRegion p_192959_, Mob p_192960_) {
      super.prepare(p_192959_, p_192960_);
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
   public Target getTarget(double p_331212_, double p_329065_, double p_336263_) {
      return this.getTargetNodeAt(p_331212_, p_329065_, p_336263_);
   }

   @Override
   public int getNeighbors(Node[] p_77483_, Node p_77484_) {
      int i = 0;
      Map<Direction, Node> map = Maps.newEnumMap(Direction.class);

      for (Direction direction : Direction.values()) {
         Node node = this.findAcceptedNode(p_77484_.x + direction.getStepX(), p_77484_.y + direction.getStepY(), p_77484_.z + direction.getStepZ());
         map.put(direction, node);
         if (this.isNodeValid(node)) {
            p_77483_[i++] = node;
         }
      }

      for (Direction direction1 : Direction.Plane.HORIZONTAL) {
         Direction direction2 = direction1.getClockWise();
         if (hasMalus(map.get(direction1)) && hasMalus(map.get(direction2))) {
            Node node1 = this.findAcceptedNode(
               p_77484_.x + direction1.getStepX() + direction2.getStepX(), p_77484_.y, p_77484_.z + direction1.getStepZ() + direction2.getStepZ()
            );
            if (this.isNodeValid(node1)) {
               p_77483_[i++] = node1;
            }
         }
      }

      return i;
   }

   protected boolean isNodeValid(@Nullable Node p_192962_) {
      return p_192962_ != null && !p_192962_.closed;
   }

   private static boolean hasMalus(@Nullable Node p_328144_) {
      return p_328144_ != null && p_328144_.costMalus >= 0.0F;
   }

   protected @Nullable Node findAcceptedNode(int p_263032_, int p_263066_, int p_263105_) {
      Node node = null;
      PathType pathtype = this.getCachedBlockType(p_263032_, p_263066_, p_263105_);
      if (this.allowBreaching && pathtype == PathType.BREACH || pathtype == PathType.WATER) {
         float f = this.mob.getPathfindingMalus(pathtype);
         if (f >= 0.0F) {
            node = this.getNode(p_263032_, p_263066_, p_263105_);
            node.type = pathtype;
            node.costMalus = Math.max(node.costMalus, f);
            if (this.currentContext.level().getFluidState(new BlockPos(p_263032_, p_263066_, p_263105_)).isEmpty()) {
               node.costMalus += 8.0F;
            }
         }
      }

      return node;
   }

   protected PathType getCachedBlockType(int p_192968_, int p_192969_, int p_192970_) {
      return (PathType)this.pathTypesByPosCache
         .computeIfAbsent(BlockPos.asLong(p_192968_, p_192969_, p_192970_), p_327515_ -> this.getPathType(this.currentContext, p_192968_, p_192969_, p_192970_));
   }

   @Override
   public PathType getPathType(PathfindingContext p_333668_, int p_333001_, int p_328513_, int p_333109_) {
      return this.getPathTypeOfMob(p_333668_, p_333001_, p_328513_, p_333109_, this.mob);
   }

   @Override
   public PathType getPathTypeOfMob(PathfindingContext p_327815_, int p_334955_, int p_333227_, int p_331057_, Mob p_333533_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int i = p_334955_; i < p_334955_ + this.entityWidth; i++) {
         for (int j = p_333227_; j < p_333227_ + this.entityHeight; j++) {
            for (int k = p_331057_; k < p_331057_ + this.entityDepth; k++) {
               BlockState blockstate = p_327815_.getBlockState(blockpos$mutableblockpos.set(i, j, k));
               FluidState fluidstate = blockstate.getFluidState();
               if (fluidstate.isEmpty() && blockstate.isPathfindable(PathComputationType.WATER) && blockstate.isAir()) {
                  return PathType.BREACH;
               }

               if (!fluidstate.is(FluidTags.WATER)) {
                  return PathType.BLOCKED;
               }
            }
         }
      }

      BlockState blockstate1 = p_327815_.getBlockState(blockpos$mutableblockpos);
      return blockstate1.isPathfindable(PathComputationType.WATER) ? PathType.WATER : PathType.BLOCKED;
   }
}
