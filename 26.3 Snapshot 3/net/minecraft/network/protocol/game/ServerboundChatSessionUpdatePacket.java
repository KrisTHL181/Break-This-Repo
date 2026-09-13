package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundChatSessionUpdatePacket(RemoteChatSession.Data chatSession) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundChatSessionUpdatePacket> STREAM_CODEC = Packet.codec(
      ServerboundChatSessionUpdatePacket::write, ServerboundChatSessionUpdatePacket::new
   );

   private ServerboundChatSessionUpdatePacket(final FriendlyByteBuf input) {
      this(RemoteChatSession.Data.read(input));
   }

   private void write(final FriendlyByteBuf output) {
      RemoteChatSession.Data.write(output, this.chatSession);
   }

   @Override
   public PacketType<ServerboundChatSessionUpdatePacket> type() {
      return GamePacketTypes.SERVERBOUND_CHAT_SESSION_UPDATE;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleChatSessionUpdate(this);
   }
}
