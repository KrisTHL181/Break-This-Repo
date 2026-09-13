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

   public ServerboundTeleportToEntityPacket(UUID p_134680_) {
      this.uuid = p_134680_;
   }

   private ServerboundTeleportToEntityPacket(FriendlyByteBuf p_179794_) {
      this.uuid = p_179794_.readUUID();
   }

   private void write(FriendlyByteBuf p_134690_) {
      p_134690_.writeUUID(this.uuid);
   }

   @Override
   public PacketType<ServerboundTeleportToEntityPacket> type() {
      return GamePacketTypes.SERVERBOUND_TELEPORT_TO_ENTITY;
   }

   public void handle(ServerGamePacketListener p_134688_) {
      p_134688_.handleTeleportToEntityPacket(this);
   }

   public @Nullable Entity getEntity(ServerLevel p_134682_) {
      return p_134682_.getEntity(this.uuid);
   }
}
