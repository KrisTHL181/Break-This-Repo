package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundLowDiskSpaceWarningPacket implements Packet<ClientGamePacketListener> {
   public static final ClientboundLowDiskSpaceWarningPacket INSTANCE = new ClientboundLowDiskSpaceWarningPacket();
   public static final StreamCodec<ByteBuf, ClientboundLowDiskSpaceWarningPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   private ClientboundLowDiskSpaceWarningPacket() {
   }

   @Override
   public PacketType<ClientboundLowDiskSpaceWarningPacket> type() {
      return GamePacketTypes.CLIENTBOUND_LOW_DISK_SPACE_WARNING;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleLowDiskSpaceWarning(this);
   }
}
