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

   private ServerboundChatPacket(FriendlyByteBuf p_179545_) {
      this(
         p_179545_.readUtf(256),
         p_179545_.readInstant(),
         p_179545_.readLong(),
         p_179545_.readNullable(MessageSignature::read),
         new LastSeenMessages.Update(p_179545_)
      );
   }

   private void write(FriendlyByteBuf p_133839_) {
      p_133839_.writeUtf(this.message, 256);
      p_133839_.writeInstant(this.timeStamp);
      p_133839_.writeLong(this.salt);
      p_133839_.writeNullable(this.signature, MessageSignature::write);
      this.lastSeenMessages.write(p_133839_);
   }

   @Override
   public PacketType<ServerboundChatPacket> type() {
      return GamePacketTypes.SERVERBOUND_CHAT;
   }

   public void handle(ServerGamePacketListener p_133836_) {
      p_133836_.handleChat(this);
   }
}
