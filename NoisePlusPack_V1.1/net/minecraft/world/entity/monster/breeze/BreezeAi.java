package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
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
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

public class BreezeAi {
   public static final float SPEED_MULTIPLIER_WHEN_SLIDING = 0.6F;
   public static final float JUMP_CIRCLE_INNER_RADIUS = 4.0F;
   public static final float JUMP_CIRCLE_MIDDLE_RADIUS = 8.0F;
   public static final float JUMP_CIRCLE_OUTER_RADIUS = 24.0F;
   static final List<SensorType<? extends Sensor<? super Breeze>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.NEAREST_PLAYERS, SensorType.BREEZE_ATTACK_ENTITY_SENSOR
   );
   static final List<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.LOOK_TARGET,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_ATTACKABLE,
      MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
      MemoryModuleType.ATTACK_TARGET,
      MemoryModuleType.WALK_TARGET,
      MemoryModuleType.BREEZE_JUMP_COOLDOWN,
      MemoryModuleType.BREEZE_JUMP_INHALING,
      MemoryModuleType.BREEZE_SHOOT,
      MemoryModuleType.BREEZE_SHOOT_CHARGING,
      MemoryModuleType.BREEZE_SHOOT_RECOVERING,
      MemoryModuleType.BREEZE_SHOOT_COOLDOWN,
      new MemoryModuleType[]{
         MemoryModuleType.BREEZE_JUMP_TARGET,
         MemoryModuleType.BREEZE_LEAVING_WATER,
         MemoryModuleType.HURT_BY,
         MemoryModuleType.HURT_BY_ENTITY,
         MemoryModuleType.PATH
      }
   );
   private static final int TICKS_TO_REMEMBER_SEEN_TARGET = 100;

   protected static Brain<?> makeBrain(Breeze p_342303_, Brain<Breeze> p_311919_) {
      initCoreActivity(p_311919_);
      initIdleActivity(p_311919_);
      initFightActivity(p_342303_, p_311919_);
      p_311919_.setCoreActivities(Set.of(Activity.CORE));
      p_311919_.setDefaultActivity(Activity.FIGHT);
      p_311919_.useDefaultActivity();
      return p_311919_;
   }

   private static void initCoreActivity(Brain<Breeze> p_312238_) {
      p_312238_.addActivity(Activity.CORE, 0, ImmutableList.of(new Swim(0.8F), new LookAtTargetSink(45, 90)));
   }

   private static void initIdleActivity(Brain<Breeze> p_335718_) {
      p_335718_.addActivity(
         Activity.IDLE,
         ImmutableList.of(
            Pair.of(0, StartAttacking.create((p_369589_, p_312068_) -> p_312068_.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE))),
            Pair.of(1, StartAttacking.create((p_359265_, p_359266_) -> p_359266_.getHurtBy())),
            Pair.of(2, new BreezeAi.SlideToTargetSink(20, 40)),
            Pair.of(3, new RunOne(ImmutableList.of(Pair.of(new DoNothing(20, 100), 1), Pair.of(RandomStroll.stroll(0.6F), 2))))
         )
      );
   }

   private static void initFightActivity(Breeze p_344626_, Brain<Breeze> p_310469_) {
      p_310469_.addActivityWithConditions(
         Activity.FIGHT,
         ImmutableList.of(
            Pair.of(0, StopAttackingIfTargetInvalid.create(Sensor.wasEntityAttackableLastNTicks(p_344626_, 100).negate()::test)),
            Pair.of(1, new Shoot()),
            Pair.of(2, new LongJump()),
            Pair.of(3, new ShootWhenStuck()),
            Pair.of(4, new Slide())
         ),
         ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT))
      );
   }

   static void updateActivity(Breeze p_331608_) {
      p_331608_.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
   }

   public static class SlideToTargetSink extends MoveToTargetSink {
      @VisibleForTesting
      public SlideToTargetSink(int p_309679_, int p_309866_) {
         super(p_309679_, p_309866_);
      }

      @Override
      protected void start(ServerLevel p_312379_, Mob p_312744_, long p_311813_) {
         super.start(p_312379_, p_312744_, p_311813_);
         p_312744_.playSound(SoundEvents.BREEZE_SLIDE);
         p_312744_.setPose(Pose.SLIDING);
      }

      @Override
      protected void stop(ServerLevel p_311146_, Mob p_310932_, long p_312981_) {
         super.stop(p_311146_, p_310932_, p_312981_);
         p_310932_.setPose(Pose.STANDING);
         if (p_310932_.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            p_310932_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 60L);
         }
      }
   }
}
