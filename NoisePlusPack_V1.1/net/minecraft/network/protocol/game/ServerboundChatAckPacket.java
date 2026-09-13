package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundChatAckPacket(int offset) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundChatAckPacket> STREAM_CODEC = Packet.codec(
      ServerboundChatAckPacket::write, ServerboundChatAckPacket::new
   );

   private ServerboundChatAckPacket(FriendlyByteBuf p_242339_) {
      this(p_242339_.readVarInt());
   }

   private void write(FriendlyByteBuf p_242345_) {
      p_242345_.writeVarInt(this.offset);
   }

   @Override
   public PacketType<ServerboundChatAckPacket> type() {
      return GamePacketTypes.SERVERBOUND_CHAT_ACK;
   }

   public void handle(ServerGamePacketListener p_242391_) {
      p_242391_.handleChatAck(this);
   }
}
