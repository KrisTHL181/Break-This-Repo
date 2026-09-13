package net.minecraft.world.entity.animal.axolotl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.TryFindWater;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

public class AxolotlAi {
   private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);
   private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 0.2F;
   private static final float SPEED_MULTIPLIER_ON_LAND = 0.15F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING_IN_WATER = 0.5F;
   private static final float SPEED_MULTIPLIER_WHEN_CHASING_IN_WATER = 0.6F;
   private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT_IN_WATER = 0.6F;

   protected static List<ActivityData<Axolotl>> getActivities() {
      return List.of(initCoreActivity(), initIdleActivity(), initFightActivity(), initPlayDeadActivity());
   }

   protected static ActivityData<Axolotl> initPlayDeadActivity() {
      return ActivityData.create(
         Activity.PLAY_DEAD,
         ImmutableList.of(Pair.of(0, new PlayDead()), Pair.of(1, EraseMemoryIf.create(BehaviorUtils::isBreeding, MemoryModuleType.PLAY_DEAD_TICKS))),
         ImmutableSet.of(Pair.of(MemoryModuleType.PLAY_DEAD_TICKS, MemoryStatus.VALUE_PRESENT)),
         ImmutableSet.of(MemoryModuleType.PLAY_DEAD_TICKS)
      );
   }

   protected static ActivityData<Axolotl> initFightActivity() {
      return ActivityData.create(
         Activity.FIGHT,
         0,
         ImmutableList.of(
            StopAttackingIfTargetInvalid.create(Axolotl::onStopAttacking),
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(AxolotlAi::getSpeedModifierChasing),
            MeleeAttack.create(20),
            EraseMemoryIf.create(BehaviorUtils::isBreeding, MemoryModuleType.ATTACK_TARGET)
         ),
         MemoryModuleType.ATTACK_TARGET
      );
   }

   protected static ActivityData<Axolotl> initCoreActivity() {
      return ActivityData.create(
         Activity.CORE,
         0,
         ImmutableList.of(
            new LookAtTargetSink(45, 90),
            new MoveToTargetSink(),
            ValidatePlayDead.create(),
            new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)
         )
      );
   }

   protected static ActivityData<Axolotl> initIdleActivity() {
      return ActivityData.create(
         Activity.IDLE,
         ImmutableList.of(
            Pair.of(0, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 6.0F, UniformInt.of(30, 60))),
            Pair.of(1, new AnimalMakeLove(EntityTypes.AXOLOTL, 0.2F, 2)),
            Pair.of(
               2,
               new RunOne(
                  ImmutableList.of(
                     Pair.of(new FollowTemptation(AxolotlAi::getSpeedModifier), 1),
                     Pair.of(
                        BabyFollowAdult.create(ADULT_FOLLOW_RANGE, AxolotlAi::getSpeedModifierFollowingAdult, MemoryModuleType.NEAREST_VISIBLE_ADULT, false), 1
                     )
                  )
               )
            ),
            Pair.of(3, StartAttacking.create(AxolotlAi::findNearestValidAttackTarget)),
            Pair.of(3, TryFindWater.create(6, 0.15F)),
            Pair.of(
               4,
               new GateBehavior(
                  ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                  ImmutableSet.of(),
                  GateBehavior.OrderPolicy.ORDERED,
                  GateBehavior.RunningPolicy.TRY_ALL,
                  ImmutableList.of(
                     Pair.of(RandomStroll.swim(0.5F), 2),
                     Pair.of(RandomStroll.stroll(0.15F, false), 2),
                     Pair.of(SetWalkTargetFromLookTarget.create(AxolotlAi::canSetWalkTargetFromLookTarget, AxolotlAi::getSpeedModifier, 3), 3),
                     Pair.of(BehaviorBuilder.triggerIf(Entity::isInWater), 5),
                     Pair.of(BehaviorBuilder.triggerIf(Entity::onGround), 5)
                  )
               )
            )
         )
      );
   }

   private static boolean canSetWalkTargetFromLookTarget(final LivingEntity body) {
      Level level = body.level();
      Optional<PositionTracker> lookTarget = body.getBrain().getMemory(MemoryModuleType.LOOK_TARGET);
      if (lookTarget.isPresent()) {
         BlockPos pos = lookTarget.get().currentBlockPosition();
         return level.isWaterAt(pos) == body.isInWater();
      } else {
         return false;
      }
   }

   public static void updateActivity(final Axolotl body) {
      Brain<Axolotl> brain = body.getBrain();
      Activity oldActivity = brain.getActiveNonCoreActivity().orElse(null);
      if (oldActivity != Activity.PLAY_DEAD) {
         brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.PLAY_DEAD, Activity.FIGHT, Activity.IDLE));
         if (oldActivity == Activity.FIGHT && brain.getActiveNonCoreActivity().orElse(null) != Activity.FIGHT) {
            brain.setMemoryWithExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN, true, 2400L);
         }
      }
   }

   private static float getSpeedModifierChasing(final LivingEntity mob) {
      return mob.isInWater() ? 0.6F : 0.15F;
   }

   private static float getSpeedModifierFollowingAdult(final LivingEntity mob) {
      return mob.isInWater() ? 0.6F : 0.15F;
   }

   private static float getSpeedModifier(final LivingEntity mob) {
      return mob.isInWater() ? 0.5F : 0.15F;
   }

   private static Optional<? extends LivingEntity> findNearestValidAttackTarget(final ServerLevel level, final Axolotl body) {
      return BehaviorUtils.isBreeding(body) ? Optional.empty() : body.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE);
   }
}
