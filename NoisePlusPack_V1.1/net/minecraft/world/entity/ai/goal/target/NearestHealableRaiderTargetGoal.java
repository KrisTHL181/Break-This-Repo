package net.minecraft.world.entity.ai.goal.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.raid.Raider;
import org.jspecify.annotations.Nullable;

public class NearestHealableRaiderTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
   private static final int DEFAULT_COOLDOWN = 200;
   private int cooldown = 0;

   public NearestHealableRaiderTargetGoal(Raider p_26087_, Class<T> p_26088_, boolean p_26089_, TargetingConditions.@Nullable Selector p_364216_) {
      super(p_26087_, p_26088_, 500, p_26089_, false, p_364216_);
   }

   public int getCooldown() {
      return this.cooldown;
   }

   public void decrementCooldown() {
      this.cooldown--;
   }

   @Override
   public boolean canUse() {
      if (this.cooldown > 0 || !this.mob.getRandom().nextBoolean()) {
         return false;
      }

      if (!((Raider)this.mob).hasActiveRaid()) {
         return false;
      }

      this.findTarget();
      return this.target != null;
   }

   @Override
   public void start() {
      this.cooldown = reducedTickDelay(200);
      super.start();
   }
}
