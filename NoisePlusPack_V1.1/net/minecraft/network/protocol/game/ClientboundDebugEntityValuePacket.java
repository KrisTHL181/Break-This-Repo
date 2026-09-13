package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.debug.DebugSubscription;

public record ClientboundDebugEntityValuePacket(int entityId, DebugSubscription.Update<?> update) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDebugEntityValuePacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClientboundDebugEntityValuePacket::entityId,
      DebugSubscription.Update.STREAM_CODEC,
      ClientboundDebugEntityValuePacket::update,
      ClientboundDebugEntityValuePacket::new
   );

   @Override
   public PacketType<ClientboundDebugEntityValuePacket> type() {
      return GamePacketTypes.CLIENTBOUND_DEBUG_ENTITY_VALUE;
   }

   public void handle(ClientGamePacketListener p_431102_) {
      p_431102_.handleDebugEntityValue(this);
   }
}
