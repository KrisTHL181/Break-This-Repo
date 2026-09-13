package net.minecraft.world.entity.animal.armadillo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
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
   private static final OneShot<Armadillo> ARMADILLO_ROLLING_OUT = BehaviorBuilder.create(
      i -> i.group(i.absent(MemoryModuleType.DANGER_DETECTED_RECENTLY)).apply(i, location -> (level, body, timestamp) -> {
         if (body.isScared()) {
            body.rollOut();
            return true;
         } else {
            return false;
         }
      })
   );

   protected static List<ActivityData<Armadillo>> getActivities() {
      return List.of(initCoreActivity(), initIdleActivity(), initScaredActivity());
   }

   private static ActivityData<Armadillo> initCoreActivity() {
      return ActivityData.create(
         Activity.CORE,
         0,
         ImmutableList.of(
            new Swim(0.8F),
            new ArmadilloAi.ArmadilloPanic(2.0F),
            new LookAtTargetSink(45, 90),
            new MoveToTargetSink() {
               @Override
               protected boolean checkExtraStartConditions(final ServerLevel level, final Mob body) {
                  return body instanceof Armadillo armadillo && armadillo.isScared() ? false : super.checkExtraStartConditions(level, body);
               }
            },
            new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
            new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS),
            ARMADILLO_ROLLING_OUT
         )
      );
   }

   private static ActivityData<Armadillo> initIdleActivity() {
      return ActivityData.create(
         Activity.IDLE,
         ImmutableList.of(
            Pair.of(0, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 6.0F, UniformInt.of(30, 60))),
            Pair.of(1, new AnimalMakeLove(EntityTypes.ARMADILLO, 1.0F, 1)),
            Pair.of(
               2,
               new RunOne(
                  ImmutableList.of(
                     Pair.of(new FollowTemptation(armadillo -> 1.25F, armadillo -> armadillo.isBaby() ? 1.0 : 2.0), 1),
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

   private static ActivityData<Armadillo> initScaredActivity() {
      return ActivityData.create(
         Activity.PANIC,
         ImmutableList.of(Pair.of(0, new ArmadilloAi.ArmadilloBallUp())),
         Set.of(
            Pair.of(MemoryModuleType.DANGER_DETECTED_RECENTLY, MemoryStatus.VALUE_PRESENT), Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT)
         )
      );
   }

   public static void updateActivity(final Armadillo body) {
      body.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.IDLE));
   }

   public static class ArmadilloBallUp extends Behavior<Armadillo> {
      private static final int BALL_UP_STAY_IN_STATE = 5 * TimeUtil.SECONDS_PER_MINUTE * 20;
      private static final int TICKS_DELAY_TO_DETERMINE_IF_DANGER_IS_STILL_AROUND = 5;
      private static final int DANGER_DETECTED_RECENTLY_DANGER_THRESHOLD = 75;
      private int nextPeekTimer = 0;
      private boolean dangerWasAround;

      public ArmadilloBallUp() {
         super(Map.of(), BALL_UP_STAY_IN_STATE);
      }

      protected void tick(final ServerLevel level, final Armadillo body, final long timestamp) {
         super.tick(level, body, timestamp);
         if (this.nextPeekTimer > 0) {
            this.nextPeekTimer--;
         }

         if (body.shouldSwitchToScaredState()) {
            body.switchToState(Armadillo.ArmadilloState.SCARED);
            if (body.onGround()) {
               body.playSound(SoundEvents.ARMADILLO_LAND);
            }
         } else {
            Armadillo.ArmadilloState state = body.getState();
            long dangerTickCounter = body.getBrain().getTimeUntilExpiry(MemoryModuleType.DANGER_DETECTED_RECENTLY);
            boolean dangerIsAround = dangerTickCounter > 75L;
            if (dangerIsAround != this.dangerWasAround) {
               this.nextPeekTimer = this.pickNextPeekTimer(body);
            }

            this.dangerWasAround = dangerIsAround;
            if (state == Armadillo.ArmadilloState.SCARED) {
               if (this.nextPeekTimer == 0 && body.onGround() && dangerIsAround) {
                  level.broadcastEntityEvent(body, (byte)64);
                  this.nextPeekTimer = this.pickNextPeekTimer(body);
               }

               if (dangerTickCounter < Armadillo.ArmadilloState.UNROLLING.animationDuration()) {
                  body.playSound(SoundEvents.ARMADILLO_UNROLL_START);
                  body.switchToState(Armadillo.ArmadilloState.UNROLLING);
               }
            } else if (state == Armadillo.ArmadilloState.UNROLLING && dangerTickCounter > Armadillo.ArmadilloState.UNROLLING.animationDuration()) {
               body.switchToState(Armadillo.ArmadilloState.SCARED);
            }
         }
      }

      private int pickNextPeekTimer(final Armadillo body) {
         return Armadillo.ArmadilloState.SCARED.animationDuration() + body.getRandom().nextIntBetweenInclusive(100, 400);
      }

      protected boolean checkExtraStartConditions(final ServerLevel level, final Armadillo body) {
         return body.onGround();
      }

      protected boolean canStillUse(final ServerLevel level, final Armadillo body, final long timestamp) {
         return body.getState().isThreatened();
      }

      protected void start(final ServerLevel level, final Armadillo body, final long timestamp) {
         body.rollUp();
      }

      protected void stop(final ServerLevel level, final Armadillo body, final long timestamp) {
         if (!body.canStayRolledUp()) {
            body.rollOut();
         }
      }
   }

   public static class ArmadilloPanic extends AnimalPanic<Armadillo> {
      public ArmadilloPanic(final float speedMultiplier) {
         super(speedMultiplier, mob -> DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES);
      }

      protected void start(final ServerLevel level, final Armadillo armadillo, final long timestamp) {
         armadillo.rollOut();
         super.start(level, armadillo, timestamp);
      }
   }
}
