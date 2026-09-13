package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

public class TradeWithPlayerGoal extends Goal {
   private final AbstractVillager mob;

   public TradeWithPlayerGoal(AbstractVillager p_460152_) {
      this.mob = p_460152_;
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

      if (this.mob.hurtMarked) {
         return false;
      }

      Player player = this.mob.getTradingPlayer();
      return player == null ? false : !(this.mob.distanceToSqr(player) > 16.0);
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
