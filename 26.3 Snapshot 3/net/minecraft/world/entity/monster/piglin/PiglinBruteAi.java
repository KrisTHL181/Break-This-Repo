package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;

public class PiglinBruteAi {
   private static final int ANGER_DURATION = 600;
   private static final int MELEE_ATTACK_COOLDOWN = 20;
   private static final double ACTIVITY_SOUND_LIKELIHOOD_PER_TICK = 0.0125;
   private static final int MAX_LOOK_DIST = 8;
   private static final int INTERACTION_RANGE = 8;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;
   private static final int HOME_CLOSE_ENOUGH_DISTANCE = 2;
   private static final int HOME_TOO_FAR_DISTANCE = 100;
   private static final int HOME_STROLL_AROUND_DISTANCE = 5;

   public static List<ActivityData<PiglinBrute>> getActivities(final PiglinBrute piglin) {
      return List.of(initCoreActivity(), initIdleActivity(), initFightActivity(piglin));
   }

   protected static void initMemories(final PiglinBrute body) {
      GlobalPos currentGlobalPos = GlobalPos.of(body.level().dimension(), body.blockPosition());
      body.getBrain().setMemory(MemoryModuleType.HOME, currentGlobalPos);
   }

   private static ActivityData<PiglinBrute> initCoreActivity() {
      return ActivityData.create(
         Activity.CORE,
         0,
         ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink(), InteractWithDoor.create(), StopBeingAngryIfTargetDead.create())
      );
   }

   private static ActivityData<PiglinBrute> initIdleActivity() {
      return ActivityData.create(
         Activity.IDLE,
         10,
         ImmutableList.of(
            StartAttacking.create(PiglinBruteAi::findNearestValidAttackTarget),
            createIdleLookBehaviors(),
            createIdleMovementBehaviors(),
            SetLookAndInteract.create(EntityTypes.PLAYER, 4)
         )
      );
   }

   private static ActivityData<PiglinBrute> initFightActivity(final PiglinBrute body) {
      return ActivityData.create(
         Activity.FIGHT,
         10,
         ImmutableList.of(
            StopAttackingIfTargetInvalid.create((level, target) -> !isNearestValidAttackTarget(level, body, target)),
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
            MeleeAttack.create(20)
         ),
         MemoryModuleType.ATTACK_TARGET
      );
   }

   private static RunOne<PiglinBrute> createIdleLookBehaviors() {
      return new RunOne<>(
         ImmutableList.of(
            Pair.of(SetEntityLookTarget.create(EntityTypes.PLAYER, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(EntityTypes.PIGLIN, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(EntityTypes.PIGLIN_BRUTE, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(8.0F), 1),
            Pair.of(new DoNothing(30, 60), 1)
         )
      );
   }

   private static RunOne<PiglinBrute> createIdleMovementBehaviors() {
      return new RunOne<>(
         ImmutableList.of(
            Pair.of(RandomStroll.stroll(0.6F), 2),
            Pair.of(InteractWith.of(EntityTypes.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
            Pair.of(InteractWith.of(EntityTypes.PIGLIN_BRUTE, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
            Pair.of(StrollToPoi.create(MemoryModuleType.HOME, 0.6F, 2, 100), 2),
            Pair.of(StrollAroundPoi.create(MemoryModuleType.HOME, 0.6F, 5), 2),
            Pair.of(new DoNothing(30, 60), 1)
         )
      );
   }

   protected static void updateActivity(final PiglinBrute body) {
      Brain<PiglinBrute> brain = body.getBrain();
      Activity oldActivity = brain.getActiveNonCoreActivity().orElse(null);
      brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
      Activity newActivity = brain.getActiveNonCoreActivity().orElse(null);
      if (oldActivity != newActivity) {
         playActivitySound(body);
      }

      body.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
   }

   private static boolean isNearestValidAttackTarget(final ServerLevel level, final AbstractPiglin body, final LivingEntity target) {
      return findNearestValidAttackTarget(level, body).filter(nearestValidTarget -> nearestValidTarget == target).isPresent();
   }

   private static Optional<? extends LivingEntity> findNearestValidAttackTarget(final ServerLevel level, final AbstractPiglin body) {
      Optional<LivingEntity> angryAt = BehaviorUtils.getLivingEntityFromUUIDMemory(body, MemoryModuleType.ANGRY_AT);
      if (angryAt.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(level, body, angryAt.get())) {
         return angryAt;
      }

      Optional<? extends LivingEntity> player = body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
      return player.isPresent() ? player : body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
   }

   protected static void wasHurtBy(final ServerLevel level, final PiglinBrute body, final LivingEntity attacker) {
      if (!(attacker instanceof AbstractPiglin)) {
         PiglinAi.maybeRetaliate(level, body, attacker);
      }
   }

   protected static void setAngerTarget(final PiglinBrute body, final LivingEntity target) {
      body.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      body.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), 600L);
   }

   protected static void maybePlayActivitySound(final PiglinBrute body) {
      if (body.level().getRandom().nextFloat() < 0.0125) {
         playActivitySound(body);
      }
   }

   private static void playActivitySound(final PiglinBrute body) {
      body.getBrain().getActiveNonCoreActivity().ifPresent(activity -> {
         if (activity == Activity.FIGHT) {
            body.playAngrySound();
         }
      });
   }
}
