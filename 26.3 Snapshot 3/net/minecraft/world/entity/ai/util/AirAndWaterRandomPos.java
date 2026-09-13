package net.minecraft.world.entity.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AirAndWaterRandomPos {
   public static @Nullable Vec3 getPos(
      final PathfinderMob mob,
      final int horizontalDist,
      final int verticalDist,
      final int flyingHeight,
      final double xDir,
      final double zDir,
      final double maxXzRadiansDifference
   ) {
      boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
      return RandomPos.generateRandomPos(
         mob, () -> generateRandomPos(mob, horizontalDist, verticalDist, flyingHeight, xDir, zDir, maxXzRadiansDifference, restrict)
      );
   }

   public static @Nullable BlockPos generateRandomPos(
      final PathfinderMob mob,
      final int horizontalDist,
      final int verticalDist,
      final int flyingHeight,
      final double xDir,
      final double zDir,
      final double maxXzRadiansDifference,
      final boolean restrict
   ) {
      BlockPos direction = RandomPos.generateRandomDirectionWithinRadians(
         mob.getRandom(), 0.0, horizontalDist, verticalDist, flyingHeight, xDir, zDir, maxXzRadiansDifference
      );
      if (direction == null) {
         return null;
      } else {
         BlockPos pos = RandomPos.generateRandomPosTowardDirection(mob, horizontalDist, mob.getRandom(), direction);
         if (!GoalUtils.isOutsideLimits(pos, mob) && !GoalUtils.isRestricted(restrict, mob, pos)) {
            pos = RandomPos.moveUpOutOfSolid(pos, mob.level().getMaxY(), blockPos -> GoalUtils.isSolid(mob, blockPos));
            return GoalUtils.hasMalus(mob, pos) ? null : pos;
         } else {
            return null;
         }
      }
   }
}
