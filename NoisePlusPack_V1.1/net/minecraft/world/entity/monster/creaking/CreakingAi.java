package net.minecraft.world.entity.monster.creaking;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;

public class CreakingAi {
   protected static final ImmutableList<? extends SensorType<? extends Sensor<? super Creaking>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS
   );
   protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.NEAREST_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.NEAREST_VISIBLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
      MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYERS,
      MemoryModuleType.LOOK_TARGET,
      MemoryModuleType.WALK_TARGET,
      MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
      MemoryModuleType.PATH,
      MemoryModuleType.ATTACK_TARGET,
      MemoryModuleType.ATTACK_COOLING_DOWN
   );

   static void initCoreActivity(Brain<Creaking> p_368784_) {
      p_368784_.addActivity(Activity.CORE, 0, ImmutableList.of(new Swim<Creaking>(0.8F) {
         protected boolean checkExtraStartConditions(ServerLevel p_365067_, Creaking p_367896_) {
            return p_367896_.canMove() && super.checkExtraStartConditions(p_365067_, p_367896_);
         }
      }, new LookAtTargetSink(45, 90), new MoveToTargetSink()));
   }

   static void initIdleActivity(Brain<Creaking> p_364638_) {
      p_364638_.addActivity(
         Activity.IDLE,
         10,
         ImmutableList.of(
            StartAttacking.create(
               (p_369678_, p_369677_) -> p_369677_.isActive(),
               (p_369697_, p_366095_) -> p_366095_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)
            ),
            SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
            new RunOne(
               ImmutableList.of(
                  Pair.of(RandomStroll.stroll(0.3F), 2), Pair.of(SetWalkTargetFromLookTarget.create(0.3F, 3), 2), Pair.of(new DoNothing(30, 60), 1)
               )
            )
         )
      );
   }

   static void initFightActivity(Creaking p_397400_, Brain<Creaking> p_362435_) {
      p_362435_.addActivityWithConditions(
         Activity.FIGHT,
         10,
         ImmutableList.of(
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
            MeleeAttack.create(Creaking::canMove, 40),
            StopAttackingIfTargetInvalid.create((p_390695_, p_390696_) -> !isAttackTargetStillReachable(p_397400_, p_390696_))
         ),
         ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT))
      );
   }

   private static boolean isAttackTargetStillReachable(Creaking p_395789_, LivingEntity p_397162_) {
      Optional<List<Player>> optional = p_395789_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYERS);
      return optional.<Boolean>map(p_390693_ -> p_397162_ instanceof Player player && p_390693_.contains(player)).orElse(false);
   }

   public static Brain.Provider<Creaking> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   public static Brain<Creaking> makeBrain(Creaking p_394141_, Brain<Creaking> p_369511_) {
      initCoreActivity(p_369511_);
      initIdleActivity(p_369511_);
      initFightActivity(p_394141_, p_369511_);
      p_369511_.setCoreActivities(ImmutableSet.of(Activity.CORE));
      p_369511_.setDefaultActivity(Activity.IDLE);
      p_369511_.useDefaultActivity();
      return p_369511_;
   }

   public static void updateActivity(Creaking p_361187_) {
      if (!p_361187_.canMove()) {
         p_361187_.getBrain().useDefaultActivity();
      } else {
         p_361187_.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
      }
   }
}
