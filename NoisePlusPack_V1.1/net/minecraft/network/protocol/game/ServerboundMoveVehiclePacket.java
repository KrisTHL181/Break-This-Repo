package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record ServerboundMoveVehiclePacket(Vec3 position, float yRot, float xRot, boolean onGround) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundMoveVehiclePacket> STREAM_CODEC = StreamCodec.composite(
      Vec3.STREAM_CODEC,
      ServerboundMoveVehiclePacket::position,
      ByteBufCodecs.FLOAT,
      ServerboundMoveVehiclePacket::yRot,
      ByteBufCodecs.FLOAT,
      ServerboundMoveVehiclePacket::xRot,
      ByteBufCodecs.BOOL,
      ServerboundMoveVehiclePacket::onGround,
      ServerboundMoveVehiclePacket::new
   );

   public static ServerboundMoveVehiclePacket fromEntity(Entity p_377579_) {
      return p_377579_.isInterpolating()
         ? new ServerboundMoveVehiclePacket(
            p_377579_.getInterpolation().position(), p_377579_.getInterpolation().yRot(), p_377579_.getInterpolation().xRot(), p_377579_.onGround()
         )
         : new ServerboundMoveVehiclePacket(p_377579_.position(), p_377579_.getYRot(), p_377579_.getXRot(), p_377579_.onGround());
   }

   @Override
   public PacketType<ServerboundMoveVehiclePacket> type() {
      return GamePacketTypes.SERVERBOUND_MOVE_VEHICLE;
   }

   public void handle(ServerGamePacketListener p_134198_) {
      p_134198_.handleMoveVehicle(this);
   }
}
