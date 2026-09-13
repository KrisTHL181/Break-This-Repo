package net.minecraft.util.profiling;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.function.LongSupplier;
import net.minecraft.SharedConstants;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SingleTickProfiler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final LongSupplier realTime;
   private final long saveThreshold;
   private int tick;
   private final File location;
   private ProfileCollector profiler = InactiveProfiler.INSTANCE;

   public SingleTickProfiler(final LongSupplier realTime, final String location, final long saveThresholdNs) {
      this.realTime = realTime;
      this.location = new File("debug", location);
      this.saveThreshold = saveThresholdNs;
   }

   public ProfilerFiller startTick() {
      this.profiler = new ActiveProfiler(this.realTime, () -> this.tick, () -> true);
      this.tick++;
      return this.profiler;
   }

   public void endTick() {
      if (this.profiler != InactiveProfiler.INSTANCE) {
         ProfileResults results = this.profiler.getResults();
         this.profiler = InactiveProfiler.INSTANCE;
         if (results.getNanoDuration() >= this.saveThreshold) {
            File file = new File(this.location, "tick-results-" + Util.getFilenameFormattedDateTime() + ".txt");
            results.saveResults(file.toPath());
            LOGGER.info("Recorded long tick -- wrote info to: {}", file.getAbsolutePath());
         }
      }
   }

   public static @Nullable SingleTickProfiler createTickProfiler(final String name) {
      return SharedConstants.DEBUG_MONITOR_TICK_TIMES ? new SingleTickProfiler(Util.timeSource(), name, SharedConstants.MAXIMUM_TICK_TIME_NANOS) : null;
   }

   public static ProfilerFiller decorateFiller(final ProfilerFiller filler, final @Nullable SingleTickProfiler tickProfiler) {
      return tickProfiler != null ? ProfilerFiller.combine(tickProfiler.startTick(), filler) : filler;
   }
}
