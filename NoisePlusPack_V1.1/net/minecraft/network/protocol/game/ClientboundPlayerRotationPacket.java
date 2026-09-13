package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundPlayerRotationPacket(float yRot, boolean relativeY, float xRot, boolean relativeX) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerRotationPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      ClientboundPlayerRotationPacket::yRot,
      ByteBufCodecs.BOOL,
      ClientboundPlayerRotationPacket::relativeY,
      ByteBufCodecs.FLOAT,
      ClientboundPlayerRotationPacket::xRot,
      ByteBufCodecs.BOOL,
      ClientboundPlayerRotationPacket::relativeX,
      ClientboundPlayerRotationPacket::new
   );

   @Override
   public PacketType<ClientboundPlayerRotationPacket> type() {
      return GamePacketTypes.CLIENTBOUND_PLAYER_ROTATION;
   }

   public void handle(ClientGamePacketListener p_367955_) {
      p_367955_.handleRotatePlayer(this);
   }
}
