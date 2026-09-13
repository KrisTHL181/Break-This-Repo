package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderLerpSizePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetBorderLerpSizePacket> STREAM_CODEC = Packet.codec(
      ClientboundSetBorderLerpSizePacket::write, ClientboundSetBorderLerpSizePacket::new
   );
   private final double oldSize;
   private final double newSize;
   private final long lerpTime;

   public ClientboundSetBorderLerpSizePacket(final WorldBorder border) {
      this.oldSize = border.getSize();
      this.newSize = border.getLerpTarget();
      this.lerpTime = border.getLerpTime();
   }

   private ClientboundSetBorderLerpSizePacket(final FriendlyByteBuf input) {
      this.oldSize = input.readDouble();
      this.newSize = input.readDouble();
      this.lerpTime = input.readVarLong();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeDouble(this.oldSize);
      output.writeDouble(this.newSize);
      output.writeVarLong(this.lerpTime);
   }

   @Override
   public PacketType<ClientboundSetBorderLerpSizePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_BORDER_LERP_SIZE;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetBorderLerpSize(this);
   }

   public double getOldSize() {
      return this.oldSize;
   }

   public double getNewSize() {
      return this.newSize;
   }

   public long getLerpTime() {
      return this.lerpTime;
   }
}
