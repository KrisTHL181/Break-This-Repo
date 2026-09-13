package net.minecraft.network.protocol.ping;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundPongResponsePacket(long time) implements Packet<ClientPongPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPongResponsePacket> STREAM_CODEC = Packet.codec(
      ClientboundPongResponsePacket::write, ClientboundPongResponsePacket::new
   );

   private ClientboundPongResponsePacket(final FriendlyByteBuf input) {
      this(input.readLong());
   }

   private void write(final FriendlyByteBuf output) {
      output.writeLong(this.time);
   }

   @Override
   public PacketType<ClientboundPongResponsePacket> type() {
      return PingPacketTypes.CLIENTBOUND_PONG_RESPONSE;
   }

   public void handle(final ClientPongPacketListener listener) {
      listener.handlePongResponse(this);
   }
}
