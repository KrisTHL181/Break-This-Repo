package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundPingPacket implements Packet<ClientCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPingPacket> STREAM_CODEC = Packet.codec(ClientboundPingPacket::write, ClientboundPingPacket::new);
   private final int id;

   public ClientboundPingPacket(int p_298858_) {
      this.id = p_298858_;
   }

   private ClientboundPingPacket(FriendlyByteBuf p_301364_) {
      this.id = p_301364_.readInt();
   }

   private void write(FriendlyByteBuf p_298056_) {
      p_298056_.writeInt(this.id);
   }

   @Override
   public PacketType<ClientboundPingPacket> type() {
      return CommonPacketTypes.CLIENTBOUND_PING;
   }

   public void handle(ClientCommonPacketListener p_299413_) {
      p_299413_.handlePing(this);
   }

   public int getId() {
      return this.id;
   }
}
