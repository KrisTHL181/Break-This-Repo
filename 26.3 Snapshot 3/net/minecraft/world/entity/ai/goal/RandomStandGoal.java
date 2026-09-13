package net.minecraft.world.entity.ai.goal;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

public class RandomStandGoal extends Goal {
   private final AbstractHorse horse;
   private int nextStand;

   public RandomStandGoal(final AbstractHorse horse) {
      this.horse = horse;
      this.resetStandInterval(horse);
   }

   @Override
   public void start() {
      this.horse.standIfPossible();
      this.playStandSound();
   }

   private void playStandSound() {
      SoundEvent ambientStandSound = this.horse.getAmbientStandSound();
      if (ambientStandSound != null) {
         this.horse.playSound(ambientStandSound);
      }
   }

   @Override
   public boolean canContinueToUse() {
      return false;
   }

   @Override
   public boolean canUse() {
      this.nextStand++;
      if (this.nextStand > 0 && this.horse.getRandom().nextInt(1000) < this.nextStand) {
         this.resetStandInterval(this.horse);
         return !this.horse.isImmobile() && this.horse.getRandom().nextInt(10) == 0;
      } else {
         return false;
      }
   }

   private void resetStandInterval(final AbstractHorse horse) {
      this.nextStand = -horse.getAmbientStandInterval();
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }
}
