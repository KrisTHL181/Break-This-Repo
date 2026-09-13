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

   private ServerboundChatCommandSignedPacket(final FriendlyByteBuf input) {
      this(input.readUtf(), input.readInstant(), input.readLong(), new ArgumentSignatures(input), new LastSeenMessages.Update(input));
   }

   private void write(final FriendlyByteBuf output) {
      output.writeUtf(this.command);
      output.writeInstant(this.timeStamp);
      output.writeLong(this.salt);
      this.argumentSignatures.write(output);
      this.lastSeenMessages.write(output);
   }

   @Override
   public PacketType<ServerboundChatCommandSignedPacket> type() {
      return GamePacketTypes.SERVERBOUND_CHAT_COMMAND_SIGNED;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleSignedChatCommand(this);
   }
}
