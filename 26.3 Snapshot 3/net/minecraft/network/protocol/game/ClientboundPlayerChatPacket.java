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

   private ClientboundPlayerChatPacket(final RegistryFriendlyByteBuf input) {
      this(
         input.readVarInt(),
         input.readUUID(),
         input.readVarInt(),
         input.readNullable(MessageSignature::read),
         new SignedMessageBody.Packed(input),
         FriendlyByteBuf.readNullable(input, ComponentSerialization.TRUSTED_STREAM_CODEC),
         FilterMask.read(input),
         ChatType.Bound.STREAM_CODEC.decode(input)
      );
   }

   private void write(final RegistryFriendlyByteBuf output) {
      output.writeVarInt(this.globalIndex);
      output.writeUUID(this.sender);
      output.writeVarInt(this.index);
      output.writeNullable(this.signature, MessageSignature::write);
      this.body.write(output);
      FriendlyByteBuf.writeNullable(output, this.unsignedContent, ComponentSerialization.TRUSTED_STREAM_CODEC);
      FilterMask.write(output, this.filterMask);
      ChatType.Bound.STREAM_CODEC.encode(output, this.chatType);
   }

   @Override
   public PacketType<ClientboundPlayerChatPacket> type() {
      return GamePacketTypes.CLIENTBOUND_PLAYER_CHAT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handlePlayerChat(this);
   }

   @Override
   public boolean isSkippable() {
      return true;
   }
}
