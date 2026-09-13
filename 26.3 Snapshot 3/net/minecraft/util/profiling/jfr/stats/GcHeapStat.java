package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;

public record GcHeapStat(Instant timestamp, long heapUsed, GcHeapStat.Timing timing) {
   public static GcHeapStat from(final RecordedEvent event) {
      return new GcHeapStat(
         event.getStartTime(),
         event.getLong("heapUsed"),
         event.getString("when").equalsIgnoreCase("before gc") ? GcHeapStat.Timing.BEFORE_GC : GcHeapStat.Timing.AFTER_GC
      );
   }

   public static GcHeapStat.Summary summary(
      final Duration recordingDuration, final List<GcHeapStat> heapStats, final Duration gcTotalDuration, final int totalGCs
   ) {
      return new GcHeapStat.Summary(recordingDuration, gcTotalDuration, totalGCs, calculateAllocationRatePerSecond(heapStats));
   }

   private static double calculateAllocationRatePerSecond(final List<GcHeapStat> heapStats) {
      long totalAllocations = 0L;
      Map<GcHeapStat.Timing, List<GcHeapStat>> byTiming = heapStats.stream().collect(Collectors.groupingBy(it -> it.timing));
      List<GcHeapStat> beforeGcs = byTiming.get(GcHeapStat.Timing.BEFORE_GC);
      List<GcHeapStat> afterGcs = byTiming.get(GcHeapStat.Timing.AFTER_GC);

      for (int i = 1; i < beforeGcs.size(); i++) {
         GcHeapStat beforeGC = beforeGcs.get(i);
         GcHeapStat previousGC = afterGcs.get(i - 1);
         totalAllocations += beforeGC.heapUsed - previousGC.heapUsed;
      }

      Duration totalDuration = Duration.between(heapStats.get(1).timestamp, heapStats.get(heapStats.size() - 1).timestamp);
      return (double)totalAllocations / totalDuration.getSeconds();
   }

   public record Summary(Duration duration, Duration gcTotalDuration, int totalGCs, double allocationRateBytesPerSecond) {
      public float gcOverHead() {
         return (float)this.gcTotalDuration.toMillis() / (float)this.duration.toMillis();
      }
   }

   public enum Timing {
      BEFORE_GC,
      AFTER_GC;
   }
}
