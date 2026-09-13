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

   private ClientboundDeleteChatPacket(final FriendlyByteBuf input) {
      this(MessageSignature.Packed.read(input));
   }

   private void write(final FriendlyByteBuf output) {
      MessageSignature.Packed.write(output, this.messageSignature);
   }

   @Override
   public PacketType<ClientboundDeleteChatPacket> type() {
      return GamePacketTypes.CLIENTBOUND_DELETE_CHAT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleDeleteChat(this);
   }
}
