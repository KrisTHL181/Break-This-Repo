package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;

public class BreezeAi {
   public static final float SPEED_MULTIPLIER_WHEN_SLIDING = 0.6F;
   public static final float JUMP_CIRCLE_INNER_RADIUS = 4.0F;
   public static final float JUMP_CIRCLE_MIDDLE_RADIUS = 8.0F;
   public static final float JUMP_CIRCLE_OUTER_RADIUS = 24.0F;
   private static final int TICKS_TO_REMEMBER_SEEN_TARGET = 100;

   protected static List<ActivityData<Breeze>> getActivities(final Breeze breeze) {
      return List.of(initCoreActivity(), initIdleActivity(), initFightActivity(breeze));
   }

   private static ActivityData<Breeze> initCoreActivity() {
      return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new Swim(0.8F), new LookAtTargetSink(45, 90)));
   }

   private static ActivityData<Breeze> initIdleActivity() {
      return ActivityData.create(
         Activity.IDLE,
         ImmutableList.of(
            Pair.of(0, StartAttacking.create((var0, breeze) -> breeze.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE))),
            Pair.of(
               1,
               StartAttacking.create(
                  (var0, breeze) -> breeze.getBrain()
                     .getMemory(MemoryModuleType.HURT_BY)
                     .map(DamageSource::getEntity)
                     .filter(entity -> entity instanceof LivingEntity)
                     .map(entity -> (LivingEntity)entity)
               )
            ),
            Pair.of(2, new BreezeAi.SlideToTargetSink(20, 40)),
            Pair.of(3, new RunOne(ImmutableList.of(Pair.of(new DoNothing(20, 100), 1), Pair.of(RandomStroll.stroll(0.6F), 2))))
         )
      );
   }

   private static ActivityData<Breeze> initFightActivity(final Breeze body) {
      return ActivityData.create(
         Activity.FIGHT,
         ImmutableList.of(
            Pair.of(0, StopAttackingIfTargetInvalid.create(Sensor.wasEntityAttackableLastNTicks(body, 100).negate()::test)),
            Pair.of(1, new Shoot()),
            Pair.of(2, new LongJump()),
            Pair.of(3, new ShootWhenStuck()),
            Pair.of(4, new Slide())
         ),
         ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT))
      );
   }

   public static void updateActivity(final Breeze body) {
      body.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
   }

   public static class SlideToTargetSink extends MoveToTargetSink {
      @VisibleForTesting
      public SlideToTargetSink(final int minTimeout, final int maxTimeout) {
         super(minTimeout, maxTimeout);
      }

      @Override
      protected void start(final ServerLevel level, final Mob body, final long timestamp) {
         super.start(level, body, timestamp);
         body.playSound(SoundEvents.BREEZE_SLIDE);
         body.setPose(Pose.SLIDING);
      }

      @Override
      protected void stop(final ServerLevel level, final Mob body, final long timestamp) {
         super.stop(level, body, timestamp);
         body.setPose(Pose.STANDING);
         if (body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 60L);
         }
      }
   }
}
