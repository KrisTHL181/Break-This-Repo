package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

public class TradeWithPlayerGoal extends Goal {
   private final AbstractVillager mob;

   public TradeWithPlayerGoal(final AbstractVillager mob) {
      this.mob = mob;
      this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (!this.mob.isAlive()) {
         return false;
      }

      if (this.mob.isInWater()) {
         return false;
      }

      if (!this.mob.onGround()) {
         return false;
      }

      if (this.mob.wasHurtRecently()) {
         return false;
      }

      Player trader = this.mob.getTradingPlayer();
      return trader == null ? false : !(this.mob.distanceToSqr(trader) > 16.0);
   }

   @Override
   public void start() {
      this.mob.getNavigation().stop();
   }

   @Override
   public void stop() {
      this.mob.setTradingPlayer(null);
   }
}
