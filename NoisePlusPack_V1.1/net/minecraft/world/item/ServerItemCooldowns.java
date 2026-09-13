package net.minecraft.world.item;

import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class ServerItemCooldowns extends ItemCooldowns {
   private final ServerPlayer player;

   public ServerItemCooldowns(ServerPlayer p_43067_) {
      this.player = p_43067_;
   }

   @Override
   protected void onCooldownStarted(Identifier p_455097_, int p_43070_) {
      super.onCooldownStarted(p_455097_, p_43070_);
      this.player.connection.send(new ClientboundCooldownPacket(p_455097_, p_43070_));
   }

   @Override
   protected void onCooldownEnded(Identifier p_456476_) {
      super.onCooldownEnded(p_456476_);
      this.player.connection.send(new ClientboundCooldownPacket(p_456476_, 0));
   }
}
