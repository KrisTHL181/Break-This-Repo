package net.minecraft.world.entity.ai.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GoalUtils {
   public static boolean hasGroundPathNavigation(Mob p_26895_) {
      return p_26895_.getNavigation().canNavigateGround();
   }

   public static boolean mobRestricted(PathfinderMob p_148443_, double p_458917_) {
      return p_148443_.hasHome() && p_148443_.getHomePosition().closerToCenterThan(p_148443_.position(), p_148443_.getHomeRadius() + p_458917_ + 1.0);
   }

   public static boolean isOutsideLimits(BlockPos p_148452_, PathfinderMob p_148453_) {
      return p_148453_.level().isOutsideBuildHeight(p_148452_.getY());
   }

   public static boolean isRestricted(boolean p_148455_, PathfinderMob p_148456_, BlockPos p_148457_) {
      return p_148455_ && !p_148456_.isWithinHome(p_148457_);
   }

   public static boolean isRestricted(boolean p_459806_, PathfinderMob p_455827_, Vec3 p_451540_) {
      return p_459806_ && !p_455827_.isWithinHome(p_451540_);
   }

   public static boolean isNotStable(PathNavigation p_148449_, BlockPos p_148450_) {
      return !p_148449_.isStableDestination(p_148450_);
   }

   public static boolean isWater(PathfinderMob p_148446_, BlockPos p_148447_) {
      return p_148446_.level().getFluidState(p_148447_).is(FluidTags.WATER);
   }

   public static boolean hasMalus(PathfinderMob p_148459_, BlockPos p_148460_) {
      return p_148459_.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(p_148459_, p_148460_)) != 0.0F;
   }

   public static boolean isSolid(PathfinderMob p_148462_, BlockPos p_148463_) {
      return p_148462_.level().getBlockState(p_148463_).isSolid();
   }
}
