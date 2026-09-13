package net.minecraft.world.entity.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HoverRandomPos {
   public static @Nullable Vec3 getPos(
      final PathfinderMob mob,
      final int horizontalDist,
      final int verticalDist,
      final double xDir,
      final double zDir,
      final float maxXzRadiansDifference,
      final int hoverMaxHeight,
      final int hoverMinHeight
   ) {
      boolean restrict = GoalUtils.mobRestricted(mob, horizontalDist);
      return RandomPos.generateRandomPos(
         mob,
         () -> {
            BlockPos direction = RandomPos.generateRandomDirectionWithinRadians(
               mob.getRandom(), 0.0, horizontalDist, verticalDist, 0, xDir, zDir, maxXzRadiansDifference
            );
            if (direction == null) {
               return null;
            }

            BlockPos pos = LandRandomPos.generateRandomPosTowardDirection(mob, horizontalDist, restrict, direction);
            if (pos == null) {
               return null;
            }

            pos = RandomPos.moveUpToAboveSolid(
               pos,
               mob.getRandom().nextInt(hoverMaxHeight - hoverMinHeight + 1) + hoverMinHeight,
               mob.level().getMaxY(),
               blockPos -> GoalUtils.isSolid(mob, blockPos)
            );
            return !GoalUtils.isWater(mob, pos) && !GoalUtils.hasMalus(mob, pos) ? pos : null;
         }
      );
   }
}
