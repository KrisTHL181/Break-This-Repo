package net.minecraft.util.profiling;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ActiveProfiler implements ProfileCollector {
   private static final long WARNING_TIME_NANOS = Duration.ofMillis(100L).toNanos();
   private static final Logger LOGGER = LogUtils.getLogger();
   private final List<String> paths = Lists.newArrayList();
   private final LongList startTimes = new LongArrayList();
   private final Map<String, ActiveProfiler.PathEntry> entries = Maps.newHashMap();
   private final IntSupplier getTickTime;
   private final LongSupplier getRealTime;
   private final long startTimeNano;
   private final int startTimeTicks;
   private String path = "";
   private boolean started;
   private ActiveProfiler.@Nullable PathEntry currentEntry;
   private final BooleanSupplier suppressWarnings;
   private final Set<Pair<String, MetricCategory>> chartedPaths = new ObjectArraySet();

   public ActiveProfiler(LongSupplier p_18383_, IntSupplier p_18384_, BooleanSupplier p_397456_) {
      this.startTimeNano = p_18383_.getAsLong();
      this.getRealTime = p_18383_;
      this.startTimeTicks = p_18384_.getAsInt();
      this.getTickTime = p_18384_;
      this.suppressWarnings = p_397456_;
   }

   @Override
   public void startTick() {
      if (this.started) {
         LOGGER.error("Profiler tick already started - missing endTick()?");
      } else {
         this.started = true;
         this.path = "";
         this.paths.clear();
         this.push("root");
      }
   }

   @Override
   public void endTick() {
      if (!this.started) {
         LOGGER.error("Profiler tick already ended - missing startTick()?");
      } else {
         this.pop();
         this.started = false;
         if (!this.path.isEmpty()) {
            LOGGER.error(
               "Profiler tick ended before path was fully popped (remainder: '{}'). Mismatched push/pop?",
               LogUtils.defer(() -> ProfileResults.demanglePath(this.path))
            );
         }
      }
   }

   @Override
   public void push(String p_18390_) {
      if (!this.started) {
         LOGGER.error("Cannot push '{}' to profiler if profiler tick hasn't started - missing startTick()?", p_18390_);
      } else {
         if (!this.path.isEmpty()) {
            this.path = this.path + "\u001e";
         }

         this.path = this.path + p_18390_;
         this.paths.add(this.path);
         this.startTimes.add(Util.getNanos());
         this.currentEntry = null;
      }
   }

   @Override
   public void push(Supplier<String> p_18392_) {
      this.push(p_18392_.get());
   }

   @Override
   public void markForCharting(MetricCategory p_145928_) {
      this.chartedPaths.add(Pair.of(this.path, p_145928_));
   }

   @Override
   public void pop() {
      if (!this.started) {
         LOGGER.error("Cannot pop from profiler if profiler tick hasn't started - missing startTick()?");
      } else if (this.startTimes.isEmpty()) {
         LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
      } else {
         long i = Util.getNanos();
         long j = this.startTimes.removeLong(this.startTimes.size() - 1);
         this.paths.removeLast();
         long k = i - j;
         ActiveProfiler.PathEntry activeprofiler$pathentry = this.getCurrentEntry();
         activeprofiler$pathentry.accumulatedDuration += k;
         activeprofiler$pathentry.count++;
         activeprofiler$pathentry.maxDuration = Math.max(activeprofiler$pathentry.maxDuration, k);
         activeprofiler$pathentry.minDuration = Math.min(activeprofiler$pathentry.minDuration, k);
         if (k > WARNING_TIME_NANOS && !this.suppressWarnings.getAsBoolean()) {
            LOGGER.warn(
               "Something's taking too long! '{}' took aprox {} ms",
               LogUtils.defer(() -> ProfileResults.demanglePath(this.path)),
               LogUtils.defer(() -> k / 1000000.0)
            );
         }

         this.path = this.paths.isEmpty() ? "" : this.paths.getLast();
         this.currentEntry = null;
      }
   }

   @Override
   public void popPush(String p_18395_) {
      this.pop();
      this.push(p_18395_);
   }

   @Override
   public void popPush(Supplier<String> p_18397_) {
      this.pop();
      this.push(p_18397_);
   }

   private ActiveProfiler.PathEntry getCurrentEntry() {
      if (this.currentEntry == null) {
         this.currentEntry = this.entries.computeIfAbsent(this.path, p_18405_ -> new ActiveProfiler.PathEntry());
      }

      return this.currentEntry;
   }

   @Override
   public void incrementCounter(String p_185247_, int p_185248_) {
      this.getCurrentEntry().counters.addTo(p_185247_, p_185248_);
   }

   @Override
   public void incrementCounter(Supplier<String> p_185250_, int p_185251_) {
      this.getCurrentEntry().counters.addTo(p_185250_.get(), p_185251_);
   }

   @Override
   public ProfileResults getResults() {
      return new FilledProfileResults(this.entries, this.startTimeNano, this.startTimeTicks, this.getRealTime.getAsLong(), this.getTickTime.getAsInt());
   }

   @Override
   public ActiveProfiler.@Nullable PathEntry getEntry(String p_145930_) {
      return this.entries.get(p_145930_);
   }

   @Override
   public Set<Pair<String, MetricCategory>> getChartedPaths() {
      return this.chartedPaths;
   }

   public static class PathEntry implements ProfilerPathEntry {
      long maxDuration = Long.MIN_VALUE;
      long minDuration = Long.MAX_VALUE;
      long accumulatedDuration;
      long count;
      final Object2LongOpenHashMap<String> counters = new Object2LongOpenHashMap();

      @Override
      public long getDuration() {
         return this.accumulatedDuration;
      }

      @Override
      public long getMaxDuration() {
         return this.maxDuration;
      }

      @Override
      public long getCount() {
         return this.count;
      }

      @Override
      public Object2LongMap<String> getCounters() {
         return Object2LongMaps.unmodifiable(this.counters);
      }
   }
}
