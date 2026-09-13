package net.minecraft.util.profiling;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ReportType;
import net.minecraft.SharedConstants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

public class FilledProfileResults implements ProfileResults {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ProfilerPathEntry EMPTY = new ProfilerPathEntry() {
      @Override
      public long getDuration() {
         return 0L;
      }

      @Override
      public long getMaxDuration() {
         return 0L;
      }

      @Override
      public long getCount() {
         return 0L;
      }

      @Override
      public Object2LongMap<String> getCounters() {
         return Object2LongMaps.emptyMap();
      }
   };
   private static final Splitter SPLITTER = Splitter.on('\u001e');
   private static final Comparator<Entry<String, FilledProfileResults.CounterCollector>> COUNTER_ENTRY_COMPARATOR = Entry.<String, FilledProfileResults.CounterCollector>comparingByValue(
         Comparator.comparingLong(c -> c.totalValue)
      )
      .reversed();
   private final Map<String, ? extends ProfilerPathEntry> entries;
   private final long startTimeNano;
   private final int startTimeTicks;
   private final long endTimeNano;
   private final int endTimeTicks;
   private final int tickDuration;

   public FilledProfileResults(
      final Map<String, ? extends ProfilerPathEntry> entries,
      final long startTimeNano,
      final int startTimeTicks,
      final long endTimeNano,
      final int endTimeTicks
   ) {
      this.entries = entries;
      this.startTimeNano = startTimeNano;
      this.startTimeTicks = startTimeTicks;
      this.endTimeNano = endTimeNano;
      this.endTimeTicks = endTimeTicks;
      this.tickDuration = endTimeTicks - startTimeTicks;
   }

   private ProfilerPathEntry getEntry(final String path) {
      ProfilerPathEntry result = this.entries.get(path);
      return result != null ? result : EMPTY;
   }

   @Override
   public List<ResultField> getTimes(String path) {
      String rawPath = path;
      ProfilerPathEntry rootEntry = this.getEntry("root");
      long globalTime = rootEntry.getDuration();
      ProfilerPathEntry currentEntry = this.getEntry(path);
      long selfTime = currentEntry.getDuration();
      long selfCount = currentEntry.getCount();
      List<ResultField> result = Lists.newArrayList();
      if (!path.isEmpty()) {
         path = path + "\u001e";
      }

      long totalTime = 0L;

      for (String key : this.entries.keySet()) {
         if (isDirectChild(path, key)) {
            totalTime += this.getEntry(key).getDuration();
         }
      }

      float oldTime = (float)totalTime;
      if (totalTime < selfTime) {
         totalTime = selfTime;
      }

      if (globalTime < totalTime) {
         globalTime = totalTime;
      }

      for (String key : this.entries.keySet()) {
         if (isDirectChild(path, key)) {
            ProfilerPathEntry entry = this.getEntry(key);
            long time = entry.getDuration();
            double timePercentage = time * 100.0 / totalTime;
            double globalPercentage = time * 100.0 / globalTime;
            String name = key.substring(path.length());
            result.add(new ResultField(name, timePercentage, globalPercentage, entry.getCount()));
         }
      }

      if ((float)totalTime > oldTime) {
         result.add(
            new ResultField("unspecified", ((float)totalTime - oldTime) * 100.0 / totalTime, ((float)totalTime - oldTime) * 100.0 / globalTime, selfCount)
         );
      }

      Collections.sort(result);
      result.add(0, new ResultField(rawPath, 100.0, totalTime * 100.0 / globalTime, selfCount));
      return result;
   }

   private static boolean isDirectChild(final String path, final String test) {
      return test.length() > path.length() && test.startsWith(path) && test.indexOf(30, path.length() + 1) < 0;
   }

   private Map<String, FilledProfileResults.CounterCollector> getCounterValues() {
      Map<String, FilledProfileResults.CounterCollector> result = Maps.newTreeMap();
      this.entries
         .forEach(
            (path, entry) -> {
               Object2LongMap<String> counters = entry.getCounters();
               if (!counters.isEmpty()) {
                  List<String> pathSegments = SPLITTER.splitToList(path);
                  counters.forEach(
                     (counter, value) -> result.computeIfAbsent(counter, k -> new FilledProfileResults.CounterCollector())
                        .addValue(pathSegments.iterator(), value)
                  );
               }
            }
         );
      return result;
   }

   @Override
   public long getStartTimeNano() {
      return this.startTimeNano;
   }

   @Override
   public int getStartTimeTicks() {
      return this.startTimeTicks;
   }

   @Override
   public long getEndTimeNano() {
      return this.endTimeNano;
   }

   @Override
   public int getEndTimeTicks() {
      return this.endTimeTicks;
   }

   @Override
   public boolean saveResults(final Path file) {
      Writer writer = null;

      try {
         Files.createDirectories(file.getParent());
         writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
         writer.write(this.getProfilerResults(this.getNanoDuration(), this.getTickDuration()));
         return true;
      } catch (Throwable t) {
         LOGGER.error("Could not save profiler results to {}", file, t);
         return false;
      } finally {
         IOUtils.closeQuietly(writer);
      }
   }

