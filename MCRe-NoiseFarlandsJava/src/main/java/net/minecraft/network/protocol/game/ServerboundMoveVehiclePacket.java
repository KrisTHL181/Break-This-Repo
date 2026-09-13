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

    public static ServerboundMoveVehiclePacket fromEntity(final Entity entity) {
        return entity.isInterpolating()
            ? new ServerboundMoveVehiclePacket(
                entity.getInterpolation().position(), entity.getInterpolation().yRot(), entity.getInterpolation().xRot(), entity.onGround()
            )
            : new ServerboundMoveVehiclePacket(entity.position(), entity.getYRot(), entity.getXRot(), entity.onGround());
    }

    @Override
    public PacketType<ServerboundMoveVehiclePacket> type() {
        return GamePacketTypes.SERVERBOUND_MOVE_VEHICLE;
    }

    public void handle(final ServerGamePacketListener listener) {
        listener.handleMoveVehicle(this);
    }
}