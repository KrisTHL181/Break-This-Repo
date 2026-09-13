package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundDeleteChatPacket(MessageSignature.Packed messageSignature) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundDeleteChatPacket> STREAM_CODEC = Packet.codec(
      ClientboundDeleteChatPacket::write, ClientboundDeleteChatPacket::new
   );

   private ClientboundDeleteChatPacket(FriendlyByteBuf p_241415_) {
      this(MessageSignature.Packed.read(p_241415_));
   }

   private void write(FriendlyByteBuf p_241358_) {
      MessageSignature.Packed.write(p_241358_, this.messageSignature);
   }

   @Override
   public PacketType<ClientboundDeleteChatPacket> type() {
      return GamePacketTypes.CLIENTBOUND_DELETE_CHAT;
   }

   public void handle(ClientGamePacketListener p_241426_) {
      p_241426_.handleDeleteChat(this);
   }
}
