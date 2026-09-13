package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundContainerClosePacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundContainerClosePacket> STREAM_CODEC = Packet.codec(
      ServerboundContainerClosePacket::write, ServerboundContainerClosePacket::new
   );
   private final int containerId;

   public ServerboundContainerClosePacket(int p_133970_) {
      this.containerId = p_133970_;
   }

   private ServerboundContainerClosePacket(FriendlyByteBuf p_179584_) {
      this.containerId = p_179584_.readContainerId();
   }

   private void write(FriendlyByteBuf p_133978_) {
      p_133978_.writeContainerId(this.containerId);
   }

   @Override
   public PacketType<ServerboundContainerClosePacket> type() {
      return GamePacketTypes.SERVERBOUND_CONTAINER_CLOSE;
   }

   public void handle(ServerGamePacketListener p_133976_) {
      p_133976_.handleContainerClose(this);
   }

   public int getContainerId() {
      return this.containerId;
   }
}
