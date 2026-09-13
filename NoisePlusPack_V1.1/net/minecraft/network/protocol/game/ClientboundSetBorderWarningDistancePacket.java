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

   public ClientboundSetBorderWarningDistancePacket(WorldBorder p_179267_) {
      this.warningBlocks = p_179267_.getWarningBlocks();
   }

   private ClientboundSetBorderWarningDistancePacket(FriendlyByteBuf p_179269_) {
      this.warningBlocks = p_179269_.readVarInt();
   }

   private void write(FriendlyByteBuf p_179271_) {
      p_179271_.writeVarInt(this.warningBlocks);
   }

   @Override
   public PacketType<ClientboundSetBorderWarningDistancePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_BORDER_WARNING_DISTANCE;
   }

   public void handle(ClientGamePacketListener p_179275_) {
      p_179275_.handleSetBorderWarningDistance(this);
   }

   public int getWarningBlocks() {
      return this.warningBlocks;
   }
}
