package net.minecraft.world.entity.ai.goal;

import net.minecraft.world.entity.Mob;

public class OpenDoorGoal extends DoorInteractGoal {
   private final boolean closeDoor;
   private int forgetTime;

   public OpenDoorGoal(Mob p_25678_, boolean p_25679_) {
      super(p_25678_);
      this.mob = p_25678_;
      this.closeDoor = p_25679_;
   }

   @Override
   public boolean canContinueToUse() {
      return this.closeDoor && this.forgetTime > 0 && super.canContinueToUse();
   }

   @Override
   public void start() {
      this.forgetTime = 20;
      this.setOpen(true);
   }

   @Override
   public void stop() {
      this.setOpen(false);
   }

   @Override
   public void tick() {
      this.forgetTime--;
      super.tick();
   }
}
