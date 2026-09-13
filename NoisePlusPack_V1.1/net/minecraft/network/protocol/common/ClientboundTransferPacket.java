package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundTransferPacket(String host, int port) implements Packet<ClientCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundTransferPacket> STREAM_CODEC = Packet.codec(
      ClientboundTransferPacket::write, ClientboundTransferPacket::new
   );

   private ClientboundTransferPacket(FriendlyByteBuf p_330783_) {
      this(p_330783_.readUtf(), p_330783_.readVarInt());
   }

   private void write(FriendlyByteBuf p_329224_) {
      p_329224_.writeUtf(this.host);
      p_329224_.writeVarInt(this.port);
   }

   @Override
   public PacketType<ClientboundTransferPacket> type() {
      return CommonPacketTypes.CLIENTBOUND_TRANSFER;
   }

   public void handle(ClientCommonPacketListener p_328535_) {
      p_328535_.handleTransfer(this);
   }
}
