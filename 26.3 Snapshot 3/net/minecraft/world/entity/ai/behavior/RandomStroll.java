package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RandomStroll {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;
   private static final int[][] SWIM_XY_DISTANCE_TIERS = new int[][]{{1, 1}, {3, 3}, {5, 5}, {6, 5}, {7, 7}, {10, 7}};

   public static OneShot<PathfinderMob> stroll(final float speedModifier) {
      return stroll(speedModifier, true);
   }

   public static OneShot<PathfinderMob> stroll(final float speedModifier, final boolean mayStrollFromWater) {
      return strollFlyOrSwim(speedModifier, body -> LandRandomPos.getPos(body, 10, 7), mayStrollFromWater ? b -> true : b -> !b.isInWater());
   }

   public static BehaviorControl<PathfinderMob> stroll(final float speedModifier, final int maxHorizontalDistance, final int maxVerticalDistance) {
      return strollFlyOrSwim(speedModifier, body -> LandRandomPos.getPos(body, maxHorizontalDistance, maxVerticalDistance), b -> true);
   }

   public static BehaviorControl<PathfinderMob> fly(final float speedModifier) {
      return strollFlyOrSwim(speedModifier, body -> getTargetFlyPos(body, 10, 7), b -> true);
   }

   public static BehaviorControl<PathfinderMob> swim(final float speedModifier) {
      return strollFlyOrSwim(speedModifier, RandomStroll::getTargetSwimPos, Entity::isInWater);
   }

   private static OneShot<PathfinderMob> strollFlyOrSwim(
      final float speedModifier, final Function<PathfinderMob, Vec3> fetchTargetPos, final Predicate<PathfinderMob> canRun
   ) {
      return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET)).apply(i, walkTarget -> (level, body, timestamp) -> {
         if (!canRun.test(body)) {
            return false;
         }

         Optional<Vec3> pathGoalPos = Optional.ofNullable(fetchTargetPos.apply(body));
         walkTarget.setOrErase(pathGoalPos.map(pos -> new WalkTarget(pos, speedModifier, 0)));
         return true;
      }));
   }

   private static @Nullable Vec3 getTargetSwimPos(final PathfinderMob body) {
      Vec3 fallback = null;
      Vec3 targetPos = null;

      for (int[] distance : SWIM_XY_DISTANCE_TIERS) {
         if (fallback == null) {
            targetPos = BehaviorUtils.getRandomSwimmablePos(body, distance[0], distance[1]);
         } else {
            targetPos = body.position().add(body.position().vectorTo(fallback).normalize().multiply(distance[0], distance[1], distance[0]));
         }

         boolean restrict = GoalUtils.mobRestricted(body, distance[0]);
         if (targetPos == null || body.level().getFluidState(BlockPos.containing(targetPos)).isEmpty() || GoalUtils.isRestricted(restrict, body, targetPos)) {
            return fallback;
         }

         fallback = targetPos;
      }

      return targetPos;
   }

   private static @Nullable Vec3 getTargetFlyPos(final PathfinderMob body, final int maxHorizontalDistance, final int maxVerticalDistance) {
      Vec3 wanderDirection = body.getViewVector(0.0F);
      return AirAndWaterRandomPos.getPos(body, maxHorizontalDistance, maxVerticalDistance, -2, wanderDirection.x, wanderDirection.z, (float) (Math.PI / 2));
   }
}
