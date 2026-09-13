package net.minecraft.world.entity.ai.goal;

import net.minecraft.world.entity.monster.zombie.Zombie;

public class ZombieAttackGoal extends MeleeAttackGoal {
   private final Zombie zombie;
   private int raiseArmTicks;

   public ZombieAttackGoal(final Zombie zombie, final double speedModifier, final boolean trackTarget) {
      super(zombie, speedModifier, trackTarget);
      this.zombie = zombie;
   }

   @Override
   public void start() {
      super.start();
      this.raiseArmTicks = 0;
   }

   @Override
   public void stop() {
      super.stop();
      this.zombie.setAggressive(false);
   }

   @Override
   public void tick() {
      super.tick();
      this.raiseArmTicks++;
      if (this.raiseArmTicks >= 5 && this.getTicksUntilNextAttack() < this.getAttackInterval() / 2) {
         this.zombie.setAggressive(true);
      } else {
         this.zombie.setAggressive(false);
      }
   }
}
