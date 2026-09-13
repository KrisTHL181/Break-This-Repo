package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundEntityTagQueryPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundEntityTagQueryPacket> STREAM_CODEC = Packet.codec(
      ServerboundEntityTagQueryPacket::write, ServerboundEntityTagQueryPacket::new
   );
   private final int transactionId;
   private final int entityId;

   public ServerboundEntityTagQueryPacket(int p_332553_, int p_328823_) {
      this.transactionId = p_332553_;
      this.entityId = p_328823_;
   }

   private ServerboundEntityTagQueryPacket(FriendlyByteBuf p_333986_) {
      this.transactionId = p_333986_.readVarInt();
      this.entityId = p_333986_.readVarInt();
   }

   private void write(FriendlyByteBuf p_333064_) {
      p_333064_.writeVarInt(this.transactionId);
      p_333064_.writeVarInt(this.entityId);
   }

   @Override
   public PacketType<ServerboundEntityTagQueryPacket> type() {
      return GamePacketTypes.SERVERBOUND_ENTITY_TAG_QUERY;
   }

   public void handle(ServerGamePacketListener p_330266_) {
      p_330266_.handleEntityTagQuery(this);
   }

   public int getTransactionId() {
      return this.transactionId;
   }

   public int getEntityId() {
      return this.entityId;
   }
}
