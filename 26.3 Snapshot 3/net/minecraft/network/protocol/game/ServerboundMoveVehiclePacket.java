package net.minecraft.network.protocol.game;

import net.minecraft.core.PositionAndRotation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;

public record ServerboundMoveVehiclePacket(PositionAndRotation movingTo, boolean onGround) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundMoveVehiclePacket> STREAM_CODEC = StreamCodec.composite(
      PositionAndRotation.STREAM_CODEC,
      ServerboundMoveVehiclePacket::movingTo,
      ByteBufCodecs.BOOL,
      ServerboundMoveVehiclePacket::onGround,
      ServerboundMoveVehiclePacket::new
   );

   public static ServerboundMoveVehiclePacket fromEntity(final Entity entity) {
      return entity.isInterpolating()
         ? new ServerboundMoveVehiclePacket(entity.getInterpolation().getCurrentPositionAndRotation(), entity.onGround())
         : new ServerboundMoveVehiclePacket(entity.storePositionAndRotation(), entity.onGround());
   }

   @Override
   public PacketType<ServerboundMoveVehiclePacket> type() {
      return GamePacketTypes.SERVERBOUND_MOVE_VEHICLE;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleMoveVehicle(this);
   }
}
