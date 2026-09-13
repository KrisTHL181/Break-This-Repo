package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.debug.DebugSubscription;

public record ClientboundDebugEventPacket(DebugSubscription.Event<?> event) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDebugEventPacket> STREAM_CODEC = StreamCodec.composite(
      DebugSubscription.Event.STREAM_CODEC, ClientboundDebugEventPacket::event, ClientboundDebugEventPacket::new
   );

   @Override
   public PacketType<ClientboundDebugEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_DEBUG_EVENT;
   }

   public void handle(ClientGamePacketListener p_425846_) {
      p_425846_.handleDebugEvent(this);
   }
}
