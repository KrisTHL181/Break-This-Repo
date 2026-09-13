package net.minecraft.world.entity.animal.camel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
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
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

public class CamelAi {
   private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 4.0F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 2.0F;
   private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 2.5F;
   private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 2.5F;
   private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 1.0F;
   private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);
   private static final ImmutableList<SensorType<? extends Sensor<? super Camel>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FOOD_TEMPTATIONS, SensorType.NEAREST_ADULT
   );
   private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.IS_PANICKING,
      MemoryModuleType.HURT_BY,
      MemoryModuleType.HURT_BY_ENTITY,
      MemoryModuleType.WALK_TARGET,
      MemoryModuleType.LOOK_TARGET,
      MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
      MemoryModuleType.PATH,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.TEMPTING_PLAYER,
      MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
      MemoryModuleType.GAZE_COOLDOWN_TICKS,
      MemoryModuleType.IS_TEMPTED,
      new MemoryModuleType[]{MemoryModuleType.BREED_TARGET, MemoryModuleType.NEAREST_VISIBLE_ADULT}
   );

   protected static void initMemories(Camel p_249638_, RandomSource p_250704_) {
   }

   public static Brain.Provider<Camel> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   protected static Brain<?> makeBrain(Brain<Camel> p_249515_) {
      initCoreActivity(p_249515_);
      initIdleActivity(p_249515_);
      p_249515_.setCoreActivities(ImmutableSet.of(Activity.CORE));
      p_249515_.setDefaultActivity(Activity.IDLE);
      p_249515_.useDefaultActivity();
      return p_249515_;
   }

   private static void initCoreActivity(Brain<Camel> p_249998_) {
      p_249998_.addActivity(
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

   private static void initIdleActivity(Brain<Camel> p_252342_) {
      p_252342_.addActivity(
         Activity.IDLE,
         ImmutableList.of(
            Pair.of(0, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6.0F, UniformInt.of(30, 60))),
            Pair.of(1, new AnimalMakeLove(EntityType.CAMEL)),
            Pair.of(
               2,
               new RunOne(
                  ImmutableList.of(
                     Pair.of(new FollowTemptation(p_250812_ -> 2.5F, p_296810_ -> p_296810_.isBaby() ? 2.5 : 3.5), 1),
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

   public static void updateActivity(Camel p_250703_) {
      p_250703_.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
   }

   public static class CamelPanic extends AnimalPanic<Camel> {
      public CamelPanic(float p_249921_) {
         super(p_249921_);
      }

      protected boolean checkExtraStartConditions(ServerLevel p_458063_, Camel p_454430_) {
         return super.checkExtraStartConditions(p_458063_, p_454430_) && !p_454430_.isMobControlled();
      }

      protected void start(ServerLevel p_329269_, Camel p_335851_, long p_328453_) {
         p_335851_.standUpInstantly();
         super.start(p_329269_, p_335851_, p_328453_);
      }
   }

   public static class RandomSitting extends Behavior<Camel> {
      private final int minimalPoseTicks;

      public RandomSitting(int p_251207_) {
         super(ImmutableMap.of());
         this.minimalPoseTicks = p_251207_ * 20;
      }

      protected boolean checkExtraStartConditions(ServerLevel p_249520_, Camel p_250322_) {
         return !p_250322_.isInWater()
            && p_250322_.getPoseTime() >= this.minimalPoseTicks
            && !p_250322_.isLeashed()
            && p_250322_.onGround()
            && !p_250322_.hasControllingPassenger()
            && p_250322_.canCamelChangePose();
      }

      protected void start(ServerLevel p_250901_, Camel p_250345_, long p_248515_) {
         if (p_250345_.isCamelSitting()) {
            p_250345_.standUp();
         } else if (!p_250345_.isPanicking()) {
            p_250345_.sitDown();
         }
      }
   }
}
