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

   public ShootTongue(SoundEvent p_218620_, SoundEvent p_218621_) {
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
      this.tongueSound = p_218620_;
      this.eatSound = p_218621_;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_218630_, Frog p_218631_) {
      LivingEntity livingentity = p_218631_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
      boolean flag = this.canPathfindToTarget(p_218631_, livingentity);
      if (!flag) {
         p_218631_.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
         this.addUnreachableTargetToMemory(p_218631_, livingentity);
      }

      return flag && p_218631_.getPose() != Pose.CROAKING && Frog.canEat(livingentity);
   }

   protected boolean canStillUse(ServerLevel p_218633_, Frog p_218634_, long p_218635_) {
      return p_218634_.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
         && this.state != ShootTongue.State.DONE
         && !p_218634_.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
   }

   protected void start(ServerLevel p_218644_, Frog p_218645_, long p_218646_) {
      LivingEntity livingentity = p_218645_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
      BehaviorUtils.lookAtEntity(p_218645_, livingentity);
      p_218645_.setTongueTarget(livingentity);
      p_218645_.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(livingentity.position(), 2.0F, 0));
      this.calculatePathCounter = 10;
      this.state = ShootTongue.State.MOVE_TO_TARGET;
   }

   protected void stop(ServerLevel p_218652_, Frog p_218653_, long p_218654_) {
      p_218653_.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
      p_218653_.eraseTongueTarget();
      p_218653_.setPose(Pose.STANDING);
   }

   private void eatEntity(ServerLevel p_218641_, Frog p_218642_) {
      p_218641_.playSound(null, p_218642_, this.eatSound, SoundSource.NEUTRAL, 2.0F, 1.0F);
      Optional<Entity> optional = p_218642_.getTongueTarget();
      if (optional.isPresent()) {
         Entity entity = optional.get();
         if (entity.isAlive()) {
            p_218642_.doHurtTarget(p_218641_, entity);
            if (!entity.isAlive()) {
               entity.remove(Entity.RemovalReason.KILLED);
            }
         }
      }
   }

   protected void tick(ServerLevel p_218660_, Frog p_218661_, long p_218662_) {
      LivingEntity livingentity = p_218661_.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
      p_218661_.setTongueTarget(livingentity);
      switch (this.state) {
         case MOVE_TO_TARGET:
            if (livingentity.distanceTo(p_218661_) < 1.75F) {
               p_218660_.playSound(null, p_218661_, this.tongueSound, SoundSource.NEUTRAL, 2.0F, 1.0F);
               p_218661_.setPose(Pose.USING_TONGUE);
               livingentity.setDeltaMovement(livingentity.position().vectorTo(p_218661_.position()).normalize().scale(0.75));
               this.eatAnimationTimer = 0;
               this.state = ShootTongue.State.CATCH_ANIMATION;
            } else if (this.calculatePathCounter <= 0) {
               p_218661_.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(livingentity.position(), 2.0F, 0));
               this.calculatePathCounter = 10;
            } else {
               this.calculatePathCounter--;
            }
            break;
         case CATCH_ANIMATION:
            if (this.eatAnimationTimer++ >= 6) {
               this.state = ShootTongue.State.EAT_ANIMATION;
               this.eatEntity(p_218660_, p_218661_);
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

   private boolean canPathfindToTarget(Frog p_238359_, LivingEntity p_238360_) {
      Path path = p_238359_.getNavigation().createPath(p_238360_, 0);
      return path != null && path.getDistToTarget() < 1.75F;
   }

   private void addUnreachableTargetToMemory(Frog p_238444_, LivingEntity p_243335_) {
      List<UUID> list = p_238444_.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS).orElseGet(ArrayList::new);
      boolean flag = !list.contains(p_243335_.getUUID());
      if (list.size() == 5 && flag) {
         list.remove(0);
      }

      if (flag) {
         list.add(p_243335_.getUUID());
      }

      p_238444_.getBrain().setMemoryWithExpiry(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS, list, 100L);
   }

   enum State {
      MOVE_TO_TARGET,
      CATCH_ANIMATION,
      EAT_ANIMATION,
      DONE;
   }
}
