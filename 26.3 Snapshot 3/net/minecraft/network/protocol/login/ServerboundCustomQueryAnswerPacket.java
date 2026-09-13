package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryAnswerPayload;
import org.jspecify.annotations.Nullable;

public record ServerboundCustomQueryAnswerPacket(int transactionId, @Nullable CustomQueryAnswerPayload payload) implements Packet<ServerLoginPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundCustomQueryAnswerPacket> STREAM_CODEC = Packet.codec(
      ServerboundCustomQueryAnswerPacket::write, ServerboundCustomQueryAnswerPacket::read
   );
   private static final int MAX_PAYLOAD_SIZE = 1048576;

   private static ServerboundCustomQueryAnswerPacket read(final FriendlyByteBuf input) {
      int transactionId = input.readVarInt();
      return new ServerboundCustomQueryAnswerPacket(transactionId, readPayload(transactionId, input));
   }

   private static CustomQueryAnswerPayload readPayload(final int transactionId, final FriendlyByteBuf input) {
      return readUnknownPayload(input);
   }

   private static CustomQueryAnswerPayload readUnknownPayload(final FriendlyByteBuf input) {
      int length = input.readableBytes();
      if (length >= 0 && length <= 1048576) {
         input.skipBytes(length);
         return DiscardedQueryAnswerPayload.INSTANCE;
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.transactionId);
      output.writeNullable(this.payload, (buf, data) -> data.write(buf));
   }

   @Override
   public PacketType<ServerboundCustomQueryAnswerPacket> type() {
      return LoginPacketTypes.SERVERBOUND_CUSTOM_QUERY_ANSWER;
   }

   public void handle(final ServerLoginPacketListener listener) {
      listener.handleCustomQueryPacket(this);
   }
}
