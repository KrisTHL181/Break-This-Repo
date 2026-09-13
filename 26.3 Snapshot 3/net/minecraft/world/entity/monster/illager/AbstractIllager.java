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
   protected AbstractIllager(final EntityType<? extends AbstractIllager> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
   }

   public AbstractIllager.IllagerArmPose getArmPose() {
      return AbstractIllager.IllagerArmPose.CROSSED;
   }

   @Override
   public boolean canAttack(final LivingEntity target) {
      return target instanceof AbstractVillager && target.isBaby() ? false : super.canAttack(target);
   }

   @Override
   protected boolean considersEntityAsAlly(final Entity other) {
      if (super.considersEntityAsAlly(other)) {
         return true;
      } else {
         return !other.is(EntityTypeTags.ILLAGER_FRIENDS) ? false : this.getTeam() == null && other.getTeam() == null;
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
      public RaiderOpenDoorGoal(final Raider raider) {
         super(raider, false);
      }

      @Override
      public boolean canUse() {
         return super.canUse() && AbstractIllager.this.hasActiveRaid();
      }
   }
}
