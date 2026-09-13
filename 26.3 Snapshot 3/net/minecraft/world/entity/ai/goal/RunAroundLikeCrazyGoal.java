package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class RunAroundLikeCrazyGoal extends Goal {
   private final AbstractHorse horse;
   private final double speedModifier;
   private double posX;
   private double posY;
   private double posZ;

   public RunAroundLikeCrazyGoal(final AbstractHorse mob, final double speedModifier) {
      this.horse = mob;
      this.speedModifier = speedModifier;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (!this.horse.isMobControlled() && !this.horse.isTamed() && this.horse.isVehicle()) {
         Vec3 pos = DefaultRandomPos.getPos(this.horse, 5, 4);
         if (pos == null) {
            return false;
         }

         this.posX = pos.x;
         this.posY = pos.y;
         this.posZ = pos.z;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void start() {
      this.horse.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
   }

   @Override
   public boolean canContinueToUse() {
      return !this.horse.isTamed() && !this.horse.getNavigation().isDone() && this.horse.isVehicle();
   }

   @Override
   public void tick() {
      if (!this.horse.isTamed() && this.horse.getRandom().nextInt(this.adjustedTickDelay(50)) == 0) {
         Entity passenger = this.horse.getFirstPassenger();
         if (passenger == null) {
            return;
         }

         if (passenger instanceof Player player) {
            int temper = this.horse.getTemper();
            int maxTemper = this.horse.getMaxTemper();
            if (maxTemper > 0 && this.horse.getRandom().nextInt(maxTemper) < temper) {
               this.horse.tameWithName(player);
               return;
            }

            this.horse.modifyTemper(5);
         }

         this.horse.ejectPassengers();
         this.horse.makeMad();
         this.horse.level().broadcastEntityEvent(this.horse, (byte)6);
      }
   }
}
