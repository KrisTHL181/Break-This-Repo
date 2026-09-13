package net.minecraft.world.entity.animal.frog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.Croak;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.LongJumpMidJump;
import net.minecraft.world.entity.ai.behavior.LongJumpToPreferredBlock;
import net.minecraft.world.entity.ai.behavior.LongJumpToRandomPos;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.TryFindLand;
import net.minecraft.world.entity.ai.behavior.TryFindLandNearWater;
import net.minecraft.world.entity.ai.behavior.TryLaySpawnOnWaterNearLand;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class FrogAi {
   private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0F;
   private static final float SPEED_MULTIPLIER_ON_LAND = 1.0F;
   private static final float SPEED_MULTIPLIER_IN_WATER = 0.75F;
   private static final UniformInt TIME_BETWEEN_LONG_JUMPS = UniformInt.of(100, 140);
   private static final int MAX_LONG_JUMP_HEIGHT = 2;
   private static final int MAX_LONG_JUMP_WIDTH = 4;
   private static final float MAX_JUMP_VELOCITY_MULTIPLIER = 3.5714288F;
   private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 1.25F;

   protected static void initMemories(Frog p_218580_, RandomSource p_218581_) {
      p_218580_.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, TIME_BETWEEN_LONG_JUMPS.sample(p_218581_));
   }

   protected static Brain<?> makeBrain(Brain<Frog> p_218576_) {
      initCoreActivity(p_218576_);
      initIdleActivity(p_218576_);
      initSwimActivity(p_218576_);
      initLaySpawnActivity(p_218576_);
      initTongueActivity(p_218576_);
      initJumpActivity(p_218576_);
      p_218576_.setCoreActivities(ImmutableSet.of(Activity.CORE));
      p_218576_.setDefaultActivity(Activity.IDLE);
      p_218576_.useDefaultActivity();
      return p_218576_;
   }

   private static void initCoreActivity(Brain<Frog> p_218587_) {
      p_218587_.addActivity(
         Activity.CORE,
         0,
         ImmutableList.of(
            new AnimalPanic(2.0F),
            new LookAtTargetSink(45, 90),
            new MoveToTargetSink(),
            new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
            new CountDownCooldownTicks(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS)
         )
      );
   }

   private static void initIdleActivity(Brain<Frog> p_218591_) {
      p_218591_.addActivityWithConditions(
         Activity.IDLE,
         ImmutableList.of(
            Pair.of(0, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6.0F, UniformInt.of(30, 60))),
            Pair.of(0, new AnimalMakeLove(EntityType.FROG)),
            Pair.of(1, new FollowTemptation(p_218585_ -> 1.25F)),
            Pair.of(
               2,
               StartAttacking.create(
                  (p_359203_, p_359204_) -> canAttack(p_359204_), (p_367558_, p_218605_) -> p_218605_.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE)
               )
            ),
            Pair.of(3, TryFindLand.create(6, 1.0F)),
            Pair.of(
               4,
               new RunOne(
                  ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                  ImmutableList.of(
                     Pair.of(RandomStroll.stroll(1.0F), 1),
                     Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 1),
                     Pair.of(new Croak(), 3),
                     Pair.of(BehaviorBuilder.triggerIf(Entity::onGround), 2)
                  )
               )
            )
         ),
         ImmutableSet.of(
            Pair.of(MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_ABSENT), Pair.of(MemoryModuleType.IS_IN_WATER, MemoryStatus.VALUE_ABSENT)
         )
      );
   }

   private static void initSwimActivity(Brain<Frog> p_218595_) {
      p_218595_.addActivityWithConditions(
         Activity.SWIM,
         ImmutableList.of(
            Pair.of(0, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6.0F, UniformInt.of(30, 60))),
            Pair.of(1, new FollowTemptation(p_218574_ -> 1.25F)),
            Pair.of(
               2,
               StartAttacking.create(
                  (p_359207_, p_359208_) -> canAttack(p_359208_), (p_367407_, p_218601_) -> p_218601_.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE)
               )
            ),
            Pair.of(3, TryFindLand.create(8, 1.5F)),
            Pair.of(
               5,
               new GateBehavior(
                  ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                  ImmutableSet.of(),
                  GateBehavior.OrderPolicy.ORDERED,
                  GateBehavior.RunningPolicy.TRY_ALL,
                  ImmutableList.of(
                     Pair.of(RandomStroll.swim(0.75F), 1),
                     Pair.of(RandomStroll.stroll(1.0F, true), 1),
                     Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 1),
                     Pair.of(BehaviorBuilder.triggerIf(Entity::isInWater), 5)
                  )
               )
            )
         ),
         ImmutableSet.of(
            Pair.of(MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_ABSENT), Pair.of(MemoryModuleType.IS_IN_WATER, MemoryStatus.VALUE_PRESENT)
         )
      );
   }

   private static void initLaySpawnActivity(Brain<Frog> p_218599_) {
      p_218599_.addActivityWithConditions(
         Activity.LAY_SPAWN,
         ImmutableList.of(
            Pair.of(0, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6.0F, UniformInt.of(30, 60))),
            Pair.of(
               1,
               StartAttacking.create(
                  (p_359205_, p_359206_) -> canAttack(p_359206_), (p_369128_, p_218597_) -> p_218597_.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE)
               )
            ),
            Pair.of(2, TryFindLandNearWater.create(8, 1.0F)),
            Pair.of(3, TryLaySpawnOnWaterNearLand.create(Blocks.FROGSPAWN)),
            Pair.of(
               4,
               new RunOne(
                  ImmutableList.of(
                     Pair.of(RandomStroll.stroll(1.0F), 2),
                     Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 1),
                     Pair.of(new Croak(), 2),
                     Pair.of(BehaviorBuilder.triggerIf(Entity::onGround), 1)
                  )
               )
            )
         ),
         ImmutableSet.of(
            Pair.of(MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_ABSENT), Pair.of(MemoryModuleType.IS_PREGNANT, MemoryStatus.VALUE_PRESENT)
         )
      );
   }

   private static void initJumpActivity(Brain<Frog> p_218603_) {
      p_218603_.addActivityWithConditions(
         Activity.LONG_JUMP,
         ImmutableList.of(
            Pair.of(0, new LongJumpMidJump(TIME_BETWEEN_LONG_JUMPS, SoundEvents.FROG_STEP)),
            Pair.of(
               1,
               new LongJumpToPreferredBlock<>(
                  TIME_BETWEEN_LONG_JUMPS,
                  2,
                  4,
                  3.5714288F,
                  p_218593_ -> SoundEvents.FROG_LONG_JUMP,
                  BlockTags.FROG_PREFER_JUMP_TO,
                  0.5F,
                  FrogAi::isAcceptableLandingSpot
               )
            )
         ),
         ImmutableSet.of(
            Pair.of(MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.IS_IN_WATER, MemoryStatus.VALUE_ABSENT)
         )
      );
   }

   private static void initTongueActivity(Brain<Frog> p_218607_) {
      p_218607_.addActivityAndRemoveMemoryWhenStopped(
         Activity.TONGUE,
         0,
         ImmutableList.of(StopAttackingIfTargetInvalid.create(), new ShootTongue(SoundEvents.FROG_TONGUE, SoundEvents.FROG_EAT)),
         MemoryModuleType.ATTACK_TARGET
      );
   }

   private static <E extends Mob> boolean isAcceptableLandingSpot(E p_249699_, BlockPos p_250057_) {
      Level level = p_249699_.level();
      BlockPos blockpos = p_250057_.below();
      if (level.getFluidState(p_250057_).isEmpty() && level.getFluidState(blockpos).isEmpty() && level.getFluidState(p_250057_.above()).isEmpty()) {
         BlockState blockstate = level.getBlockState(p_250057_);
         BlockState blockstate1 = level.getBlockState(blockpos);
         if (!blockstate.is(BlockTags.FROG_PREFER_JUMP_TO) && !blockstate1.is(BlockTags.FROG_PREFER_JUMP_TO)) {
            PathfindingContext pathfindingcontext = new PathfindingContext(p_249699_.level(), p_249699_);
            PathType pathtype = WalkNodeEvaluator.getPathTypeStatic(pathfindingcontext, p_250057_.mutable());
            PathType pathtype1 = WalkNodeEvaluator.getPathTypeStatic(pathfindingcontext, blockpos.mutable());
            return pathtype == PathType.TRAPDOOR || blockstate.isAir() && pathtype1 == PathType.TRAPDOOR
               ? true
               : LongJumpToRandomPos.defaultAcceptableLandingSpot(p_249699_, p_250057_);
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean canAttack(Frog p_218589_) {
      return !BehaviorUtils.isBreeding(p_218589_);
   }

   public static void updateActivity(Frog p_218578_) {
      p_218578_.getBrain()
         .setActiveActivityToFirstValid(ImmutableList.of(Activity.TONGUE, Activity.LAY_SPAWN, Activity.LONG_JUMP, Activity.SWIM, Activity.IDLE));
   }

   public static Predicate<ItemStack> getTemptations() {
      return p_326992_ -> p_326992_.is(ItemTags.FROG_FOOD);
   }
}
