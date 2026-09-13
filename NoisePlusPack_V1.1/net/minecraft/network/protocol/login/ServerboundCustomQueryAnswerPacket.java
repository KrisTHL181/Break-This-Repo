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

   private static ServerboundCustomQueryAnswerPacket read(FriendlyByteBuf p_300962_) {
      int i = p_300962_.readVarInt();
      return new ServerboundCustomQueryAnswerPacket(i, readPayload(i, p_300962_));
   }

   private static CustomQueryAnswerPayload readPayload(int p_298211_, FriendlyByteBuf p_300600_) {
      return readUnknownPayload(p_300600_);
   }

   private static CustomQueryAnswerPayload readUnknownPayload(FriendlyByteBuf p_299934_) {
      int i = p_299934_.readableBytes();
      if (i >= 0 && i <= 1048576) {
         p_299934_.skipBytes(i);
         return DiscardedQueryAnswerPayload.INSTANCE;
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   private void write(FriendlyByteBuf p_299339_) {
      p_299339_.writeVarInt(this.transactionId);
      p_299339_.writeNullable(this.payload, (p_300758_, p_298999_) -> p_298999_.write(p_300758_));
   }

   @Override
   public PacketType<ServerboundCustomQueryAnswerPacket> type() {
      return LoginPacketTypes.SERVERBOUND_CUSTOM_QUERY_ANSWER;
   }

   public void handle(ServerLoginPacketListener p_298492_) {
      p_298492_.handleCustomQueryPacket(this);
   }
}
