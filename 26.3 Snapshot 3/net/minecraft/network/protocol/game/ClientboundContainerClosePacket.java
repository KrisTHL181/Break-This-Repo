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

   public ClientboundContainerClosePacket(final int containerId) {
      this.containerId = containerId;
   }

   private ClientboundContainerClosePacket(final FriendlyByteBuf input) {
      this.containerId = input.readContainerId();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeContainerId(this.containerId);
   }

   @Override
   public PacketType<ClientboundContainerClosePacket> type() {
      return GamePacketTypes.CLIENTBOUND_CONTAINER_CLOSE;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleContainerClose(this);
   }

   public int getContainerId() {
      return this.containerId;
   }
}
