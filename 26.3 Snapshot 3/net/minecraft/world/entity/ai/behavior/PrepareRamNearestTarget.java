package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class PrepareRamNearestTarget<E extends PathfinderMob> extends Behavior<E> {
   public static final int TIME_OUT_DURATION = 160;
   private final ToIntFunction<E> getCooldownOnFail;
   private final int minRamDistance;
   private final int maxRamDistance;
   private final float walkSpeed;
   private final TargetingConditions ramTargeting;
   private final int ramPrepareTime;
   private final Function<E, SoundEvent> getPrepareRamSound;
   private Optional<Long> reachedRamPositionTimestamp = Optional.empty();
   private Optional<PrepareRamNearestTarget.RamCandidate> ramCandidate = Optional.empty();

   public PrepareRamNearestTarget(
      final ToIntFunction<E> getCooldownOnFail,
      final int minRamDistance,
      final int maxRamDistance,
      final float walkSpeed,
      final TargetingConditions ramTargeting,
      final int ramPrepareTime,
      final Function<E, SoundEvent> getPrepareRamSound
   ) {
      super(
         ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.RAM_COOLDOWN_TICKS,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.RAM_TARGET,
            MemoryStatus.VALUE_ABSENT
         ),
         160
      );
      this.getCooldownOnFail = getCooldownOnFail;
      this.minRamDistance = minRamDistance;
      this.maxRamDistance = maxRamDistance;
      this.walkSpeed = walkSpeed;
      this.ramTargeting = ramTargeting;
      this.ramPrepareTime = ramPrepareTime;
      this.getPrepareRamSound = getPrepareRamSound;
   }

   protected void start(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      Brain<?> brain = body.getBrain();
      brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .flatMap(livingEntities -> livingEntities.findClosest(entity -> this.ramTargeting.test(level, body, entity)))
         .ifPresent(livingEntity -> this.chooseRamPosition(body, livingEntity));
   }

   protected void stop(final ServerLevel level, final E body, final long timestamp) {
      Brain<?> brain = body.getBrain();
      if (!brain.hasMemoryValue(MemoryModuleType.RAM_TARGET)) {
         level.broadcastEntityEvent(body, (byte)59);
         brain.setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getCooldownOnFail.applyAsInt(body));
      }
   }

   protected boolean canStillUse(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      return this.ramCandidate.isPresent() && this.ramCandidate.get().getTarget().isAlive();
   }

   protected void tick(final ServerLevel level, final E body, final long timestamp) {
      if (!this.ramCandidate.isEmpty()) {
         body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.ramCandidate.get().getStartPosition(), this.walkSpeed, 0));
         body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.ramCandidate.get().getTarget(), true));
         boolean didTargetMove = !this.ramCandidate.get().getTarget().blockPosition().equals(this.ramCandidate.get().getTargetPosition());
         if (didTargetMove) {
            level.broadcastEntityEvent(body, (byte)59);
            body.getNavigation().stop();
            this.chooseRamPosition(body, this.ramCandidate.get().target);
         } else {
            BlockPos startRamPos = body.blockPosition();
            if (startRamPos.equals(this.ramCandidate.get().getStartPosition())) {
               level.broadcastEntityEvent(body, (byte)58);
               if (this.reachedRamPositionTimestamp.isEmpty()) {
                  this.reachedRamPositionTimestamp = Optional.of(timestamp);
               }

               if (timestamp - this.reachedRamPositionTimestamp.get() >= this.ramPrepareTime) {
                  body.getBrain().setMemory(MemoryModuleType.RAM_TARGET, this.getEdgeOfBlock(startRamPos, this.ramCandidate.get().getTargetPosition()));
                  level.playSound(null, body, this.getPrepareRamSound.apply(body), SoundSource.NEUTRAL, 1.0F, body.getVoicePitch());
                  this.ramCandidate = Optional.empty();
               }
            }
         }
      }
   }

   private Vec3 getEdgeOfBlock(final BlockPos startRamPos, final BlockPos targetPos) {
      double offsetDistance = 0.5;
      double xOffset = 0.5 * Mth.sign(targetPos.getX() - startRamPos.getX());
      double zOffset = 0.5 * Mth.sign(targetPos.getZ() - startRamPos.getZ());
      return Vec3.atBottomCenterOf(targetPos).add(xOffset, 0.0, zOffset);
   }

   private Optional<BlockPos> calculateRammingStartPosition(final PathfinderMob body, final LivingEntity ramableTarget) {
      BlockPos targetPos = ramableTarget.blockPosition();
      if (!this.isWalkableBlock(body, targetPos)) {
         return Optional.empty();
      }

      List<BlockPos> possibleRamPositions = Lists.newArrayList();
      BlockPos.MutableBlockPos walkablePosFurthestAwayFromTarget = targetPos.mutable();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         walkablePosFurthestAwayFromTarget.set(targetPos);

         for (int distance = 0; distance < this.maxRamDistance; distance++) {
            if (!this.isWalkableBlock(body, walkablePosFurthestAwayFromTarget.move(direction))) {
               walkablePosFurthestAwayFromTarget.move(direction.getOpposite());
               break;
            }
         }

         if (walkablePosFurthestAwayFromTarget.distManhattan(targetPos) >= this.minRamDistance) {
            possibleRamPositions.add(walkablePosFurthestAwayFromTarget.immutable());
         }
      }

      PathNavigation navigation = body.getNavigation();
      return possibleRamPositions.stream().sorted(Comparator.comparingDouble(body.blockPosition()::distSqr)).filter(pos -> {
         Path path = navigation.createPath(pos, 0);
         return path != null && path.canReach();
      }).findFirst();
   }

   private boolean isWalkableBlock(final PathfinderMob body, final BlockPos targetPos) {
      return body.getNavigation().isStableDestination(targetPos) && body.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(body, targetPos)) == 0.0F;
   }

   private void chooseRamPosition(final PathfinderMob body, final LivingEntity ramableTarget) {
      this.reachedRamPositionTimestamp = Optional.empty();
      this.ramCandidate = this.calculateRammingStartPosition(body, ramableTarget)
         .map(pos -> new PrepareRamNearestTarget.RamCandidate(pos, ramableTarget.blockPosition(), ramableTarget));
   }

   public static class RamCandidate {
      private final BlockPos startPosition;
      private final BlockPos targetPosition;
      private final LivingEntity target;

      public RamCandidate(final BlockPos startPosition, final BlockPos targetPosition, final LivingEntity target) {
         this.startPosition = startPosition;
         this.targetPosition = targetPosition;
         this.target = target;
      }

      public BlockPos getStartPosition() {
         return this.startPosition;
      }

      public BlockPos getTargetPosition() {
         return this.targetPosition;
      }

      public LivingEntity getTarget() {
         return this.target;
      }
   }
}
