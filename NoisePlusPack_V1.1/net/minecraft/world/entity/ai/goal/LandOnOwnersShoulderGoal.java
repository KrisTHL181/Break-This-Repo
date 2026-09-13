package net.minecraft.world.entity.ai.goal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;

public class LandOnOwnersShoulderGoal extends Goal {
   private final ShoulderRidingEntity entity;
   private boolean isSittingOnShoulder;

   public LandOnOwnersShoulderGoal(ShoulderRidingEntity p_451156_) {
      this.entity = p_451156_;
   }

   @Override
   public boolean canUse() {
      if (!(this.entity.getOwner() instanceof ServerPlayer serverplayer)) {
         return false;
      } else {
         boolean flag = !serverplayer.isSpectator() && !serverplayer.getAbilities().flying && !serverplayer.isInWater() && !serverplayer.isInPowderSnow;
         return !this.entity.isOrderedToSit() && flag && this.entity.canSitOnShoulder();
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
      if (!this.isSittingOnShoulder
         && !this.entity.isInSittingPose()
         && !this.entity.isLeashed()
         && this.entity.getOwner() instanceof ServerPlayer serverplayer
         && this.entity.getBoundingBox().intersects(serverplayer.getBoundingBox())) {
         this.isSittingOnShoulder = this.entity.setEntityOnShoulder(serverplayer);
      }
   }
}
