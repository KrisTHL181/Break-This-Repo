package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundAcceptTeleportationPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundAcceptTeleportationPacket> STREAM_CODEC = Packet.codec(
      ServerboundAcceptTeleportationPacket::write, ServerboundAcceptTeleportationPacket::new
   );
   private final int id;

   public ServerboundAcceptTeleportationPacket(int p_133788_) {
      this.id = p_133788_;
   }

   private ServerboundAcceptTeleportationPacket(FriendlyByteBuf p_179538_) {
      this.id = p_179538_.readVarInt();
   }

   private void write(FriendlyByteBuf p_133797_) {
      p_133797_.writeVarInt(this.id);
   }

   @Override
   public PacketType<ServerboundAcceptTeleportationPacket> type() {
      return GamePacketTypes.SERVERBOUND_ACCEPT_TELEPORTATION;
   }

   public void handle(ServerGamePacketListener p_133794_) {
      p_133794_.handleAcceptTeleportPacket(this);
   }

   public int getId() {
      return this.id;
   }
}
