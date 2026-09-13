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

   public PathNavigation(Mob p_26515_, Level p_26516_) {
      this.mob = p_26515_;
      this.level = p_26516_;
      this.pathFinder = this.createPathFinder(Mth.floor(p_26515_.getAttributeBaseValue(Attributes.FOLLOW_RANGE) * 16.0));
      if (p_26516_ instanceof ServerLevel serverlevel) {
         ServerDebugSubscribers serverdebugsubscribers = serverlevel.getServer().debugSubscribers();
         this.pathFinder.setCaptureDebug(() -> serverdebugsubscribers.hasAnySubscriberFor(DebugSubscriptions.ENTITY_PATHS));
      }
   }

   public void updatePathfinderMaxVisitedNodes() {
      int i = Mth.floor(this.getMaxPathLength() * 16.0F);
      this.pathFinder.setMaxVisitedNodes(i);
   }

   public void setRequiredPathLength(float p_370213_) {
      this.requiredPathLength = p_370213_;
      this.updatePathfinderMaxVisitedNodes();
   }

   private float getMaxPathLength() {
      return Math.max((float)this.mob.getAttributeValue(Attributes.FOLLOW_RANGE), this.requiredPathLength);
   }

   public void resetMaxVisitedNodesMultiplier() {
      this.maxVisitedNodesMultiplier = 1.0F;
   }

   public void setMaxVisitedNodesMultiplier(float p_26530_) {
      this.maxVisitedNodesMultiplier = p_26530_;
   }

   public @Nullable BlockPos getTargetPos() {
      return this.targetPos;
   }

   protected abstract PathFinder createPathFinder(int var1);

   public void setSpeedModifier(double p_26518_) {
      this.speedModifier = p_26518_;
   }

   public void recomputePath() {
      if (this.level.getGameTime() - this.timeLastRecompute > 20L) {
         if (this.targetPos != null) {
            this.path = null;
            this.path = this.createPath(this.targetPos, this.reachRange);
            this.timeLastRecompute = this.level.getGameTime();
            this.hasDelayedRecomputation = false;
         }
      } else {
         this.hasDelayedRecomputation = true;
      }
   }

   public final @Nullable Path createPath(double p_26525_, double p_26526_, double p_26527_, int p_26528_) {
      return this.createPath(BlockPos.containing(p_26525_, p_26526_, p_26527_), p_26528_);
   }

   public @Nullable Path createPath(Stream<BlockPos> p_26557_, int p_26558_) {
      return this.createPath(p_26557_.collect(Collectors.toSet()), 8, false, p_26558_);
   }

   public @Nullable Path createPath(Set<BlockPos> p_26549_, int p_26550_) {
      return this.createPath(p_26549_, 8, false, p_26550_);
   }

   public @Nullable Path createPath(BlockPos p_26546_, int p_26547_) {
      return this.createPath(ImmutableSet.of(p_26546_), 8, false, p_26547_);
   }

   public @Nullable Path createPath(BlockPos p_148219_, int p_148220_, int p_148221_) {
      return this.createPath(ImmutableSet.of(p_148219_), 8, false, p_148220_, p_148221_);
   }

   public @Nullable Path createPath(Entity p_26534_, int p_26535_) {
      return this.createPath(ImmutableSet.of(p_26534_.blockPosition()), 16, true, p_26535_);
   }

   protected @Nullable Path createPath(Set<BlockPos> p_26552_, int p_26553_, boolean p_26554_, int p_26555_) {
      return this.createPath(p_26552_, p_26553_, p_26554_, p_26555_, this.getMaxPathLength());
   }

   protected @Nullable Path createPath(Set<BlockPos> p_148223_, int p_148224_, boolean p_148225_, int p_148226_, float p_148227_) {
      if (p_148223_.isEmpty()) {
         return null;
      }

      if (this.mob.getY() < this.level.getMinY()) {
         return null;
      }

      if (!this.canUpdatePath()) {
         return null;
      }

      if (this.path != null && !this.path.isDone() && p_148223_.contains(this.targetPos)) {
         return this.path;
      }

      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("pathfind");
      BlockPos blockpos = p_148225_ ? this.mob.blockPosition().above() : this.mob.blockPosition();
      int i = (int)(p_148227_ + p_148224_);
      PathNavigationRegion pathnavigationregion = new PathNavigationRegion(this.level, blockpos.offset(-i, -i, -i), blockpos.offset(i, i, i));
      Path path = this.pathFinder.findPath(pathnavigationregion, this.mob, p_148223_, p_148227_, p_148226_, this.maxVisitedNodesMultiplier);
      profilerfiller.pop();
      if (path != null && path.getTarget() != null) {
         this.targetPos = path.getTarget();
         this.reachRange = p_148226_;
         this.resetStuckTimeout();
      }

      return path;
   }

   public boolean moveTo(double p_26520_, double p_26521_, double p_26522_, double p_26523_) {
      return this.moveTo(this.createPath(p_26520_, p_26521_, p_26522_, 1), p_26523_);
   }

   public boolean moveTo(double p_330495_, double p_329397_, double p_335206_, int p_329667_, double p_331294_) {
      return this.moveTo(this.createPath(p_330495_, p_329397_, p_335206_, p_329667_), p_331294_);
   }

   public boolean moveTo(Entity p_26532_, double p_26533_) {
      Path path = this.createPath(p_26532_, 1);
      return path != null && this.moveTo(path, p_26533_);
   }

   public boolean moveTo(@Nullable Path p_26537_, double p_26538_) {
      if (p_26537_ == null) {
         this.path = null;
         return false;
      }

      if (!p_26537_.sameAs(this.path)) {
         this.path = p_26537_;
      }

      if (this.isDone()) {
         return false;
      }

      this.trimPath();
      if (this.path.getNodeCount() <= 0) {
         return false;
      }

      this.speedModifier = p_26538_;
      Vec3 vec3 = this.getTempMobPos();
      this.lastStuckCheck = this.tick;
      this.lastStuckCheckPos = vec3;
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
            Vec3 vec3 = this.getTempMobPos();
            Vec3 vec31 = this.path.getNextEntityPos(this.mob);
            if (vec3.y > vec31.y && !this.mob.onGround() && Mth.floor(vec3.x) == Mth.floor(vec31.x) && Mth.floor(vec3.z) == Mth.floor(vec31.z)) {
               this.path.advance();
            }
         }

         if (!this.isDone()) {
            Vec3 vec32 = this.path.getNextEntityPos(this.mob);
            this.mob.getMoveControl().setWantedPosition(vec32.x, this.getGroundY(vec32), vec32.z, this.speedModifier);
         }
      }
   }

   protected double getGroundY(Vec3 p_186132_) {
      BlockPos blockpos = BlockPos.containing(p_186132_);
      return this.level.getBlockState(blockpos.below()).isAir() ? p_186132_.y : WalkNodeEvaluator.getFloorLevel(this.level, blockpos);
   }

   protected void followThePath() {
      Vec3 vec3 = this.getTempMobPos();
      this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75F ? this.mob.getBbWidth() / 2.0F : 0.75F - this.mob.getBbWidth() / 2.0F;
      Vec3i vec3i = this.path.getNextNodePos();
      double d0 = Math.abs(this.mob.getX() - (vec3i.getX() + 0.5));
      double d1 = Math.abs(this.mob.getY() - vec3i.getY());
      double d2 = Math.abs(this.mob.getZ() - (vec3i.getZ() + 0.5));
      boolean flag = d0 < this.maxDistanceToWaypoint && d2 < this.maxDistanceToWaypoint && d1 < 1.0;
      if (flag || this.canCutCorner(this.path.getNextNode().type) && this.shouldTargetNextNodeInDirection(vec3)) {
         this.path.advance();
      }

      this.doStuckDetection(vec3);
   }

   private boolean shouldTargetNextNodeInDirection(Vec3 p_26560_) {
      if (this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount()) {
         return false;
      }

      Vec3 vec3 = Vec3.atBottomCenterOf(this.path.getNextNodePos());
      if (!p_26560_.closerThan(vec3, 2.0)) {
         return false;
      }

      if (this.canMoveDirectly(p_26560_, this.path.getNextEntityPos(this.mob))) {
         return true;
      }

      Vec3 vec31 = Vec3.atBottomCenterOf(this.path.getNodePos(this.path.getNextNodeIndex() + 1));
      Vec3 vec32 = vec3.subtract(p_26560_);
      Vec3 vec33 = vec31.subtract(p_26560_);
      double d0 = vec32.lengthSqr();
      double d1 = vec33.lengthSqr();
      boolean flag = d1 < d0;
      boolean flag1 = d0 < 0.5;
      if (!flag && !flag1) {
         return false;
      }

      Vec3 vec34 = vec32.normalize();
      Vec3 vec35 = vec33.normalize();
      return vec35.dot(vec34) < 0.0;
   }

   protected void doStuckDetection(Vec3 p_26539_) {
      if (this.tick - this.lastStuckCheck > 100) {
         float f = this.mob.getSpeed() >= 1.0F ? this.mob.getSpeed() : this.mob.getSpeed() * this.mob.getSpeed();
         float f1 = f * 100.0F * 0.25F;
         if (p_26539_.distanceToSqr(this.lastStuckCheckPos) < f1 * f1) {
            this.isStuck = true;
            this.stop();
         } else {
            this.isStuck = false;
         }

         this.lastStuckCheck = this.tick;
         this.lastStuckCheckPos = p_26539_;
      }

      if (this.path != null && !this.path.isDone()) {
         Vec3i vec3i = this.path.getNextNodePos();
         long i = this.level.getGameTime();
         if (vec3i.equals(this.timeoutCachedNode)) {
            this.timeoutTimer = this.timeoutTimer + (i - this.lastTimeoutCheck);
         } else {
            this.timeoutCachedNode = vec3i;
            double d0 = p_26539_.distanceTo(Vec3.atBottomCenterOf(this.timeoutCachedNode));
            this.timeoutLimit = this.mob.getSpeed() > 0.0F ? d0 / this.mob.getSpeed() * 20.0 : 0.0;
         }

         if (this.timeoutLimit > 0.0 && this.timeoutTimer > this.timeoutLimit * 3.0) {
            this.timeoutPath();
         }

         this.lastTimeoutCheck = i;
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
            Node node1 = i + 1 < this.path.getNodeCount() ? this.path.getNode(i + 1) : null;
            BlockState blockstate = this.level.getBlockState(new BlockPos(node.x, node.y, node.z));
            if (blockstate.is(BlockTags.CAULDRONS)) {
               this.path.replaceNode(i, node.cloneAndMove(node.x, node.y + 1, node.z));
               if (node1 != null && node.y >= node1.y) {
                  this.path.replaceNode(i + 1, node.cloneAndMove(node1.x, node.y + 1, node1.z));
               }
            }
         }
      }
   }

   protected boolean canMoveDirectly(Vec3 p_186133_, Vec3 p_186134_) {
      return false;
   }

   public boolean canCutCorner(PathType p_334253_) {
      return p_334253_ != PathType.DANGER_FIRE && p_334253_ != PathType.DANGER_OTHER && p_334253_ != PathType.WALKABLE_DOOR;
   }

   protected static boolean isClearForMovementBetween(Mob p_262599_, Vec3 p_262674_, Vec3 p_262586_, boolean p_262676_) {
      Vec3 vec3 = new Vec3(p_262586_.x, p_262586_.y + p_262599_.getBbHeight() * 0.5, p_262586_.z);
      return p_262599_.level()
            .clip(new ClipContext(p_262674_, vec3, ClipContext.Block.COLLIDER, p_262676_ ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, p_262599_))
            .getType()
         == HitResult.Type.MISS;
   }

   public boolean isStableDestination(BlockPos p_26545_) {
      BlockPos blockpos = p_26545_.below();
      return this.level.getBlockState(blockpos).isSolidRender();
   }

   public NodeEvaluator getNodeEvaluator() {
      return this.nodeEvaluator;
   }

   public void setCanFloat(boolean p_26563_) {
      this.nodeEvaluator.setCanFloat(p_26563_);
   }

   public boolean canFloat() {
      return this.nodeEvaluator.canFloat();
   }

   public boolean shouldRecomputePath(BlockPos p_200904_) {
      if (this.hasDelayedRecomputation) {
         return false;
      } else if (this.path != null && !this.path.isDone() && this.path.getNodeCount() != 0) {
         Node node = this.path.getEndNode();
         Vec3 vec3 = new Vec3((node.x + this.mob.getX()) / 2.0, (node.y + this.mob.getY()) / 2.0, (node.z + this.mob.getZ()) / 2.0);
         return p_200904_.closerToCenterThan(vec3, this.path.getNodeCount() - this.path.getNextNodeIndex());
      } else {
         return false;
      }
   }

   public float getMaxDistanceToWaypoint() {
      return this.maxDistanceToWaypoint;
   }

   public boolean isStuck() {
      return this.isStuck;
   }

   public abstract boolean canNavigateGround();

   public void setCanOpenDoors(boolean p_406281_) {
      this.nodeEvaluator.setCanOpenDoors(p_406281_);
   }
}
