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

   private ClientboundDebugSamplePacket(final FriendlyByteBuf input) {
      this(input.readLongArray(), input.readEnum(RemoteDebugSampleType.class));
   }

   private void write(final FriendlyByteBuf output) {
      output.writeLongArray(this.sample);
      output.writeEnum(this.debugSampleType);
   }

   @Override
   public PacketType<ClientboundDebugSamplePacket> type() {
      return GamePacketTypes.CLIENTBOUND_DEBUG_SAMPLE;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleDebugSample(this);
   }
}
