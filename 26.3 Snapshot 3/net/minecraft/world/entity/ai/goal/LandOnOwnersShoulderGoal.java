package net.minecraft.world.entity.ai.goal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;

public class LandOnOwnersShoulderGoal extends Goal {
   private final ShoulderRidingEntity entity;
   private boolean isSittingOnShoulder;

   public LandOnOwnersShoulderGoal(final ShoulderRidingEntity entity) {
      this.entity = entity;
   }

   @Override
   public boolean canUse() {
      if (!(this.entity.getOwner() instanceof ServerPlayer owner)) {
         return false;
      } else {
         boolean ownerThatCanBeSatOn = !owner.isSpectator() && !owner.getAbilities().flying && !owner.isInWater() && !owner.isInPowderSnow;
         return !this.entity.isOrderedToSit() && ownerThatCanBeSatOn && this.entity.canSitOnShoulder();
      }
   }

   @Override
   public boolean isInterruptable() {
      return !this.isSittingOnShoulder;
   }

   @Override
   public void start() {
      this.isSittingOnShoulder = false;
   }

   @Override
   public void tick() {
      if (!this.isSittingOnShoulder && !this.entity.isInSittingPose() && !this.entity.isLeashed()) {
         if (this.entity.getOwner() instanceof ServerPlayer owner && this.entity.getBoundingBox().intersects(owner.getBoundingBox())) {
            this.isSittingOnShoulder = this.entity.setEntityOnShoulder(owner);
         }
      }
   }
}
