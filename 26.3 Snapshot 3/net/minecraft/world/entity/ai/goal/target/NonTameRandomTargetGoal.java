package net.minecraft.world.entity.ai.goal.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jspecify.annotations.Nullable;

public class NonTameRandomTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
   private final TamableAnimal tamableMob;

   public NonTameRandomTargetGoal(
      final TamableAnimal mob, final Class<T> targetType, final boolean mustSee, final TargetingConditions.@Nullable Selector subselector
   ) {
      super(mob, targetType, 10, mustSee, false, subselector);
      this.tamableMob = mob;
   }

   @Override
   public boolean canUse() {
      return !this.tamableMob.isTame() && super.canUse();
   }

   @Override
   public boolean canContinueToUse() {
      return this.targetConditions != null ? this.targetConditions.test(getServerLevel(this.mob), this.mob, this.target) : super.canContinueToUse();
   }
}
