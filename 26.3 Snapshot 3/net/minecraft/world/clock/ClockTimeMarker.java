package net.minecraft.world.clock;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

public record ClockTimeMarker(Holder<WorldClock> clock, int ticks, Optional<Integer> periodTicks, boolean showInCommands) {
   public static final Codec<ResourceKey<ClockTimeMarker>> KEY_CODEC = ResourceKey.codec(ClockTimeMarkers.ROOT_ID);

   public long getRepetitionCount(final long totalTicks) {
      if (this.periodTicks.isEmpty()) {
         return totalTicks >= this.ticks ? 1L : 0L;
      }

      int periodTicks = this.periodTicks.get();
      return totalTicks / periodTicks + (totalTicks % periodTicks >= this.ticks ? 1 : 0);
   }

   public long resolveTimeToMoveTo(final long totalTicks) {
      if (this.periodTicks.isEmpty()) {
         return this.ticks;
      }

      int periodTicks = this.periodTicks.get();
      return totalTicks + durationToNext(periodTicks, totalTicks % periodTicks, this.ticks);
   }

   public boolean occursAt(final long totalTicks) {
      return this.periodTicks.isEmpty() ? this.ticks == totalTicks : this.ticks == totalTicks % this.periodTicks.get().intValue();
   }

   private static long durationToNext(final int periodTicks, final long from, final long to) {
      long duration = to - from;
      return duration > 0L ? duration : periodTicks + duration;
   }
}
