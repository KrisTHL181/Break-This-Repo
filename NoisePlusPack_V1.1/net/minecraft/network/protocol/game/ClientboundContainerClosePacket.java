package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundContainerClosePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundContainerClosePacket> STREAM_CODEC = Packet.codec(
      ClientboundContainerClosePacket::write, ClientboundContainerClosePacket::new
   );
   private final int containerId;

   public ClientboundContainerClosePacket(int p_131933_) {
      this.containerId = p_131933_;
   }

   private ClientboundContainerClosePacket(FriendlyByteBuf p_178820_) {
      this.containerId = p_178820_.readContainerId();
   }

   private void write(FriendlyByteBuf p_131941_) {
      p_131941_.writeContainerId(this.containerId);
   }

   @Override
   public PacketType<ClientboundContainerClosePacket> type() {
      return GamePacketTypes.CLIENTBOUND_CONTAINER_CLOSE;
   }

   public void handle(ClientGamePacketListener p_131939_) {
      p_131939_.handleContainerClose(this);
   }

   public int getContainerId() {
      return this.containerId;
   }
}
