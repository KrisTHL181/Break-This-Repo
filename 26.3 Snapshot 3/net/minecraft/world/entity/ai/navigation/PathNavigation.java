package net.minecraft.world.entity.ai.navigation;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.ServerDebugSubscribers;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class PathNavigation {
   private static final int MAX_TIME_RECOMPUTE = 20;
   private static final int STUCK_CHECK_INTERVAL = 100;
   private static final float STUCK_THRESHOLD_DISTANCE_FACTOR = 0.25F;
   protected final Mob mob;
   protected final Level level;
   protected @Nullable Path path;
   protected double speedModifier;
   protected int tick;
   protected int lastStuckCheck;
   protected Vec3 lastStuckCheckPos = Vec3.ZERO;
   protected Vec3i timeoutCachedNode = Vec3i.ZERO;
   protected long timeoutTimer;
   protected long lastTimeoutCheck;
   protected double timeoutLimit;
   protected float maxDistanceToWaypoint = 0.5F;
   protected boolean hasDelayedRecomputation;
   protected long timeLastRecompute;
   protected NodeEvaluator nodeEvaluator;
   private @Nullable BlockPos targetPos;
   private int reachRange;
   private float maxVisitedNodesMultiplier = 1.0F;
   private final PathFinder pathFinder;
   private boolean isStuck;
   private float requiredPathLength = 16.0F;

   public PathNavigation(final Mob mob, final Level level) {
      this.mob = mob;
      this.level = level;
      this.pathFinder = this.createPathFinder(Mth.floor(mob.getAttributeBaseValue(Attributes.FOLLOW_RANGE) * 16.0));
      if (level instanceof ServerLevel serverLevel) {
         ServerDebugSubscribers subscribers = serverLevel.getServer().debugSubscribers();
         this.pathFinder.setCaptureDebug(() -> subscribers.hasAnySubscriberFor(DebugSubscriptions.ENTITY_PATHS));
      }
   }

   public void updatePathfinderMaxVisitedNodes() {
      int maxVisitedNodes = Mth.floor(this.getMaxPathLength() * 16.0F);
      this.pathFinder.setMaxVisitedNodes(maxVisitedNodes);
   }

   public void setRequiredPathLength(final float length) {
      this.requiredPathLength = length;
      this.updatePathfinderMaxVisitedNodes();
   }

   private float getMaxPathLength() {
      return Math.max((float)this.mob.getAttributeValue(Attributes.FOLLOW_RANGE), this.requiredPathLength);
   }

   public void resetMaxVisitedNodesMultiplier() {
      this.maxVisitedNodesMultiplier = 1.0F;
   }

   public void setMaxVisitedNodesMultiplier(final float maxVisitedNodesMultiplier) {
      this.maxVisitedNodesMultiplier = maxVisitedNodesMultiplier;
   }

   public @Nullable BlockPos getTargetPos() {
      return this.targetPos;
   }

   protected abstract PathFinder createPathFinder(final int maxVisitedNodes);

   public void setSpeedModifier(final double speedModifier) {
      this.speedModifier = speedModifier;
   }

   public void recomputePath() {
      if (this.level.getGameTime() - this.timeLastRecompute <= 20L || !this.canUpdatePath()) {
         this.hasDelayedRecomputation = true;
      } else if (this.targetPos != null) {
         this.path = null;
         this.path = this.createPath(this.targetPos, this.reachRange);
         this.timeLastRecompute = this.level.getGameTime();
         this.hasDelayedRecomputation = false;
      }
   }

   public final @Nullable Path createPath(final double x, final double y, final double z, final int reachRange) {
      return this.createPath(BlockPos.containing(x, y, z), reachRange);
   }

   public @Nullable Path createPath(final Stream<BlockPos> positions, final int reachRange) {
      return this.createPath(positions.collect(Collectors.toSet()), 8, false, reachRange);
   }

   public @Nullable Path createPath(final Set<BlockPos> positions, final int reachRange) {
      return this.createPath(positions, 8, false, reachRange);
   }

   public @Nullable Path createPath(final BlockPos pos, final int reachRange) {
      return this.createPath(ImmutableSet.of(pos), 8, false, reachRange);
   }

   public @Nullable Path createPath(final BlockPos pos, final int reachRange, final int maxPathLength) {
      return this.createPath(ImmutableSet.of(pos), 8, false, reachRange, maxPathLength);
   }

   public @Nullable Path createPath(final Entity target, final int reachRange) {
      return this.createPath(ImmutableSet.of(target.blockPosition()), 16, true, reachRange);
   }

   protected @Nullable Path createPath(final Set<BlockPos> targets, final int radiusOffset, final boolean above, final int reachRange) {
      return this.createPath(targets, radiusOffset, above, reachRange, this.getMaxPathLength());
   }

   protected @Nullable Path createPath(
      final Set<BlockPos> targets, final int radiusOffset, final boolean above, final int reachRange, final float maxPathLength
   ) {
      if (targets.isEmpty()) {
         return null;
      }

      if (this.mob.getY() < this.level.getMinY()) {
         return null;
      }

      if (!this.canUpdatePath()) {
         return null;
      }

      if (this.path != null && !this.path.isDone() && targets.contains(this.targetPos)) {
         return this.path;
      }

      ProfilerFiller profiler = Profiler.get();
      profiler.push("pathfind");
      BlockPos fromPos = above ? this.mob.blockPosition().above() : this.mob.blockPosition();
      int radius = (int)(maxPathLength + radiusOffset);
      PathNavigationRegion region = new PathNavigationRegion(this.level, fromPos.offset(-radius, -radius, -radius), fromPos.offset(radius, radius, radius));
      Path path = this.pathFinder.findPath(region, this.mob, targets, maxPathLength, reachRange, this.maxVisitedNodesMultiplier);
      profiler.pop();
      if (path != null && path.getTarget() != null) {
         this.targetPos = path.getTarget();
         this.reachRange = reachRange;
         this.resetStuckTimeout();
      }

      return path;
   }

   public boolean moveTo(final double x, final double y, final double z, final double speedModifier) {
      return this.moveTo(this.createPath(x, y, z, 1), speedModifier);
   }

   public boolean moveTo(final double x, final double y, final double z, final int reachRange, final double speedModifier) {
      return this.moveTo(this.createPath(x, y, z, reachRange), speedModifier);
   }

   public boolean moveTo(final Entity target, final double speedModifier) {
      Path newPath = this.createPath(target, 1);
      return newPath != null && this.moveTo(newPath, speedModifier);
   }

   public boolean moveTo(final @Nullable Path newPath, final double speedModifier) {
      if (newPath == null) {
         this.path = null;
         return false;
      }

      if (!newPath.sameAs(this.path)) {
         this.path = newPath;
      }

      if (this.isDone()) {
         return false;
      }

      this.trimPath();
      if (this.path.getNodeCount() <= 0) {
         return false;
      }

      this.speedModifier = speedModifier;
      Vec3 mobPos = this.getTempMobPos();
      this.lastStuckCheck = this.tick;
      this.lastStuckCheckPos = mobPos;
      return true;
   }

   public @Nullable Path getPath() {
      return this.path;
   }

   public void tick() {
      this.tick++;
      if (this.hasDelayedRecomputation) {
         this.recomputePath();
      }

      if (!this.isDone()) {
         if (this.canUpdatePath()) {
            this.followThePath();
         } else if (this.path != null && !this.path.isDone()) {
            Vec3 mobPos = this.getTempMobPos();
            Vec3 pos = this.path.getNextEntityPos(this.mob);
            if (mobPos.y > pos.y && !this.mob.onGround() && Mth.floor(mobPos.x) == Mth.floor(pos.x) && Mth.floor(mobPos.z) == Mth.floor(pos.z)) {
               this.path.advance();
            }
         }

         if (!this.isDone()) {
            Vec3 target = this.path.getNextEntityPos(this.mob);
            this.mob.getMoveControl().setWantedPosition(target.x, this.getGroundY(target), target.z, this.speedModifier);
         }
      }
   }

   protected double getGroundY(final Vec3 target) {
      BlockPos blockPos = BlockPos.containing(target);
      return this.level.getBlockState(blockPos.below()).isAir() ? target.y : WalkNodeEvaluator.getFloorLevel(this.level, blockPos);
   }

   protected void followThePath() {
      Vec3 mobPos = this.getTempMobPos();
      this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75F ? this.mob.getBbWidth() / 2.0F : 0.75F - this.mob.getBbWidth() / 2.0F;
      Vec3i currentNodePos = this.path.getNextNodePos();
      double xDistance = Math.abs(this.mob.getX() - (currentNodePos.getX() + 0.5));
      double yDistance = Math.abs(this.mob.getY() - currentNodePos.getY());
      double zDistance = Math.abs(this.mob.getZ() - (currentNodePos.getZ() + 0.5));
      boolean isCloseEnoughToCurrentNode = xDistance < this.maxDistanceToWaypoint
         && zDistance < this.maxDistanceToWaypoint
         && yDistance < this.getMaxVerticalDistanceToWaypoint();
      if (isCloseEnoughToCurrentNode || this.canCutCorner(this.path.getNextNode().type) && this.shouldTargetNextNodeInDirection(mobPos)) {
         this.path.advance();
      }

      this.doStuckDetection(mobPos);
   }

   private boolean shouldTargetNextNodeInDirection(final Vec3 mobPosition) {
      if (this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount()) {
         return false;
      }

      Vec3 currentNode = Vec3.atBottomCenterOf(this.path.getNextNodePos());
      if (!mobPosition.closerThan(currentNode, 2.0)) {
         return false;
      }

      if (this.canMoveDirectly(mobPosition, this.path.getNextEntityPos(this.mob))) {
         return true;
      }

      Vec3 nextNode = Vec3.atBottomCenterOf(this.path.getNodePos(this.path.getNextNodeIndex() + 1));
      Vec3 mobToCurrent = currentNode.subtract(mobPosition);
      Vec3 mobToNext = nextNode.subtract(mobPosition);
      double mobToCurrentSqr = mobToCurrent.lengthSqr();
      double mobToNextSqr = mobToNext.lengthSqr();
      boolean closerToNextThanCurrent = mobToNextSqr < mobToCurrentSqr;
      boolean withinCurrentBlock = mobToCurrentSqr < 0.5;
      if (!closerToNextThanCurrent && !withinCurrentBlock) {
         return false;
      }

      Vec3 mobDirection = mobToCurrent.normalize();
      Vec3 pathDirection = mobToNext.normalize();
      return pathDirection.dot(mobDirection) < 0.0;
   }

   protected void doStuckDetection(final Vec3 mobPos) {
      if (this.tick - this.lastStuckCheck > 100) {
         float effectiveSpeed = this.mob.getSpeed() >= 1.0F ? this.mob.getSpeed() : this.mob.getSpeed() * this.mob.getSpeed();
         float thresholdDistance = effectiveSpeed * 100.0F * 0.25F;
         if (mobPos.distanceToSqr(this.lastStuckCheckPos) < thresholdDistance * thresholdDistance) {
            this.isStuck = true;
            this.stop();
         } else {
            this.isStuck = false;
         }

         this.lastStuckCheck = this.tick;
         this.lastStuckCheckPos = mobPos;
      }

      if (this.path != null && !this.path.isDone()) {
         Vec3i pos = this.path.getNextNodePos();
         long time = this.level.getGameTime();
         if (pos.equals(this.timeoutCachedNode)) {
            this.timeoutTimer = this.timeoutTimer + (time - this.lastTimeoutCheck);
         } else {
            this.timeoutCachedNode = pos;
            double distToNode = mobPos.distanceTo(Vec3.atBottomCenterOf(this.timeoutCachedNode));
            this.timeoutLimit = this.mob.getSpeed() > 0.0F ? distToNode / this.mob.getSpeed() * 20.0 : 0.0;
         }

         if (this.timeoutLimit > 0.0 && this.timeoutTimer > this.timeoutLimit * 3.0) {
            this.timeoutPath();
         }

         this.lastTimeoutCheck = time;
      }
   }

   private void timeoutPath() {
      this.resetStuckTimeout();
      this.stop();
   }

   private void resetStuckTimeout() {
      this.timeoutCachedNode = Vec3i.ZERO;
      this.timeoutTimer = 0L;
      this.timeoutLimit = 0.0;
      this.isStuck = false;
   }

   public boolean isDone() {
      return this.path == null || this.path.isDone();
   }

   public boolean isInProgress() {
      return !this.isDone();
   }

   public void stop() {
      this.path = null;
   }

   protected abstract Vec3 getTempMobPos();

   protected abstract boolean canUpdatePath();

   protected void trimPath() {
      if (this.path != null) {
         for (int i = 0; i < this.path.getNodeCount(); i++) {
            Node node = this.path.getNode(i);
            Node nextNode = i + 1 < this.path.getNodeCount() ? this.path.getNode(i + 1) : null;
            BlockState state = this.level.getBlockState(new BlockPos(node.x, node.y, node.z));
            if (state.is(BlockTags.CAULDRONS)) {
               this.path.replaceNode(i, node.cloneAndMove(node.x, node.y + 1, node.z));
               if (nextNode != null && node.y >= nextNode.y) {
                  this.path.replaceNode(i + 1, node.cloneAndMove(nextNode.x, node.y + 1, nextNode.z));
               }
            }
         }
      }
   }

   protected boolean canMoveDirectly(final Vec3 startPos, final Vec3 stopPos) {
      return false;
   }

   public boolean canCutCorner(final PathType pathType) {
      return pathType != PathType.FIRE_IN_NEIGHBOR && pathType != PathType.DAMAGING_IN_NEIGHBOR && pathType != PathType.WALKABLE_DOOR;
   }

   protected static boolean isClearForMovementBetween(final Mob mob, final Vec3 startPos, final Vec3 stopPos, final boolean blockedByFluids) {
      Vec3 to = new Vec3(stopPos.x, stopPos.y + mob.getBbHeight() * 0.5, stopPos.z);
      return mob.level()
            .clip(new ClipContext(startPos, to, ClipContext.Block.COLLIDER, blockedByFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, mob))
            .getType()
         == HitResult.Type.MISS;
   }

   public boolean isStableDestination(final BlockPos pos) {
      BlockPos below = pos.below();
      return this.level.getBlockState(below).isSolidRender();
   }

   public NodeEvaluator getNodeEvaluator() {
      return this.nodeEvaluator;
   }

   public void setCanFloat(final boolean canFloat) {
      this.nodeEvaluator.setCanFloat(canFloat);
   }

   public boolean canFloat() {
      return this.nodeEvaluator.canFloat();
   }

   public boolean shouldRecomputePath(final BlockPos pos) {
      if (this.hasDelayedRecomputation) {
         return false;
      } else if (this.path != null && !this.path.isDone() && this.path.getNodeCount() != 0) {
         Node target = this.path.getEndNode();
         Vec3 middlePos = new Vec3((target.x + this.mob.getX()) / 2.0, (target.y + this.mob.getY()) / 2.0, (target.z + this.mob.getZ()) / 2.0);
         return pos.closerToCenterThan(middlePos, this.path.getNodeCount() - this.path.getNextNodeIndex());
      } else {
         return false;
      }
   }

   public float getMaxDistanceToWaypoint() {
      return this.maxDistanceToWaypoint;
   }

   public float getMaxVerticalDistanceToWaypoint() {
      return 1.0F;
   }

   public boolean isStuck() {
      return this.isStuck;
   }

   public abstract boolean canNavigateGround();

   public void setCanOpenDoors(final boolean canOpenDoors) {
      this.nodeEvaluator.setCanOpenDoors(canOpenDoors);
   }
}
