package net.minecraft.world.entity.ai.goal;

import net.minecraft.world.entity.Mob;

public class OpenDoorGoal extends DoorInteractGoal {
   private final boolean closeDoor;
   private int forgetTime;

   public OpenDoorGoal(final Mob mob, final boolean closeDoorAfter) {
      super(mob);
      this.mob = mob;
      this.closeDoor = closeDoorAfter;
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
