package net.minecraft.world.entity.animal.armadillo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.entity.ai.behavior.OneShot;
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

public class ArmadilloAi {
   private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 1.25F;
   private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 1.25F;
   private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 1.0F;
   private static final double DEFAULT_CLOSE_ENOUGH_DIST = 2.0;
   private static final double BABY_CLOSE_ENOUGH_DIST = 1.0;
   private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);
   private static final ImmutableList<SensorType<? extends Sensor<? super Armadillo>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FOOD_TEMPTATIONS, SensorType.NEAREST_ADULT, SensorType.ARMADILLO_SCARE_DETECTED
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
      new MemoryModuleType[]{MemoryModuleType.BREED_TARGET, MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.DANGER_DETECTED_RECENTLY}
   );
   private static final OneShot<Armadillo> ARMADILLO_ROLLING_OUT = BehaviorBuilder.create(
      p_336267_ -> p_336267_.group(p_336267_.absent(MemoryModuleType.DANGER_DETECTED_RECENTLY))
         .apply(p_336267_, p_332554_ -> (p_330349_, p_333991_, p_333776_) -> {
            if (p_333991_.isScared()) {
               p_333991_.rollOut();
               return true;
            } else {
               return false;
            }
         })
   );

   public static Brain.Provider<Armadillo> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   protected static Brain<?> makeBrain(Brain<Armadillo> p_329024_) {
      initCoreActivity(p_329024_);
      initIdleActivity(p_329024_);
      initScaredActivity(p_329024_);
      p_329024_.setCoreActivities(Set.of(Activity.CORE));
      p_329024_.setDefaultActivity(Activity.IDLE);
      p_329024_.useDefaultActivity();
      return p_329024_;
   }

   private static void initCoreActivity(Brain<Armadillo> p_330038_) {
      p_330038_.addActivity(
         Activity.CORE,
         0,
         ImmutableList.of(
            new Swim(0.8F),
            new ArmadilloAi.ArmadilloPanic(2.0F),
            new LookAtTargetSink(45, 90),
            new MoveToTargetSink() {
               @Override
               protected boolean checkExtraStartConditions(ServerLevel p_335403_, Mob p_333295_) {
                  return p_333295_ instanceof Armadillo armadillo && armadillo.isScared() ? false : super.checkExtraStartConditions(p_335403_, p_333295_);
               }
            },
            new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
            new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS),
            ARMADILLO_ROLLING_OUT
         )
      );
   }

   private static void initIdleActivity(Brain<Armadillo> p_334108_) {
      p_334108_.addActivity(
         Activity.IDLE,
         ImmutableList.of(
            Pair.of(0, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6.0F, UniformInt.of(30, 60))),
            Pair.of(1, new AnimalMakeLove(EntityType.ARMADILLO, 1.0F, 1)),
            Pair.of(
               2,
               new RunOne(
                  ImmutableList.of(
                     Pair.of(new FollowTemptation(p_329728_ -> 1.25F, p_335020_ -> p_335020_.isBaby() ? 1.0 : 2.0), 1),
                     Pair.of(BabyFollowAdult.create(ADULT_FOLLOW_RANGE, 1.25F), 1)
                  )
               )
            ),
            Pair.of(3, new RandomLookAround(UniformInt.of(150, 250), 30.0F, 0.0F, 0.0F)),
            Pair.of(
               4,
               new RunOne(
                  ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                  ImmutableList.of(
                     Pair.of(RandomStroll.stroll(1.0F), 1), Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 1), Pair.of(new DoNothing(30, 60), 1)
                  )
               )
            )
         )
      );
   }

   private static void initScaredActivity(Brain<Armadillo> p_333900_) {
      p_333900_.addActivityWithConditions(
         Activity.PANIC,
         ImmutableList.of(Pair.of(0, new ArmadilloAi.ArmadilloBallUp())),
         Set.of(
            Pair.of(MemoryModuleType.DANGER_DETECTED_RECENTLY, MemoryStatus.VALUE_PRESENT), Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT)
         )
      );
   }

   public static void updateActivity(Armadillo p_328298_) {
      p_328298_.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
   }

   public static class ArmadilloBallUp extends Behavior<Armadillo> {
      static final int BALL_UP_STAY_IN_STATE = 5 * TimeUtil.SECONDS_PER_MINUTE * 20;
      static final int TICKS_DELAY_TO_DETERMINE_IF_DANGER_IS_STILL_AROUND = 5;
      static final int DANGER_DETECTED_RECENTLY_DANGER_THRESHOLD = 75;
      int nextPeekTimer = 0;
      boolean dangerWasAround;

      public ArmadilloBallUp() {
         super(Map.of(), BALL_UP_STAY_IN_STATE);
      }

      protected void tick(ServerLevel p_334605_, Armadillo p_330148_, long p_334278_) {
         super.tick(p_334605_, p_330148_, p_334278_);
         if (this.nextPeekTimer > 0) {
            this.nextPeekTimer--;
         }

         if (p_330148_.shouldSwitchToScaredState()) {
            p_330148_.switchToState(Armadillo.ArmadilloState.SCARED);
            if (p_330148_.onGround()) {
               p_330148_.playSound(SoundEvents.ARMADILLO_LAND);
            }
         } else {
            Armadillo.ArmadilloState armadillo$armadillostate = p_330148_.getState();
            long i = p_330148_.getBrain().getTimeUntilExpiry(MemoryModuleType.DANGER_DETECTED_RECENTLY);
            boolean flag = i > 75L;
            if (flag != this.dangerWasAround) {
               this.nextPeekTimer = this.pickNextPeekTimer(p_330148_);
            }

            this.dangerWasAround = flag;
            if (armadillo$armadillostate == Armadillo.ArmadilloState.SCARED) {
               if (this.nextPeekTimer == 0 && p_330148_.onGround() && flag) {
                  p_334605_.broadcastEntityEvent(p_330148_, (byte)64);
                  this.nextPeekTimer = this.pickNextPeekTimer(p_330148_);
               }

               if (i < Armadillo.ArmadilloState.UNROLLING.animationDuration()) {
                  p_330148_.playSound(SoundEvents.ARMADILLO_UNROLL_START);
                  p_330148_.switchToState(Armadillo.ArmadilloState.UNROLLING);
               }
            } else if (armadillo$armadillostate == Armadillo.ArmadilloState.UNROLLING && i > Armadillo.ArmadilloState.UNROLLING.animationDuration()) {
               p_330148_.switchToState(Armadillo.ArmadilloState.SCARED);
            }
         }
      }

      private int pickNextPeekTimer(Armadillo p_335572_) {
         return Armadillo.ArmadilloState.SCARED.animationDuration() + p_335572_.getRandom().nextIntBetweenInclusive(100, 400);
      }

      protected boolean checkExtraStartConditions(ServerLevel p_332996_, Armadillo p_331814_) {
         return p_331814_.onGround();
      }

      protected boolean canStillUse(ServerLevel p_333827_, Armadillo p_332835_, long p_327907_) {
         return p_332835_.getState().isThreatened();
      }

      protected void start(ServerLevel p_330354_, Armadillo p_328883_, long p_332299_) {
         p_328883_.rollUp();
      }

      protected void stop(ServerLevel p_333949_, Armadillo p_328962_, long p_327908_) {
         if (!p_328962_.canStayRolledUp()) {
            p_328962_.rollOut();
         }
      }
   }

   public static class ArmadilloPanic extends AnimalPanic<Armadillo> {
      public ArmadilloPanic(float p_335847_) {
         super(p_335847_, p_342538_ -> DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES);
      }

      protected void start(ServerLevel p_329316_, Armadillo p_335334_, long p_332229_) {
         p_335334_.rollOut();
         super.start(p_329316_, p_335334_, p_332229_);
      }
   }
}
