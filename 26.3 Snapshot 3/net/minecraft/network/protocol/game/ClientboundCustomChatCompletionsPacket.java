package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundCustomChatCompletionsPacket(ClientboundCustomChatCompletionsPacket.Action action, List<String> entries)
   implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundCustomChatCompletionsPacket> STREAM_CODEC = Packet.codec(
      ClientboundCustomChatCompletionsPacket::write, ClientboundCustomChatCompletionsPacket::new
   );

   private ClientboundCustomChatCompletionsPacket(final FriendlyByteBuf input) {
      this(input.readEnum(ClientboundCustomChatCompletionsPacket.Action.class), input.readList(FriendlyByteBuf::readUtf));
   }

   private void write(final FriendlyByteBuf output) {
      output.writeEnum(this.action);
      output.writeCollection(this.entries, FriendlyByteBuf::writeUtf);
   }

   @Override
   public PacketType<ClientboundCustomChatCompletionsPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CUSTOM_CHAT_COMPLETIONS;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleCustomChatCompletions(this);
   }

   public enum Action {
      ADD,
      REMOVE,
      SET;
   }
}
