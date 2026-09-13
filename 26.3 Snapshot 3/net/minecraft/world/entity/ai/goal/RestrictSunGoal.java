package net.minecraft.world.entity.ai.goal;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;

public class RestrictSunGoal extends Goal {
   private final PathfinderMob mob;

   public RestrictSunGoal(final PathfinderMob mob) {
      this.mob = mob;
   }

   @Override
   public boolean canUse() {
      return this.mob.level().isBrightOutside() && this.mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && GoalUtils.hasGroundPathNavigation(this.mob);
   }

   @Override
   public void start() {
      if (this.mob.getNavigation() instanceof GroundPathNavigation pathNavigation) {
         pathNavigation.setAvoidSun(true);
      }
   }

   @Override
   public void stop() {
      if (GoalUtils.hasGroundPathNavigation(this.mob) && this.mob.getNavigation() instanceof GroundPathNavigation pathNavigation) {
         pathNavigation.setAvoidSun(false);
      }
   }
}
