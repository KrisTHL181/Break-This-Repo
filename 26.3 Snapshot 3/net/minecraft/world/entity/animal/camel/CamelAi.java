package net.minecraft.world.entity.animal.camel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomLookAround;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

public class CamelAi {
   private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 4.0F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 2.0F;
   private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 2.5F;
   private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 2.5F;
   private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 1.0F;
   private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);

   protected static void initMemories(final Camel body, final RandomSource random) {
   }

   protected static List<ActivityData<Camel>> getActivities() {
      return List.of(initCoreActivity(), initIdleActivity());
   }

   private static ActivityData<Camel> initCoreActivity() {
      return ActivityData.create(
         Activity.CORE,
         0,
         ImmutableList.of(
            new Swim(0.8F),
            new CamelAi.CamelPanic(4.0F),
            new LookAtTargetSink(45, 90),
            new MoveToTargetSink(),
            new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
            new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)
         )
      );
   }

   private static ActivityData<Camel> initIdleActivity() {
      return ActivityData.create(
         Activity.IDLE,
         ImmutableList.of(
            Pair.of(0, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 6.0F, UniformInt.of(30, 60))),
            Pair.of(1, new AnimalMakeLove(EntityTypes.CAMEL)),
            Pair.of(
               2,
               new RunOne(
                  ImmutableList.of(
                     Pair.of(new FollowTemptation(camel -> 2.5F, camel -> camel.isBaby() ? 2.5 : 3.5), 1),
                     Pair.of(BehaviorBuilder.triggerIf(Predicate.not(Camel::refuseToMove), BabyFollowAdult.create(ADULT_FOLLOW_RANGE, 2.5F)), 1)
                  )
               )
            ),
            Pair.of(3, new RandomLookAround(UniformInt.of(150, 250), 30.0F, 0.0F, 0.0F)),
            Pair.of(
               4,
               new RunOne(
                  ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                  ImmutableList.of(
                     Pair.of(BehaviorBuilder.triggerIf(Predicate.not(Camel::refuseToMove), RandomStroll.stroll(2.0F)), 1),
                     Pair.of(BehaviorBuilder.triggerIf(Predicate.not(Camel::refuseToMove), SetWalkTargetFromLookTarget.create(2.0F, 3)), 1),
                     Pair.of(new CamelAi.RandomSitting(20), 1),
                     Pair.of(new DoNothing(30, 60), 1)
                  )
               )
            )
         )
      );
   }

   public static void updateActivity(final Camel body) {
      body.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
   }

   public static class CamelPanic extends AnimalPanic<Camel> {
      public CamelPanic(final float speedMultiplier) {
         super(speedMultiplier);
      }

      protected boolean checkExtraStartConditions(final ServerLevel level, final Camel body) {
         return super.checkExtraStartConditions(level, body) && !body.isMobControlled();
      }

      protected void start(final ServerLevel level, final Camel camel, final long timestamp) {
         camel.standUpInstantly();
         super.start(level, camel, timestamp);
      }
   }

   public static class RandomSitting extends Behavior<Camel> {
      private final int minimalPoseTicks;

      public RandomSitting(final int minimalPoseTimeSec) {
         super(ImmutableMap.of());
         this.minimalPoseTicks = minimalPoseTimeSec * 20;
      }

      protected boolean checkExtraStartConditions(final ServerLevel level, final Camel body) {
         return !body.isInWater()
            && body.getPoseTime() >= this.minimalPoseTicks
            && !body.isLeashed()
            && body.onGround()
            && !body.hasControllingPassenger()
            && body.canCamelChangePose();
      }

      protected void start(final ServerLevel level, final Camel body, final long timestamp) {
         if (body.isCamelSitting()) {
            body.standUp();
         } else if (!body.isPanicking()) {
            body.sitDown();
         }
      }
   }
}