   protected String getProfilerResults(final long timespan, final int tickspan) {
      StringBuilder builder = new StringBuilder();
      ReportType.PROFILE.appendHeader(builder, List.of());
      builder.append("Version: ").append(SharedConstants.getCurrentVersion().id()).append('\n');
      builder.append("Time span: ").append(timespan / 1000000L).append(" ms\n");
      builder.append("Tick span: ").append(tickspan).append(" ticks\n");
      builder.append("// This is approximately ")
         .append(String.format(Locale.ROOT, "%.2f", tickspan / ((float)timespan / 1.0E9F)))
         .append(" ticks per second. It should be ")
         .append(20)
         .append(" ticks per second\n\n");
      builder.append("--- BEGIN PROFILE DUMP ---\n\n");
      this.appendProfilerResults(0, "root", builder);
      builder.append("--- END PROFILE DUMP ---\n\n");
      Map<String, FilledProfileResults.CounterCollector> counters = this.getCounterValues();
      if (!counters.isEmpty()) {
         builder.append("--- BEGIN COUNTER DUMP ---\n\n");
         this.appendCounters(counters, builder, tickspan);
         builder.append("--- END COUNTER DUMP ---\n\n");
      }

      return builder.toString();
   }

   @Override
   public String getProfilerResults() {
      StringBuilder builder = new StringBuilder();
      this.appendProfilerResults(0, "root", builder);
      return builder.toString();
   }

   private static StringBuilder indentLine(final StringBuilder builder, final int depth) {
      builder.append(String.format(Locale.ROOT, "[%02d] ", depth));

      for (int j = 0; j < depth; j++) {
         builder.append("|   ");
      }

      return builder;
   }

   private void appendProfilerResults(final int depth, final String path, final StringBuilder builder) {
      List<ResultField> results = this.getTimes(path);
      Object2LongMap<String> counters = ((ProfilerPathEntry)ObjectUtils.firstNonNull(new ProfilerPathEntry[]{this.entries.get(path), EMPTY})).getCounters();
      counters.forEach(
         (id, value) -> indentLine(builder, depth).append('#').append(id).append(' ').append(value).append('/').append(value / this.tickDuration).append('\n')
      );
      if (results.size() >= 3) {
         for (int i = 1; i < results.size(); i++) {
            ResultField result = results.get(i);
            indentLine(builder, depth)
               .append(result.name)
               .append('(')
               .append(result.count)
               .append('/')
               .append(String.format(Locale.ROOT, "%.0f", (float)result.count / this.tickDuration))
               .append(')')
               .append(" - ")
               .append(String.format(Locale.ROOT, "%.2f", result.percentage))
               .append("%/")
               .append(String.format(Locale.ROOT, "%.2f", result.globalPercentage))
               .append("%\n");
            if (!"unspecified".equals(result.name)) {
               try {
                  this.appendProfilerResults(depth + 1, path + "\u001e" + result.name, builder);
               } catch (Exception e) {
                  builder.append("[[ EXCEPTION ").append(e).append(" ]]");
               }
            }
         }
      }
   }

   private void appendCounterResults(
      final int depth, final String name, final FilledProfileResults.CounterCollector result, final int tickspan, final StringBuilder builder
   ) {
      indentLine(builder, depth)
         .append(name)
         .append(" total:")
         .append(result.selfValue)
         .append('/')
         .append(result.totalValue)
         .append(" average: ")
         .append(result.selfValue / tickspan)
         .append('/')
         .append(result.totalValue / tickspan)
         .append('\n');
      result.children
         .entrySet()
         .stream()
         .sorted(COUNTER_ENTRY_COMPARATOR)
         .forEach(e -> this.appendCounterResults(depth + 1, e.getKey(), e.getValue(), tickspan, builder));
   }

   private void appendCounters(final Map<String, FilledProfileResults.CounterCollector> counters, final StringBuilder builder, final int tickspan) {
      counters.forEach((counter, counterRoot) -> {
         builder.append("-- Counter: ").append(counter).append(" --\n");
         this.appendCounterResults(0, "root", counterRoot.children.get("root"), tickspan, builder);
         builder.append("\n\n");
      });
   }

   @Override
   public int getTickDuration() {
      return this.tickDuration;
   }

   private static class CounterCollector {
      private long selfValue;
      private long totalValue;
      private final Map<String, FilledProfileResults.CounterCollector> children = Maps.newHashMap();

      public void addValue(final Iterator<String> path, final long value) {
         this.totalValue += value;
         if (!path.hasNext()) {
            this.selfValue += value;
         } else {
            this.children.computeIfAbsent(path.next(), k -> new FilledProfileResults.CounterCollector()).addValue(path, value);
         }
      }
   }
}
