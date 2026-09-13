package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundChatCommandSignedPacket(
   String command, Instant timeStamp, long salt, ArgumentSignatures argumentSignatures, LastSeenMessages.Update lastSeenMessages
) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundChatCommandSignedPacket> STREAM_CODEC = Packet.codec(
      ServerboundChatCommandSignedPacket::write, ServerboundChatCommandSignedPacket::new
   );

   private ServerboundChatCommandSignedPacket(FriendlyByteBuf p_333361_) {
      this(p_333361_.readUtf(), p_333361_.readInstant(), p_333361_.readLong(), new ArgumentSignatures(p_333361_), new LastSeenMessages.Update(p_333361_));
   }

   private void write(FriendlyByteBuf p_332640_) {
      p_332640_.writeUtf(this.command);
      p_332640_.writeInstant(this.timeStamp);
      p_332640_.writeLong(this.salt);
      this.argumentSignatures.write(p_332640_);
      this.lastSeenMessages.write(p_332640_);
   }

   @Override
   public PacketType<ServerboundChatCommandSignedPacket> type() {
      return GamePacketTypes.SERVERBOUND_CHAT_COMMAND_SIGNED;
   }

   public void handle(ServerGamePacketListener p_329693_) {
      p_329693_.handleSignedChatCommand(this);
   }
}
