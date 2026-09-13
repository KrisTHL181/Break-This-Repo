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

   private ClientboundCustomChatCompletionsPacket(FriendlyByteBuf p_243340_) {
      this(p_243340_.readEnum(ClientboundCustomChatCompletionsPacket.Action.class), p_243340_.readList(FriendlyByteBuf::readUtf));
   }

   private void write(FriendlyByteBuf p_240782_) {
      p_240782_.writeEnum(this.action);
      p_240782_.writeCollection(this.entries, FriendlyByteBuf::writeUtf);
   }

   @Override
   public PacketType<ClientboundCustomChatCompletionsPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CUSTOM_CHAT_COMPLETIONS;
   }

   public void handle(ClientGamePacketListener p_240794_) {
      p_240794_.handleCustomChatCompletions(this);
   }

   public enum Action {
      ADD,
      REMOVE,
      SET;
   }
}
