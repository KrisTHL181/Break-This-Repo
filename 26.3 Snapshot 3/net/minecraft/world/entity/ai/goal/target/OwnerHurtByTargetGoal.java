package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public class OwnerHurtByTargetGoal extends TargetGoal {
   private final TamableAnimal tameAnimal;
   private LivingEntity ownerLastHurtBy;
   private int timestamp;

   public OwnerHurtByTargetGoal(final TamableAnimal tameAnimal) {
      super(tameAnimal, false);
      this.tameAnimal = tameAnimal;
      this.setFlags(EnumSet.of(Goal.Flag.TARGET));
   }

   @Override
   public boolean canUse() {
      if (this.tameAnimal.isTame() && !this.tameAnimal.isOrderedToSit()) {
         LivingEntity owner = this.tameAnimal.getOwner();
         if (owner == null) {
            return false;
         } else {
            DamageSource lastDamageSource = owner.getLastDamageSource(100);
            if (lastDamageSource != null && !lastDamageSource.is(DamageTypeTags.NO_WOLF_RETALIATION)) {
               this.ownerLastHurtBy = owner.getLastHurtByMob();
               int ts = owner.getLastHurtByMobTimestamp();
               return ts != this.timestamp
                  && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT)
                  && this.tameAnimal.wantsToAttack(this.ownerLastHurtBy, owner);
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   @Override
   public void start() {
      this.mob.setTarget(this.ownerLastHurtBy);
      LivingEntity owner = this.tameAnimal.getOwner();
      if (owner != null) {
         this.timestamp = owner.getLastHurtByMobTimestamp();
      }

      super.start();
   }
}
