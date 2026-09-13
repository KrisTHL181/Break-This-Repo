package net.minecraft.world.entity.monster.hoglin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.BecomePassiveIfMemoryPresent;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;

public class HoglinAi {
   public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
   public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
   private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
   private static final int ATTACK_DURATION = 200;
   private static final int DESIRED_DISTANCE_FROM_PIGLIN_WHEN_IDLING = 8;
   private static final int DESIRED_DISTANCE_FROM_PIGLIN_WHEN_RETREATING = 15;
   private static final int ATTACK_INTERVAL = 40;
   private static final int BABY_ATTACK_INTERVAL = 15;
   private static final int REPELLENT_PACIFY_TIME = 200;
   private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);
   private static final float SPEED_MULTIPLIER_WHEN_AVOIDING_REPELLENT = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.3F;
   private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 0.6F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.4F;
   private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 0.6F;

   protected static List<ActivityData<Hoglin>> getActivities() {
      return List.of(initCoreActivity(), initIdleActivity(), initFightActivity(), initRetreatActivity());
   }

   private static ActivityData<Hoglin> initCoreActivity() {
      return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink()));
   }

   private static ActivityData<Hoglin> initIdleActivity() {
      return ActivityData.create(
         Activity.IDLE,
         10,
         ImmutableList.of(
            BecomePassiveIfMemoryPresent.create(MemoryModuleType.NEAREST_REPELLENT, 200),
            new AnimalMakeLove(EntityTypes.HOGLIN, 0.6F, 2),
            SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, true),
            StartAttacking.create(HoglinAi::findNearestValidAttackTarget),
            BehaviorBuilder.triggerIf(Hoglin::isAdult, SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, 0.4F, 8, false)),
            SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
            BabyFollowAdult.create(ADULT_FOLLOW_RANGE, 0.6F),
            createIdleMovementBehaviors()
         )
      );
   }

   private static ActivityData<Hoglin> initFightActivity() {
      return ActivityData.create(
         Activity.FIGHT,
         10,
         ImmutableList.of(
            BecomePassiveIfMemoryPresent.create(MemoryModuleType.NEAREST_REPELLENT, 200),
            new AnimalMakeLove(EntityTypes.HOGLIN, 0.6F, 2),
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
            BehaviorBuilder.triggerIf(Hoglin::isAdult, MeleeAttack.create(40)),
            BehaviorBuilder.triggerIf(AgeableMob::isBaby, MeleeAttack.create(15)),
            StopAttackingIfTargetInvalid.create(),
            EraseMemoryIf.create(HoglinAi::isBreeding, MemoryModuleType.ATTACK_TARGET)
         ),
         MemoryModuleType.ATTACK_TARGET
      );
   }

   private static ActivityData<Hoglin> initRetreatActivity() {
      return ActivityData.create(
         Activity.AVOID,
         10,
         ImmutableList.of(
            SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.3F, 15, false),
            createIdleMovementBehaviors(),
            SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
            EraseMemoryIf.create(HoglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)
         ),
         MemoryModuleType.AVOID_TARGET
      );
   }

   private static RunOne<Hoglin> createIdleMovementBehaviors() {
      return new RunOne<>(
         ImmutableList.of(Pair.of(RandomStroll.stroll(0.4F), 2), Pair.of(SetWalkTargetFromLookTarget.create(0.4F, 3), 2), Pair.of(new DoNothing(30, 60), 1))
      );
   }

   protected static void updateActivity(final Hoglin body) {
      Brain<Hoglin> brain = body.getBrain();
      Activity oldActivity = brain.getActiveNonCoreActivity().orElse(null);
      brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.AVOID, Activity.IDLE));
      Activity newActivity = brain.getActiveNonCoreActivity().orElse(null);
      if (oldActivity != newActivity) {
         getSoundForCurrentActivity(body).ifPresent(body::makeSound);
      }

      body.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
   }

   protected static void onHitTarget(final Hoglin attackerBody, final LivingEntity target) {
      if (!attackerBody.isBaby()) {
         if (target.is(EntityTypes.PIGLIN) && piglinsOutnumberHoglins(attackerBody)) {
            setAvoidTarget(attackerBody, target);
            broadcastRetreat(attackerBody, target);
         } else {
            broadcastAttackTarget(attackerBody, target);
         }
      }
   }

   private static void broadcastRetreat(final Hoglin body, final LivingEntity target) {
      getVisibleAdultHoglins(body).forEach(hoglin -> retreatFromNearestTarget(hoglin, target));
   }

   private static void retreatFromNearestTarget(final Hoglin body, final LivingEntity newAvoidTarget) {
      LivingEntity nearest = newAvoidTarget;
      Brain<Hoglin> brain = body.getBrain();
      nearest = BehaviorUtils.getNearestTarget(body, brain.getMemory(MemoryModuleType.AVOID_TARGET), nearest);
      nearest = BehaviorUtils.getNearestTarget(body, brain.getMemory(MemoryModuleType.ATTACK_TARGET), nearest);
      setAvoidTarget(body, nearest);
   }

   private static void setAvoidTarget(final Hoglin body, final LivingEntity avoidTarget) {
      body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
      body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      body.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, avoidTarget, RETREAT_DURATION.sample(body.level().getRandom()));
   }

   private static Optional<? extends LivingEntity> findNearestValidAttackTarget(final ServerLevel level, final Hoglin body) {
      return !isPacified(body) && !isBreeding(body) ? body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) : Optional.empty();
   }

   static boolean isPosNearNearestRepellent(final Hoglin body, final BlockPos pos) {
      Optional<BlockPos> repellentPos = body.getBrain().getMemory(MemoryModuleType.NEAREST_REPELLENT);
      return repellentPos.isPresent() && repellentPos.get().closerThan(pos, 8.0);
   }

   private static boolean wantsToStopFleeing(final Hoglin body) {
      return body.isAdult() && !piglinsOutnumberHoglins(body);
   }

   private static boolean piglinsOutnumberHoglins(final Hoglin body) {
      if (body.isBaby()) {
         return false;
      }

      int piglinCount = body.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0);
      int hoglinCount = body.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0) + 1;
      return piglinCount > hoglinCount;
   }

   protected static void wasHurtBy(final ServerLevel level, final Hoglin body, final LivingEntity attacker) {
      Brain<Hoglin> brain = body.getBrain();
      brain.eraseMemory(MemoryModuleType.PACIFIED);
      brain.eraseMemory(MemoryModuleType.BREED_TARGET);
      if (body.isBaby()) {
         retreatFromNearestTarget(body, attacker);
      } else {
         maybeRetaliate(level, body, attacker);
      }
   }

   private static void maybeRetaliate(final ServerLevel level, final Hoglin body, final LivingEntity attacker) {
      if (!body.getBrain().isActive(Activity.AVOID) || !attacker.is(EntityTypes.PIGLIN)) {
         if (!attacker.is(EntityTypes.HOGLIN)) {
            if (!BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(body, attacker, 4.0)) {
               if (Sensor.isEntityAttackable(level, body, attacker)) {
                  setAttackTarget(body, attacker);
                  broadcastAttackTarget(body, attacker);
               }
            }
         }
      }
   }

   private static void setAttackTarget(final Hoglin body, final LivingEntity target) {
      Brain<Hoglin> brain = body.getBrain();
      brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      brain.eraseMemory(MemoryModuleType.BREED_TARGET);
      brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, 200L);
   }

   private static void broadcastAttackTarget(final Hoglin body, final LivingEntity target) {
      getVisibleAdultHoglins(body).forEach(hoglin -> setAttackTargetIfCloserThanCurrent(hoglin, target));
   }

   private static void setAttackTargetIfCloserThanCurrent(final Hoglin body, final LivingEntity newTarget) {
      if (!isPacified(body)) {
         Optional<LivingEntity> currentTarget = body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
         LivingEntity nearest = BehaviorUtils.getNearestTarget(body, currentTarget, newTarget);
         setAttackTarget(body, nearest);
      }
   }

   public static Optional<SoundEvent> getSoundForCurrentActivity(final Hoglin body) {
      return body.getBrain().getActiveNonCoreActivity().map(activity -> getSoundForActivity(body, activity));
   }

   private static SoundEvent getSoundForActivity(final Hoglin body, final Activity activity) {
      if (activity == Activity.AVOID || body.isConverting()) {
         return SoundEvents.HOGLIN_RETREAT;
      } else if (activity == Activity.FIGHT) {
         return SoundEvents.HOGLIN_ANGRY;
      } else {
         return isNearRepellent(body) ? SoundEvents.HOGLIN_RETREAT : SoundEvents.HOGLIN_AMBIENT;
      }
   }

   private static List<Hoglin> getVisibleAdultHoglins(final Hoglin body) {
      return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS).orElse(ImmutableList.of());
   }

   private static boolean isNearRepellent(final Hoglin body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_REPELLENT);
   }

   private static boolean isBreeding(final Hoglin body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET);
   }

   protected static boolean isPacified(final Hoglin body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.PACIFIED);
   }
}
