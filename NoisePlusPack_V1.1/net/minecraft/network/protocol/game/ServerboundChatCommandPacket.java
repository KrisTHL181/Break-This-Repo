package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundChatCommandPacket(String command) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundChatCommandPacket> STREAM_CODEC = Packet.codec(
      ServerboundChatCommandPacket::write, ServerboundChatCommandPacket::new
   );

   private ServerboundChatCommandPacket(FriendlyByteBuf p_237932_) {
      this(p_237932_.readUtf());
   }

   private void write(FriendlyByteBuf p_237936_) {
      p_237936_.writeUtf(this.command);
   }

   @Override
   public PacketType<ServerboundChatCommandPacket> type() {
      return GamePacketTypes.SERVERBOUND_CHAT_COMMAND;
   }

   public void handle(ServerGamePacketListener p_237940_) {
      p_237940_.handleChatCommand(this);
   }
}
