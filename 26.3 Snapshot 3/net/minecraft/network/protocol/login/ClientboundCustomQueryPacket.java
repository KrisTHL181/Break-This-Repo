package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import net.minecraft.resources.Identifier;

public record ClientboundCustomQueryPacket(int transactionId, CustomQueryPayload payload) implements Packet<ClientLoginPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundCustomQueryPacket> STREAM_CODEC = Packet.codec(
      ClientboundCustomQueryPacket::write, ClientboundCustomQueryPacket::new
   );
   private static final int MAX_PAYLOAD_SIZE = 1048576;

   private ClientboundCustomQueryPacket(final FriendlyByteBuf input) {
      this(input.readVarInt(), readPayload(input.readIdentifier(), input));
   }

   private static CustomQueryPayload readPayload(final Identifier identifier, final FriendlyByteBuf input) {
      return readUnknownPayload(identifier, input);
   }

   private static DiscardedQueryPayload readUnknownPayload(final Identifier identifier, final FriendlyByteBuf input) {
      int length = input.readableBytes();
      if (length >= 0 && length <= 1048576) {
         input.skipBytes(length);
         return new DiscardedQueryPayload(identifier);
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.transactionId);
      output.writeIdentifier(this.payload.id());
      this.payload.write(output);
   }

   @Override
   public PacketType<ClientboundCustomQueryPacket> type() {
      return LoginPacketTypes.CLIENTBOUND_CUSTOM_QUERY;
   }

   public void handle(final ClientLoginPacketListener listener) {
      listener.handleCustomQuery(this);
   }
}
