package net.minecraft.world.entity.ai.goal.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.raid.Raider;
import org.jspecify.annotations.Nullable;

public class NearestAttackableWitchTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
   private boolean canAttack = true;

   public NearestAttackableWitchTargetGoal(
      Raider p_26076_, Class<T> p_26077_, int p_26078_, boolean p_26079_, boolean p_26080_, TargetingConditions.@Nullable Selector p_363276_
   ) {
      super(p_26076_, p_26077_, p_26078_, p_26079_, p_26080_, p_363276_);
   }

   public void setCanAttack(boolean p_26084_) {
      this.canAttack = p_26084_;
   }

   @Override
   public boolean canUse() {
      return this.canAttack && super.canUse();
   }
}
