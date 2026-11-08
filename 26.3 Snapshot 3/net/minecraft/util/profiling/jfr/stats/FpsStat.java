package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record FpsStat(int fps) {
   public static FpsStat from(final RecordedEvent event, final String field) {
      return new FpsStat(event.getInt(field));
   }
}
