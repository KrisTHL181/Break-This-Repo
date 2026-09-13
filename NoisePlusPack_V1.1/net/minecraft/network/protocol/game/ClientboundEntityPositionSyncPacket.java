package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;

public record ClientboundEntityPositionSyncPacket(int id, PositionMoveRotation values, boolean onGround) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundEntityPositionSyncPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClientboundEntityPositionSyncPacket::id,
      PositionMoveRotation.STREAM_CODEC,
      ClientboundEntityPositionSyncPacket::values,
      ByteBufCodecs.BOOL,
      ClientboundEntityPositionSyncPacket::onGround,
      ClientboundEntityPositionSyncPacket::new
   );

   public static ClientboundEntityPositionSyncPacket of(Entity p_365521_) {
      return new ClientboundEntityPositionSyncPacket(
         p_365521_.getId(),
         new PositionMoveRotation(p_365521_.trackingPosition(), p_365521_.getDeltaMovement(), p_365521_.getYRot(), p_365521_.getXRot()),
         p_365521_.onGround()
      );
   }

   @Override
   public PacketType<ClientboundEntityPositionSyncPacket> type() {
      return GamePacketTypes.CLIENTBOUND_ENTITY_POSITION_SYNC;
   }

   public void handle(ClientGamePacketListener p_363663_) {
      p_363663_.handleEntityPositionSync(this);
   }
}
