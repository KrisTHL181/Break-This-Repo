package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundKeepAlivePacket implements Packet<ServerCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundKeepAlivePacket> STREAM_CODEC = Packet.codec(
      ServerboundKeepAlivePacket::write, ServerboundKeepAlivePacket::new
   );
   private final long id;

   public ServerboundKeepAlivePacket(long p_300615_) {
      this.id = p_300615_;
   }

   private ServerboundKeepAlivePacket(FriendlyByteBuf p_299677_) {
      this.id = p_299677_.readLong();
   }

   private void write(FriendlyByteBuf p_299172_) {
      p_299172_.writeLong(this.id);
   }

   @Override
   public PacketType<ServerboundKeepAlivePacket> type() {
      return CommonPacketTypes.SERVERBOUND_KEEP_ALIVE;
   }

   public void handle(ServerCommonPacketListener p_297247_) {
      p_297247_.handleKeepAlive(this);
   }

   public long getId() {
      return this.id;
   }
}
