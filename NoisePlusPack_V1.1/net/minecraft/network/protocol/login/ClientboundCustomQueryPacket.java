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

   private ClientboundCustomQueryPacket(FriendlyByteBuf p_179810_) {
      this(p_179810_.readVarInt(), readPayload(p_179810_.readIdentifier(), p_179810_));
   }

   private static CustomQueryPayload readPayload(Identifier p_454280_, FriendlyByteBuf p_299332_) {
      return readUnknownPayload(p_454280_, p_299332_);
   }

   private static DiscardedQueryPayload readUnknownPayload(Identifier p_455327_, FriendlyByteBuf p_297706_) {
      int i = p_297706_.readableBytes();
      if (i >= 0 && i <= 1048576) {
         p_297706_.skipBytes(i);
         return new DiscardedQueryPayload(p_455327_);
      } else {
         throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
      }
   }

   private void write(FriendlyByteBuf p_134757_) {
      p_134757_.writeVarInt(this.transactionId);
      p_134757_.writeIdentifier(this.payload.id());
      this.payload.write(p_134757_);
   }

   @Override
   public PacketType<ClientboundCustomQueryPacket> type() {
      return LoginPacketTypes.CLIENTBOUND_CUSTOM_QUERY;
   }

   public void handle(ClientLoginPacketListener p_134754_) {
      p_134754_.handleCustomQuery(this);
   }
}
