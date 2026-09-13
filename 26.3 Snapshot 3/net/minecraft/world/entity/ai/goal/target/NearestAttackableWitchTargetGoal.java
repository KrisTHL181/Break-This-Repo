package net.minecraft.world.entity.ai.goal.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.raid.Raider;
import org.jspecify.annotations.Nullable;

public class NearestAttackableWitchTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
   private boolean canAttack = true;

   public NearestAttackableWitchTargetGoal(
      final Raider raider,
      final Class<T> targetType,
      final int randomInterval,
      final boolean mustSee,
      final boolean mustReach,
      final TargetingConditions.@Nullable Selector subselector
   ) {
      super(raider, targetType, randomInterval, mustSee, mustReach, subselector);
   }

   public void setCanAttack(final boolean canAttack) {
      this.canAttack = canAttack;
   }

   @Override
   public boolean canUse() {
      return this.canAttack && super.canUse();
   }
}
