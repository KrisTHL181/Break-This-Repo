package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderWarningDistancePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetBorderWarningDistancePacket> STREAM_CODEC = Packet.codec(
      ClientboundSetBorderWarningDistancePacket::write, ClientboundSetBorderWarningDistancePacket::new
   );
   private final int warningBlocks;

   public ClientboundSetBorderWarningDistancePacket(final WorldBorder border) {
      this.warningBlocks = border.getWarningBlocks();
   }

   private ClientboundSetBorderWarningDistancePacket(final FriendlyByteBuf input) {
      this.warningBlocks = input.readVarInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.warningBlocks);
   }

   @Override
   public PacketType<ClientboundSetBorderWarningDistancePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_BORDER_WARNING_DISTANCE;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetBorderWarningDistance(this);
   }

   public int getWarningBlocks() {
      return this.warningBlocks;
   }
}
