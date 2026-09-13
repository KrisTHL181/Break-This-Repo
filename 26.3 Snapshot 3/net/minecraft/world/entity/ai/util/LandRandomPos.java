package net.minecraft.world.entity.ai.util;

import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LandRandomPos {
   public static @Nullable Vec3 getPos(final PathfinderMob mob, final int horizontalDist, final int verticalDist) {
      return getPos(mob, horizontalDist, verticalDist, mob::getWalkTargetValue);
   }

   public static @Nullable Vec3 getPos(
      final PathfinderMob mob, final int horizontalDist, final int verticalDist, final ToDoubleFunction<BlockPos> positionWeight
   ) {
      boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
      return RandomPos.generateRandomPos(() -> {
         BlockPos direction = RandomPos.generateRandomDirection(mob.getRandom(), horizontalDist, verticalDist);
         BlockPos pos = generateRandomPosTowardDirection(mob, horizontalDist, restrict, direction);
         return pos == null ? null : movePosUpOutOfSolid(mob, pos);
      }, positionWeight);
   }

   public static @Nullable Vec3 getPosTowards(final PathfinderMob mob, final int horizontalDist, final int verticalDist, final Vec3 towardsPos) {
      Vec3 dir = towardsPos.subtract(mob.getX(), mob.getY(), mob.getZ());
      boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
      return getPosInDirection(mob, 0.0, horizontalDist, verticalDist, dir, restrict);
   }

   public static @Nullable Vec3 getPosAway(final PathfinderMob mob, final int horizontalDist, final int verticalDist, final Vec3 avoidPos) {
      return getPosAway(mob, 0.0, horizontalDist, verticalDist, avoidPos);
   }

   public static @Nullable Vec3 getPosAway(
      final PathfinderMob mob, final double minHorizontalDist, final double maxHorizontalDist, final int verticalDist, final Vec3 avoidPos
   ) {
      Vec3 dirAway = mob.position().subtract(avoidPos);
      if (dirAway.length() == 0.0) {
         dirAway = new Vec3(mob.getRandom().nextDouble() - 0.5, 0.0, mob.getRandom().nextDouble() - 0.5);
      }

      boolean restrict = GoalUtils.mobRestricted(mob, maxHorizontalDist);
      return getPosInDirection(mob, minHorizontalDist, maxHorizontalDist, verticalDist, dirAway, restrict);
   }

   private static @Nullable Vec3 getPosInDirection(
      final PathfinderMob mob, final double minHorizontalDist, final double maxHorizontalDist, final int verticalDist, final Vec3 dir, final boolean restrict
   ) {
      return RandomPos.generateRandomPos(
         mob,
         () -> {
            BlockPos direction = RandomPos.generateRandomDirectionWithinRadians(
               mob.getRandom(), minHorizontalDist, maxHorizontalDist, verticalDist, 0, dir.x, dir.z, (float) (Math.PI / 2)
            );
            if (direction == null) {
               return null;
            }

            BlockPos pos = generateRandomPosTowardDirection(mob, maxHorizontalDist, restrict, direction);
            return pos == null ? null : movePosUpOutOfSolid(mob, pos);
         }
      );
   }

   public static @Nullable BlockPos movePosUpOutOfSolid(final PathfinderMob mob, BlockPos pos) {
      pos = RandomPos.moveUpOutOfSolid(pos, mob.level().getMaxY(), blockPos -> GoalUtils.isSolid(mob, blockPos));
      return !GoalUtils.isWater(mob, pos) && !GoalUtils.hasMalus(mob, pos) ? pos : null;
   }

   public static @Nullable BlockPos generateRandomPosTowardDirection(
      final PathfinderMob mob, final double horizontalDist, final boolean restrict, final BlockPos direction
   ) {
      BlockPos pos = RandomPos.generateRandomPosTowardDirection(mob, horizontalDist, mob.getRandom(), direction);
      return !GoalUtils.isOutsideLimits(pos, mob) && !GoalUtils.isRestricted(restrict, mob, pos) && !GoalUtils.isNotStable(mob.getNavigation(), pos)
         ? pos
         : null;
   }
}
