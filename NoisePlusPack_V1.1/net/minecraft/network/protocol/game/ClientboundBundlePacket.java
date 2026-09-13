package net.minecraft.network.protocol.game;

import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundBundlePacket extends BundlePacket<ClientGamePacketListener> {
   public ClientboundBundlePacket(Iterable<Packet<? super ClientGamePacketListener>> p_265231_) {
      super(p_265231_);
   }

   @Override
   public PacketType<ClientboundBundlePacket> type() {
      return GamePacketTypes.CLIENTBOUND_BUNDLE;
   }

   public void handle(ClientGamePacketListener p_265490_) {
      p_265490_.handleBundlePacket(this);
   }
}
