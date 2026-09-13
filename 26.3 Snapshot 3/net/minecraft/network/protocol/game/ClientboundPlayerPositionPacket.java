package net.minecraft.network.protocol.game;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;

public record ClientboundPlayerPositionPacket(int id, PositionMoveRotation change, Set<Relative> relatives) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerPositionPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClientboundPlayerPositionPacket::id,
      PositionMoveRotation.STREAM_CODEC,
      ClientboundPlayerPositionPacket::change,
      Relative.SET_STREAM_CODEC,
      ClientboundPlayerPositionPacket::relatives,
      ClientboundPlayerPositionPacket::new
   );

   public static ClientboundPlayerPositionPacket of(final int id, final PositionMoveRotation values, final Set<Relative> relatives) {
      return new ClientboundPlayerPositionPacket(id, values, relatives);
   }

   @Override
   public PacketType<ClientboundPlayerPositionPacket> type() {
      return GamePacketTypes.CLIENTBOUND_PLAYER_POSITION;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleMovePlayer(this);
   }
}
