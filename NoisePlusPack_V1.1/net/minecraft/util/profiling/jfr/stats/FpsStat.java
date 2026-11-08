package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record FpsStat(int fps) {
   public static FpsStat from(RecordedEvent p_450778_, String p_460995_) {
      return new FpsStat(p_450778_.getInt(p_460995_));
   }
}
