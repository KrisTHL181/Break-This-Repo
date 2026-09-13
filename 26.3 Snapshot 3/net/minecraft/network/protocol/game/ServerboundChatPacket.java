package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.Nullable;

public record ServerboundChatPacket(String message, Instant timeStamp, long salt, @Nullable MessageSignature signature, LastSeenMessages.Update lastSeenMessages)
   implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundChatPacket> STREAM_CODEC = Packet.codec(ServerboundChatPacket::write, ServerboundChatPacket::new);

   private ServerboundChatPacket(final FriendlyByteBuf input) {
      this(input.readUtf(256), input.readInstant(), input.readLong(), input.readNullable(MessageSignature::read), new LastSeenMessages.Update(input));
   }

   private void write(final FriendlyByteBuf output) {
      output.writeUtf(this.message, 256);
      output.writeInstant(this.timeStamp);
      output.writeLong(this.salt);
      output.writeNullable(this.signature, MessageSignature::write);
      this.lastSeenMessages.write(output);
   }

   @Override
   public PacketType<ServerboundChatPacket> type() {
      return GamePacketTypes.SERVERBOUND_CHAT;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleChat(this);
   }
}
