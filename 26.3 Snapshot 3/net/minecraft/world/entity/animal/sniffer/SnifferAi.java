package net.minecraft.world.entity.animal.sniffer;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.schedule.Activity;
import org.slf4j.Logger;

public class SnifferAi {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_LOOK_DISTANCE = 6;
   private static final int SNIFFING_COOLDOWN_TICKS = 9600;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0F;
   private static final float SPEED_MULTIPLIER_WHEN_SNIFFING = 1.25F;
   private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 1.25F;

   public static List<ActivityData<Sniffer>> getActivities() {
      return List.of(initCoreActivity(), initIdleActivity(), initSniffingActivity(), initDigActivity());
   }

   private static Sniffer resetSniffing(final Sniffer body) {
      body.getBrain().eraseMemory(MemoryModuleType.SNIFFER_DIGGING);
      body.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
      return body.transitionTo(Sniffer.State.IDLING);
   }

   private static ActivityData<Sniffer> initCoreActivity() {
      return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new Swim(0.8F), new AnimalPanic<Sniffer>(2.0F) {
         protected void start(final ServerLevel level, final Sniffer body, final long timestamp) {
            SnifferAi.resetSniffing(body);
            super.start(level, body, timestamp);
         }
      }, new MoveToTargetSink(500, 700), new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)));
   }

   private static ActivityData<Sniffer> initSniffingActivity() {
      return ActivityData.create(
         Activity.SNIFF,
         ImmutableList.of(Pair.of(0, new SnifferAi.Searching())),
         Set.of(
            Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.SNIFFER_SNIFFING_TARGET, MemoryStatus.VALUE_PRESENT),
            Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT)
         )
      );
   }

   private static ActivityData<Sniffer> initDigActivity() {
      return ActivityData.create(
         Activity.DIG,
         ImmutableList.of(Pair.of(0, new SnifferAi.Digging(160, 180)), Pair.of(0, new SnifferAi.FinishedDigging(40))),
         Set.of(
            Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.SNIFFER_DIGGING, MemoryStatus.VALUE_PRESENT)
         )
      );
   }

   private static ActivityData<Sniffer> initIdleActivity() {
      return ActivityData.create(
         Activity.IDLE,
         ImmutableList.of(
            Pair.of(0, new AnimalMakeLove(EntityTypes.SNIFFER) {
               @Override
               protected void start(final ServerLevel level, final Animal body, final long timestamp) {
                  SnifferAi.resetSniffing((Sniffer)body);
                  super.start(level, body, timestamp);
               }
            }),
            Pair.of(1, new FollowTemptation(sniffer -> 1.25F, sniffer -> sniffer.isBaby() ? 2.5 : 3.5) {
               @Override
               protected void start(final ServerLevel level, final PathfinderMob body, final long timestamp) {
                  SnifferAi.resetSniffing((Sniffer)body);
                  super.start(level, body, timestamp);
               }
            }),
            Pair.of(2, new LookAtTargetSink(45, 90)),
            Pair.of(3, new SnifferAi.FeelingHappy(40, 100)),
            Pair.of(
               4,
               new RunOne(
                  ImmutableList.of(
                     Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 2),
                     Pair.of(new SnifferAi.Scenting(40, 80), 1),
                     Pair.of(new SnifferAi.Sniffing(40, 80), 1),
                     Pair.of(SetEntityLookTarget.create(EntityTypes.PLAYER, 6.0F), 1),
                     Pair.of(RandomStroll.stroll(1.0F), 1),
                     Pair.of(new DoNothing(5, 20), 2)
                  )
               )
            )
         ),
         Set.of(Pair.of(MemoryModuleType.SNIFFER_DIGGING, MemoryStatus.VALUE_ABSENT))
      );
   }

   public static void updateActivity(final Sniffer body) {
      body.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.DIG, Activity.SNIFF, Activity.IDLE));
   }

   private static class Digging extends Behavior<Sniffer> {
      private Digging(final int min, final int max) {
         super(
            Map.of(
               MemoryModuleType.IS_PANICKING,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.WALK_TARGET,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.SNIFFER_DIGGING,
               MemoryStatus.VALUE_PRESENT,
               MemoryModuleType.SNIFF_COOLDOWN,
               MemoryStatus.VALUE_ABSENT
            ),
            min,
            max
         );
      }

      protected boolean checkExtraStartConditions(final ServerLevel level, final Sniffer sniffer) {
         return sniffer.canSniff();
      }

      protected boolean canStillUse(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         return sniffer.getBrain().getMemory(MemoryModuleType.SNIFFER_DIGGING).isPresent() && sniffer.canDig() && !sniffer.isInLove();
      }

      protected void start(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         sniffer.transitionTo(Sniffer.State.DIGGING);
      }

      protected void stop(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         boolean finished = this.timedOut(timestamp);
         if (finished) {
            sniffer.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 9600L);
         } else {
            SnifferAi.resetSniffing(sniffer);
         }
      }
   }

   private static class FeelingHappy extends Behavior<Sniffer> {
      private FeelingHappy(final int min, final int max) {
         super(Map.of(MemoryModuleType.SNIFFER_HAPPY, MemoryStatus.VALUE_PRESENT), min, max);
      }

      protected boolean canStillUse(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         return true;
      }

      protected void start(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         sniffer.transitionTo(Sniffer.State.FEELING_HAPPY);
      }

      protected void stop(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         sniffer.transitionTo(Sniffer.State.IDLING);
         sniffer.getBrain().eraseMemory(MemoryModuleType.SNIFFER_HAPPY);
      }
   }

   private static class FinishedDigging extends Behavior<Sniffer> {
      private FinishedDigging(final int duration) {
         super(
            Map.of(
               MemoryModuleType.IS_PANICKING,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.WALK_TARGET,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.SNIFFER_DIGGING,
               MemoryStatus.VALUE_PRESENT,
               MemoryModuleType.SNIFF_COOLDOWN,
               MemoryStatus.VALUE_PRESENT
            ),
            duration,
            duration
         );
      }

      protected boolean checkExtraStartConditions(final ServerLevel level, final Sniffer sniffer) {
         return true;
      }

      protected boolean canStillUse(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         return sniffer.getBrain().getMemory(MemoryModuleType.SNIFFER_DIGGING).isPresent();
      }

      protected void start(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         sniffer.transitionTo(Sniffer.State.RISING);
      }

      protected void stop(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         boolean finished = this.timedOut(timestamp);
         sniffer.transitionTo(Sniffer.State.IDLING).onDiggingComplete(finished);
         sniffer.getBrain().eraseMemory(MemoryModuleType.SNIFFER_DIGGING);
         sniffer.getBrain().setMemory(MemoryModuleType.SNIFFER_HAPPY, true);
      }
   }

   private static class Scenting extends Behavior<Sniffer> {
      private Scenting(final int min, final int max) {
         super(
            Map.of(
               MemoryModuleType.IS_PANICKING,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.SNIFFER_DIGGING,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.SNIFFER_SNIFFING_TARGET,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.SNIFFER_HAPPY,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.BREED_TARGET,
               MemoryStatus.VALUE_ABSENT
            ),
            min,
            max
         );
      }

      protected boolean checkExtraStartConditions(final ServerLevel level, final Sniffer sniffer) {
         return !sniffer.isTempted();
      }

      protected boolean canStillUse(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         return true;
      }

      protected void start(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         sniffer.transitionTo(Sniffer.State.SCENTING);
      }

      protected void stop(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         sniffer.transitionTo(Sniffer.State.IDLING);
      }
   }

   private static class Searching extends Behavior<Sniffer> {
      private Searching() {
         super(
            Map.of(
               MemoryModuleType.WALK_TARGET,
               MemoryStatus.VALUE_PRESENT,
               MemoryModuleType.IS_PANICKING,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.SNIFFER_SNIFFING_TARGET,
               MemoryStatus.VALUE_PRESENT
            ),
            600
         );
      }

      protected boolean checkExtraStartConditions(final ServerLevel level, final Sniffer sniffer) {
         return sniffer.canSniff();
      }

      protected boolean canStillUse(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         if (!sniffer.canSniff()) {
            sniffer.transitionTo(Sniffer.State.IDLING);
            return false;
         } else {
            Optional<BlockPos> walkTarget = sniffer.getBrain()
               .getMemory(MemoryModuleType.WALK_TARGET)
               .map(WalkTarget::getTarget)
               .map(PositionTracker::currentBlockPosition);
            Optional<BlockPos> sniffingTarget = sniffer.getBrain().getMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
            return !walkTarget.isEmpty() && !sniffingTarget.isEmpty() ? sniffingTarget.get().equals(walkTarget.get()) : false;
         }
      }

      protected void start(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         sniffer.transitionTo(Sniffer.State.SEARCHING);
      }

      protected void stop(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         if (sniffer.canDig() && sniffer.canSniff()) {
            sniffer.getBrain().setMemory(MemoryModuleType.SNIFFER_DIGGING, true);
         }

         sniffer.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
         sniffer.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
      }
   }

   private static class Sniffing extends Behavior<Sniffer> {
      private Sniffing(final int min, final int max) {
         super(
            Map.of(
               MemoryModuleType.WALK_TARGET,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.SNIFFER_SNIFFING_TARGET,
               MemoryStatus.VALUE_ABSENT,
               MemoryModuleType.SNIFF_COOLDOWN,
               MemoryStatus.VALUE_ABSENT
            ),
            min,
            max
         );
      }

      protected boolean checkExtraStartConditions(final ServerLevel level, final Sniffer body) {
         return !body.isBaby() && body.canSniff();
      }

      protected boolean canStillUse(final ServerLevel level, final Sniffer body, final long timestamp) {
         return body.canSniff();
      }

      protected void start(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         sniffer.transitionTo(Sniffer.State.SNIFFING);
      }

      protected void stop(final ServerLevel level, final Sniffer sniffer, final long timestamp) {
         boolean finished = this.timedOut(timestamp);
         sniffer.transitionTo(Sniffer.State.IDLING);
         if (finished) {
            sniffer.calculateDigPosition().ifPresent(position -> {
               sniffer.getBrain().setMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET, position);
               sniffer.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(position, 1.25F, 0));
            });
         }
      }
   }
}
