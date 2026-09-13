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

   private ServerboundChatSessionUpdatePacket(FriendlyByteBuf p_254010_) {
      this(RemoteChatSession.Data.read(p_254010_));
   }

   private void write(FriendlyByteBuf p_253690_) {
      RemoteChatSession.Data.write(p_253690_, this.chatSession);
   }

   @Override
   public PacketType<ServerboundChatSessionUpdatePacket> type() {
      return GamePacketTypes.SERVERBOUND_CHAT_SESSION_UPDATE;
   }

   public void handle(ServerGamePacketListener p_253620_) {
      p_253620_.handleChatSessionUpdate(this);
   }
}
