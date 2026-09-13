package net.minecraft.world.clock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public record ClockState(long totalTicks, float partialTick, float rate, boolean paused) {
   public static final Codec<ClockState> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.LONG.fieldOf("total_ticks").forGetter(ClockState::totalTicks),
            Codec.FLOAT.optionalFieldOf("partial_tick", 0.0F).forGetter(ClockState::partialTick),
            ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("rate", 1.0F).forGetter(ClockState::rate),
            Codec.BOOL.optionalFieldOf("paused", false).forGetter(ClockState::paused)
         )
         .apply(i, ClockState::new)
   );
}
