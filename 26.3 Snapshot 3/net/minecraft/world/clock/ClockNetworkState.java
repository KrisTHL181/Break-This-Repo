package net.minecraft.world.clock;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ClockNetworkState(long totalTicks, float partialTick, float rate) {
   public static final StreamCodec<ByteBuf, ClockNetworkState> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_LONG,
      ClockNetworkState::totalTicks,
      ByteBufCodecs.FLOAT,
      ClockNetworkState::partialTick,
      ByteBufCodecs.FLOAT,
      ClockNetworkState::rate,
      ClockNetworkState::new
   );
}
