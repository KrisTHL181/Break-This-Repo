package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderCenterPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetBorderCenterPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetBorderCenterPacket::write, ClientboundSetBorderCenterPacket::new
   );
   private final double newCenterX;
   private final double newCenterZ;

   public ClientboundSetBorderCenterPacket(final WorldBorder border) {
      this.newCenterX = border.getCenterX();
      this.newCenterZ = border.getCenterZ();
   }

   private ClientboundSetBorderCenterPacket(final FriendlyByteBuf input) {
      this.newCenterX = input.readDouble();
      this.newCenterZ = input.readDouble();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeDouble(this.newCenterX);
      output.writeDouble(this.newCenterZ);
   }

   @Override
   public PacketType<ClientboundSetBorderCenterPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_BORDER_CENTER;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetBorderCenter(this);
   }

   public double getNewCenterZ() {
      return this.newCenterZ;
   }

   public double getNewCenterX() {
      return this.newCenterX;
   }
}
