package net.minecraft.world.entity.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GoalUtils {
   public static boolean hasGroundPathNavigation(final Mob mob) {
      return mob.getNavigation().canNavigateGround();
   }

   public static boolean mobRestricted(final PathfinderMob mob, final double horizontalDist) {
      return mob.hasHome() && mob.getHomePosition().closerToCenterThan(mob.position(), mob.getHomeRadius() + horizontalDist + 1.0);
   }

   public static boolean isOutsideLimits(final BlockPos pos, final PathfinderMob mob) {
      return mob.level().isOutsideBuildHeight(pos.getY());
   }

   public static boolean isRestricted(final boolean restrict, final PathfinderMob mob, final BlockPos pos) {
      return restrict && !mob.isWithinHome(pos);
   }

   public static boolean isRestricted(final boolean restrict, final PathfinderMob mob, final Vec3 pos) {
      return restrict && !mob.isWithinHome(pos);
   }

   public static boolean isNotStable(final PathNavigation navigation, final BlockPos pos) {
      return !navigation.isStableDestination(pos);
   }

   public static boolean isWater(final PathfinderMob mob, final BlockPos pos) {
      return mob.level().getFluidState(pos).is(FluidTags.WATER);
   }

   public static boolean hasMalus(final PathfinderMob mob, final BlockPos pos) {
      return mob.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(mob, pos)) != 0.0F;
   }

   public static boolean isSolid(final PathfinderMob mob, final BlockPos pos) {
      return mob.level().getBlockState(pos).isSolid();
   }
}
