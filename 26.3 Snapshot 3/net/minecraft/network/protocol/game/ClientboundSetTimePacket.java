package net.minecraft.network.protocol.game;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;

public record ClientboundSetTimePacket(long gameTime, Map<Holder<WorldClock>, ClockNetworkState> clockUpdates) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSetTimePacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.LONG,
      ClientboundSetTimePacket::gameTime,
      ByteBufCodecs.map(HashMap::new, WorldClock.STREAM_CODEC, ClockNetworkState.STREAM_CODEC),
      ClientboundSetTimePacket::clockUpdates,
      ClientboundSetTimePacket::new
   );

   @Override
   public PacketType<ClientboundSetTimePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_TIME;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetTime(this);
   }
}
