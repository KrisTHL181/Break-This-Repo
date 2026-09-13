package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record ClientboundMoveMinecartPacket(int entityId, List<NewMinecartBehavior.MinecartStep> lerpSteps) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundMoveMinecartPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClientboundMoveMinecartPacket::entityId,
      NewMinecartBehavior.MinecartStep.STREAM_CODEC.apply(ByteBufCodecs.list()),
      ClientboundMoveMinecartPacket::lerpSteps,
      ClientboundMoveMinecartPacket::new
   );

   @Override
   public PacketType<ClientboundMoveMinecartPacket> type() {
      return GamePacketTypes.CLIENTBOUND_MOVE_MINECART_ALONG_TRACK;
   }

   public void handle(ClientGamePacketListener p_366622_) {
      p_366622_.handleMinecartAlongTrack(this);
   }

   public @Nullable Entity getEntity(Level p_361971_) {
      return p_361971_.getEntity(this.entityId);
   }
}
