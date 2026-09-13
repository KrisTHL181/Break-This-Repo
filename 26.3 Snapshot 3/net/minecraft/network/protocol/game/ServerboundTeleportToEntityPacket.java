package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class ServerboundTeleportToEntityPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundTeleportToEntityPacket> STREAM_CODEC = Packet.codec(
      ServerboundTeleportToEntityPacket::write, ServerboundTeleportToEntityPacket::new
   );
   private final UUID uuid;

   public ServerboundTeleportToEntityPacket(final UUID uuid) {
      this.uuid = uuid;
   }

   private ServerboundTeleportToEntityPacket(final FriendlyByteBuf input) {
      this.uuid = input.readUUID();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeUUID(this.uuid);
   }

   @Override
   public PacketType<ServerboundTeleportToEntityPacket> type() {
      return GamePacketTypes.SERVERBOUND_TELEPORT_TO_ENTITY;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleTeleportToEntityPacket(this);
   }

   public @Nullable Entity getEntity(final ServerLevel level) {
      return level.getEntity(this.uuid);
   }
}
