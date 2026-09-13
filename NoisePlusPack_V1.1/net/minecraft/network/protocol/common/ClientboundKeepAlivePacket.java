package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundKeepAlivePacket implements Packet<ClientCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundKeepAlivePacket> STREAM_CODEC = Packet.codec(
      ClientboundKeepAlivePacket::write, ClientboundKeepAlivePacket::new
   );
   private final long id;

   public ClientboundKeepAlivePacket(long p_300888_) {
      this.id = p_300888_;
   }

   private ClientboundKeepAlivePacket(FriendlyByteBuf p_300278_) {
      this.id = p_300278_.readLong();
   }

   private void write(FriendlyByteBuf p_299560_) {
      p_299560_.writeLong(this.id);
   }

   @Override
   public PacketType<ClientboundKeepAlivePacket> type() {
      return CommonPacketTypes.CLIENTBOUND_KEEP_ALIVE;
   }

   public void handle(ClientCommonPacketListener p_297897_) {
      p_297897_.handleKeepAlive(this);
   }

   public long getId() {
      return this.id;
   }
}
