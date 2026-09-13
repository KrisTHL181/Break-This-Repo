package net.minecraft.world.entity.animal.frog;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.Path;

public class ShootTongue extends Behavior<Frog> {
   public static final int TIME_OUT_DURATION = 100;
   public static final int CATCH_ANIMATION_DURATION = 6;
   public static final int TONGUE_ANIMATION_DURATION = 10;
   private static final float EATING_DISTANCE = 1.75F;
   private static final float EATING_MOVEMENT_FACTOR = 0.75F;
   public static final int UNREACHABLE_TONGUE_TARGETS_COOLDOWN_DURATION = 100;
   public static final int MAX_UNREACHBLE_TONGUE_TARGETS_IN_MEMORY = 5;
   private int eatAnimationTimer;
   private int calculatePathCounter;
   private final SoundEvent tongueSound;
   private final SoundEvent eatSound;
   private ShootTongue.State state = ShootTongue.State.DONE;

   public ShootTongue(final SoundEvent tongueSound, final SoundEvent eatSound) {
      super(
         ImmutableMap.of(
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.IS_PANICKING,
            MemoryStatus.VALUE_ABSENT
         ),
         100
      );
      this.tongueSound = tongueSound;
      this.eatSound = eatSound;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Frog body) {
      LivingEntity target = body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
      boolean canPathfindToTarget = this.canPathfindToTarget(body, target);
      if (!canPathfindToTarget) {
         body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
         this.addUnreachableTargetToMemory(body, target);
      }

      return canPathfindToTarget && body.getPose() != Pose.CROAKING && Frog.canEat(target);
   }

   protected boolean canStillUse(final ServerLevel level, final Frog body, final long timestamp) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
         && this.state != ShootTongue.State.DONE
         && !body.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
   }

   protected void start(final ServerLevel level, final Frog body, final long timestamp) {
      LivingEntity target = body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
      BehaviorUtils.lookAtEntity(body, target);
      body.setTongueTarget(target);
      body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target.position(), 2.0F, 0));
      this.calculatePathCounter = 10;
      this.state = ShootTongue.State.MOVE_TO_TARGET;
   }

   protected void stop(final ServerLevel level, final Frog body, final long timestamp) {
      body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
      body.eraseTongueTarget();
      body.setPose(Pose.STANDING);
   }

   private void eatEntity(final ServerLevel level, final Frog body) {
      level.playSound(null, body, this.eatSound, SoundSource.NEUTRAL, 2.0F, 1.0F);
      Optional<Entity> tongueTarget = body.getTongueTarget();
      if (tongueTarget.isPresent()) {
         Entity target = tongueTarget.get();
         if (target.isAlive()) {
            body.doHurtTarget(level, target);
            if (!target.isAlive()) {
               target.remove(Entity.RemovalReason.KILLED);
            }
         }
      }
   }

   protected void tick(final ServerLevel level, final Frog body, final long timestamp) {
      LivingEntity target = body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
      body.setTongueTarget(target);
      switch (this.state) {
         case MOVE_TO_TARGET:
            if (target.distanceTo(body) < 1.75F) {
               level.playSound(null, body, this.tongueSound, SoundSource.NEUTRAL, 2.0F, 1.0F);
               body.setPose(Pose.USING_TONGUE);
               target.setDeltaMovement(target.position().vectorTo(body.position()).normalize().scale(0.75));
               this.eatAnimationTimer = 0;
               this.state = ShootTongue.State.CATCH_ANIMATION;
            } else if (this.calculatePathCounter <= 0) {
               body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target.position(), 2.0F, 0));
               this.calculatePathCounter = 10;
            } else {
               this.calculatePathCounter--;
            }
            break;
         case CATCH_ANIMATION:
            if (this.eatAnimationTimer++ >= 6) {
               this.state = ShootTongue.State.EAT_ANIMATION;
               this.eatEntity(level, body);
            }
            break;
         case EAT_ANIMATION:
            if (this.eatAnimationTimer >= 10) {
               this.state = ShootTongue.State.DONE;
            } else {
               this.eatAnimationTimer++;
            }
         case DONE:
      }
   }

   private boolean canPathfindToTarget(final Frog body, final LivingEntity target) {
      Path path = body.getNavigation().createPath(target, 0);
      return path != null && path.getDistToTarget() < 1.75F;
   }

   private void addUnreachableTargetToMemory(final Frog body, final LivingEntity entity) {
      List<UUID> unreachableTargets = body.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS).orElseGet(ArrayList::new);
      boolean shouldAddUnreachableTarget = !unreachableTargets.contains(entity.getUUID());
      if (unreachableTargets.size() == 5 && shouldAddUnreachableTarget) {
         unreachableTargets.remove(0);
      }

      if (shouldAddUnreachableTarget) {
         unreachableTargets.add(entity.getUUID());
      }

      body.getBrain().setMemoryWithExpiry(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS, unreachableTargets, 100L);
   }

   private enum State {
      MOVE_TO_TARGET,
      CATCH_ANIMATION,
      EAT_ANIMATION,
      DONE;
   }
}
