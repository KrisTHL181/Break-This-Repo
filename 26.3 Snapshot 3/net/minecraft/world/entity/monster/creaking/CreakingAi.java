package net.minecraft.world.entity.monster.creaking;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;

public class CreakingAi {
   private static ActivityData<Creaking> initCoreActivity() {
      return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new Swim<Creaking>(0.8F) {
         protected boolean checkExtraStartConditions(final ServerLevel level, final Creaking body) {
            return body.canMove() && super.checkExtraStartConditions(level, body);
         }
      }, new LookAtTargetSink(45, 90), new MoveToTargetSink()));
   }

   private static ActivityData<Creaking> initIdleActivity() {
      return ActivityData.create(
         Activity.IDLE,
         10,
         ImmutableList.of(
            StartAttacking.create((level, mob) -> mob.isActive(), (level, mob) -> mob.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)),
            SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
            new RunOne(
               ImmutableList.of(
                  Pair.of(RandomStroll.stroll(0.3F), 2), Pair.of(SetWalkTargetFromLookTarget.create(0.3F, 3), 2), Pair.of(new DoNothing(30, 60), 1)
               )
            )
         )
      );
   }

   private static ActivityData<Creaking> initFightActivity(final Creaking body) {
      return ActivityData.create(
         Activity.FIGHT,
         10,
         ImmutableList.of(
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
            MeleeAttack.create(Creaking::canMove, 40),
            StopAttackingIfTargetInvalid.create((level, target) -> !isAttackTargetStillReachable(body, target))
         ),
         ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT))
      );
   }

   private static boolean isAttackTargetStillReachable(final Creaking creaking, final LivingEntity target) {
      Optional<List<Player>> visibleAttackablePlayers = creaking.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYERS);
      return visibleAttackablePlayers.<Boolean>map(players -> target instanceof Player player && players.contains(player)).orElse(false);
   }

   protected static List<ActivityData<Creaking>> getActivities(final Creaking creaking) {
      return List.of(initCoreActivity(), initIdleActivity(), initFightActivity(creaking));
   }

   public static void updateActivity(final Creaking creaking) {
      if (!creaking.canMove()) {
         creaking.getBrain().useDefaultActivity();
      } else {
         creaking.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
      }
   }
}
