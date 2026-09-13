package net.minecraft.world.entity.ai.goal;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

public class RandomStandGoal extends Goal {
   private final AbstractHorse horse;
   private int nextStand;

   public RandomStandGoal(AbstractHorse p_454732_) {
      this.horse = p_454732_;
      this.resetStandInterval(p_454732_);
   }

   @Override
   public void start() {
      this.horse.standIfPossible();
      this.playStandSound();
   }

   private void playStandSound() {
      SoundEvent soundevent = this.horse.getAmbientStandSound();
      if (soundevent != null) {
         this.horse.playSound(soundevent);
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

   private void resetStandInterval(AbstractHorse p_454566_) {
      this.nextStand = -p_454566_.getAmbientStandInterval();
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }
}
