package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.Nullable;

public record ClientboundPlayerChatPacket(
   int globalIndex,
   UUID sender,
   int index,
   @Nullable MessageSignature signature,
   SignedMessageBody.Packed body,
   @Nullable Component unsignedContent,
   FilterMask filterMask,
   ChatType.Bound chatType
) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPlayerChatPacket> STREAM_CODEC = Packet.codec(
      ClientboundPlayerChatPacket::write, ClientboundPlayerChatPacket::new
   );

   private ClientboundPlayerChatPacket(RegistryFriendlyByteBuf p_329037_) {
      this(
         p_329037_.readVarInt(),
         p_329037_.readUUID(),
         p_329037_.readVarInt(),
         p_329037_.readNullable(MessageSignature::read),
         new SignedMessageBody.Packed(p_329037_),
         FriendlyByteBuf.readNullable(p_329037_, ComponentSerialization.TRUSTED_STREAM_CODEC),
         FilterMask.read(p_329037_),
         ChatType.Bound.STREAM_CODEC.decode(p_329037_)
      );
   }

   private void write(RegistryFriendlyByteBuf p_329687_) {
      p_329687_.writeVarInt(this.globalIndex);
      p_329687_.writeUUID(this.sender);
      p_329687_.writeVarInt(this.index);
      p_329687_.writeNullable(this.signature, MessageSignature::write);
      this.body.write(p_329687_);
      FriendlyByteBuf.writeNullable(p_329687_, this.unsignedContent, ComponentSerialization.TRUSTED_STREAM_CODEC);
      FilterMask.write(p_329687_, this.filterMask);
      ChatType.Bound.STREAM_CODEC.encode(p_329687_, this.chatType);
   }

   @Override
   public PacketType<ClientboundPlayerChatPacket> type() {
      return GamePacketTypes.CLIENTBOUND_PLAYER_CHAT;
   }

   public void handle(ClientGamePacketListener p_237759_) {
      p_237759_.handlePlayerChat(this);
   }

   @Override
   public boolean isSkippable() {
      return true;
   }
}
