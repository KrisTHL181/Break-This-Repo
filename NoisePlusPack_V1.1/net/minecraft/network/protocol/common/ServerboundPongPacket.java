package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPongPacket implements Packet<ServerCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundPongPacket> STREAM_CODEC = Packet.codec(ServerboundPongPacket::write, ServerboundPongPacket::new);
   private final int id;

   public ServerboundPongPacket(int p_300898_) {
      this.id = p_300898_;
   }

   private ServerboundPongPacket(FriendlyByteBuf p_297786_) {
      this.id = p_297786_.readInt();
   }

   private void write(FriendlyByteBuf p_299986_) {
      p_299986_.writeInt(this.id);
   }

   @Override
   public PacketType<ServerboundPongPacket> type() {
      return CommonPacketTypes.SERVERBOUND_PONG;
   }

   public void handle(ServerCommonPacketListener p_298626_) {
      p_298626_.handlePong(this);
   }

   public int getId() {
      return this.id;
   }
}
