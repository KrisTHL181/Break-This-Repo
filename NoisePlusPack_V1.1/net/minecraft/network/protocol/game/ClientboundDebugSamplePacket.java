package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.debugchart.RemoteDebugSampleType;

public record ClientboundDebugSamplePacket(long[] sample, RemoteDebugSampleType debugSampleType) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundDebugSamplePacket> STREAM_CODEC = Packet.codec(
      ClientboundDebugSamplePacket::write, ClientboundDebugSamplePacket::new
   );

   private ClientboundDebugSamplePacket(FriendlyByteBuf p_330326_) {
      this(p_330326_.readLongArray(), p_330326_.readEnum(RemoteDebugSampleType.class));
   }

   private void write(FriendlyByteBuf p_330431_) {
      p_330431_.writeLongArray(this.sample);
      p_330431_.writeEnum(this.debugSampleType);
   }

   @Override
   public PacketType<ClientboundDebugSamplePacket> type() {
      return GamePacketTypes.CLIENTBOUND_DEBUG_SAMPLE;
   }

   public void handle(ClientGamePacketListener p_330875_) {
      p_330875_.handleDebugSample(this);
   }
}
