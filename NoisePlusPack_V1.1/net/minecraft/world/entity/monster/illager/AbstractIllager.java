package net.minecraft.world.entity.monster.illager;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;

public abstract class AbstractIllager extends Raider {
   protected AbstractIllager(EntityType<? extends AbstractIllager> p_458606_, Level p_460243_) {
      super(p_458606_, p_460243_);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
   }

   public AbstractIllager.IllagerArmPose getArmPose() {
      return AbstractIllager.IllagerArmPose.CROSSED;
   }

   @Override
   public boolean canAttack(LivingEntity p_460879_) {
      return p_460879_ instanceof AbstractVillager && p_460879_.isBaby() ? false : super.canAttack(p_460879_);
   }

   @Override
   protected boolean considersEntityAsAlly(Entity p_450873_) {
      if (super.considersEntityAsAlly(p_450873_)) {
         return true;
      } else {
         return !p_450873_.getType().is(EntityTypeTags.ILLAGER_FRIENDS) ? false : this.getTeam() == null && p_450873_.getTeam() == null;
      }
   }

   public enum IllagerArmPose {
      CROSSED,
      ATTACKING,
      SPELLCASTING,
      BOW_AND_ARROW,
      CROSSBOW_HOLD,
      CROSSBOW_CHARGE,
      CELEBRATING,
      NEUTRAL;
   }

   protected class RaiderOpenDoorGoal extends OpenDoorGoal {
      public RaiderOpenDoorGoal(final Raider p_452917_) {
         super(p_452917_, false);
      }

      @Override
      public boolean canUse() {
         return super.canUse() && AbstractIllager.this.hasActiveRaid();
      }
   }
}
