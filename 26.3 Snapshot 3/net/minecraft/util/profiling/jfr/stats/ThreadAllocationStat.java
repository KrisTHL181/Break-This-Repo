package net.minecraft.util.profiling.jfr.stats;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.slf4j.Logger;

public record ThreadAllocationStat(Instant timestamp, String threadName, long totalBytes) {
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final String UNKNOWN_THREAD = "unknown";

   public static ThreadAllocationStat from(final RecordedEvent event) {
      RecordedThread recoredThread = event.getThread("thread");
      String threadName = recoredThread == null ? "unknown" : (String)MoreObjects.firstNonNull(recoredThread.getJavaName(), "unknown");
      return new ThreadAllocationStat(event.getStartTime(), threadName, event.getLong("allocated"));
   }

   public static ThreadAllocationStat.Summary summary(final List<ThreadAllocationStat> stats) {
      Map<String, Double> allocationsPerSecondByThread = new TreeMap<>();
      Map<String, List<ThreadAllocationStat>> byThread = stats.stream().collect(Collectors.groupingBy(it -> it.threadName));
      byThread.forEach((thread, threadStats) -> {
         if (threadStats.size() >= 2) {
            ThreadAllocationStat first = threadStats.getFirst();
            ThreadAllocationStat last = threadStats.getLast();
            long duration = Duration.between(first.timestamp, last.timestamp).getSeconds();
            if (duration <= 0L) {
               LOGGER.warn("Thread allocation stat timestamps are not in chronological order for thread {}, skipping it", thread);
            } else {
               long diff = last.totalBytes - first.totalBytes;
               allocationsPerSecondByThread.put(thread, (double)diff / duration);
            }
         }
      });
      return new ThreadAllocationStat.Summary(allocationsPerSecondByThread);
   }

   public record Summary(Map<String, Double> allocationsPerSecondByThread) {
   }
}
