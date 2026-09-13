package net.minecraft.world.entity.ai.goal;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

public class LookAtTradingPlayerGoal extends LookAtPlayerGoal {
   private final AbstractVillager villager;

   public LookAtTradingPlayerGoal(AbstractVillager p_458945_) {
      super(p_458945_, Player.class, 8.0F);
      this.villager = p_458945_;
   }

   @Override
   public boolean canUse() {
      if (this.villager.isTrading()) {
         this.lookAt = this.villager.getTradingPlayer();
         return true;
      } else {
         return false;
      }
   }
}
