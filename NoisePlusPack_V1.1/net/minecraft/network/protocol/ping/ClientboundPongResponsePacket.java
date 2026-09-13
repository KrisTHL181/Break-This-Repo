package net.minecraft.network.protocol.ping;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundPongResponsePacket(long time) implements Packet<ClientPongPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPongResponsePacket> STREAM_CODEC = Packet.codec(
      ClientboundPongResponsePacket::write, ClientboundPongResponsePacket::new
   );

   private ClientboundPongResponsePacket(FriendlyByteBuf p_334575_) {
      this(p_334575_.readLong());
   }

   private void write(FriendlyByteBuf p_335126_) {
      p_335126_.writeLong(this.time);
   }

   @Override
   public PacketType<ClientboundPongResponsePacket> type() {
      return PingPacketTypes.CLIENTBOUND_PONG_RESPONSE;
   }

   public void handle(ClientPongPacketListener p_332635_) {
      p_332635_.handlePongResponse(this);
   }
}
