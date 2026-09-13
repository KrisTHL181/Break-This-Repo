package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public class OwnerHurtTargetGoal extends TargetGoal {
   private final TamableAnimal tameAnimal;
   private LivingEntity ownerLastHurt;
   private int timestamp;

   public OwnerHurtTargetGoal(final TamableAnimal tameAnimal) {
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
         }

         this.ownerLastHurt = owner.getLastHurtMob();
         int ts = owner.getLastHurtMobTimestamp();
         return ts != this.timestamp
            && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT)
            && this.tameAnimal.wantsToAttack(this.ownerLastHurt, owner);
      } else {
         return false;
      }
   }

   @Override
   public void start() {
      this.mob.setTarget(this.ownerLastHurt);
      LivingEntity owner = this.tameAnimal.getOwner();
      if (owner != null) {
         this.timestamp = owner.getLastHurtMobTimestamp();
      }

      super.start();
   }
}
