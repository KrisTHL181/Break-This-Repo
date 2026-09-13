package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionPath;

public record ClientboundEntityPositionSyncPacket(int id, PositionPath position, float yRot, float xRot, boolean onGround)
   implements MovementPacket<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundEntityPositionSyncPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClientboundEntityPositionSyncPacket::id,
      PositionPath.STREAM_CODEC,
      ClientboundEntityPositionSyncPacket::position,
      ByteBufCodecs.FLOAT,
      ClientboundEntityPositionSyncPacket::yRot,
      ByteBufCodecs.FLOAT,
      ClientboundEntityPositionSyncPacket::xRot,
      ByteBufCodecs.BOOL,
      ClientboundEntityPositionSyncPacket::onGround,
      ClientboundEntityPositionSyncPacket::new
   );

   public static ClientboundEntityPositionSyncPacket of(final Entity entity, final PositionPath position) {
      return new ClientboundEntityPositionSyncPacket(entity.getId(), position, entity.getYRot(), entity.getXRot(), entity.onGround());
   }

   public static ClientboundEntityPositionSyncPacket of(final Entity entity) {
      return of(entity, PositionPath.of(entity.trackingPosition()));
   }

   @Override
   public PacketType<ClientboundEntityPositionSyncPacket> type() {
      return GamePacketTypes.CLIENTBOUND_ENTITY_POSITION_SYNC;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleEntityPositionSync(this);
   }

   @Override
   public boolean hasPosition() {
      return true;
   }

   @Override
   public boolean hasRotation() {
      return true;
   }
}
