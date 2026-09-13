package net.minecraft.network.protocol.ping;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPingRequestPacket implements Packet<ServerPingPacketListener> {
   public static final StreamCodec<ByteBuf, ServerboundPingRequestPacket> STREAM_CODEC = Packet.codec(
      ServerboundPingRequestPacket::write, ServerboundPingRequestPacket::new
   );
   private final long time;

   public ServerboundPingRequestPacket(long p_333024_) {
      this.time = p_333024_;
   }

   private ServerboundPingRequestPacket(ByteBuf p_344424_) {
      this.time = p_344424_.readLong();
   }

   private void write(ByteBuf p_343870_) {
      p_343870_.writeLong(this.time);
   }

   @Override
   public PacketType<ServerboundPingRequestPacket> type() {
      return PingPacketTypes.SERVERBOUND_PING_REQUEST;
   }

   public void handle(ServerPingPacketListener p_336205_) {
      p_336205_.handlePingRequest(this);
   }

   public long getTime() {
      return this.time;
   }
}
