package net.minecraft.world.item;

import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class ServerItemCooldowns extends ItemCooldowns {
   private final ServerPlayer player;

   public ServerItemCooldowns(final ServerPlayer player) {
      this.player = player;
   }

   @Override
   protected void onCooldownStarted(final Identifier cooldownGroup, final int duration) {
      super.onCooldownStarted(cooldownGroup, duration);
      this.player.connection.send(new ClientboundCooldownPacket(cooldownGroup, duration));
   }

   @Override
   protected void onCooldownEnded(final Identifier cooldownGroup) {
      super.onCooldownEnded(cooldownGroup);
      this.player.connection.send(new ClientboundCooldownPacket(cooldownGroup, 0));
   }
}
