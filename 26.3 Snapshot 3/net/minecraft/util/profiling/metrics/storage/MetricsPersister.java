package net.minecraft.util.profiling.metrics.storage;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class MetricsPersister {
   public static final Path PROFILING_RESULTS_DIR = Paths.get("debug/profiling");
   public static final String METRICS_DIR_NAME = "metrics";
   public static final String DEVIATIONS_DIR_NAME = "deviations";
   public static final String PROFILING_RESULT_FILENAME = "profiling.txt";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String rootFolderName;

   public MetricsPersister(final String rootFolderName) {
      this.rootFolderName = rootFolderName;
   }

   public Path saveReports(
      final Set<MetricSampler> samplers, final Map<MetricSampler, List<RecordedDeviation>> deviationsBySampler, final ProfileResults profilerResults
   ) {
      try {
         Files.createDirectories(PROFILING_RESULTS_DIR);
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }

      try {
         Path tempDir = Files.createTempDirectory("minecraft-profiling");
         tempDir.toFile().deleteOnExit();
         Files.createDirectories(PROFILING_RESULTS_DIR);
         Path workingDir = tempDir.resolve(this.rootFolderName);
         Path metricsDir = workingDir.resolve("metrics");
         this.saveMetrics(samplers, metricsDir);
         if (!deviationsBySampler.isEmpty()) {
            this.saveDeviations(deviationsBySampler, workingDir.resolve("deviations"));
         }

         this.saveProfilingTaskExecutionResult(profilerResults, workingDir);
         return tempDir;
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   private void saveMetrics(final Set<MetricSampler> samplers, final Path dir) {
      if (samplers.isEmpty()) {
         throw new IllegalArgumentException("Expected at least one sampler to persist");
      }

      Map<MetricCategory, List<MetricSampler>> samplersByCategory = samplers.stream().collect(Collectors.groupingBy(MetricSampler::getCategory));
      samplersByCategory.forEach((category, samplersInCategory) -> this.saveCategory(category, (List<MetricSampler>)samplersInCategory, dir));
   }

   private void saveCategory(final MetricCategory category, final List<MetricSampler> samplers, final Path dir) {
      Path file = dir.resolve(Util.sanitizeName(category.getDescription(), Identifier::validPathChar) + ".csv");
      Writer writer = null;

      try {
         Files.createDirectories(file.getParent());
         writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
         CsvOutput.Builder csvBuilder = CsvOutput.builder();
         csvBuilder.addColumn("@tick");

         for (MetricSampler sampler : samplers) {
            csvBuilder.addColumn(sampler.getName());
         }

         CsvOutput csvOutput = csvBuilder.build(writer);
         List<MetricSampler.SamplerResult> results = samplers.stream().map(MetricSampler::result).collect(Collectors.toList());
         int firstTick = results.stream().mapToInt(MetricSampler.SamplerResult::getFirstTick).summaryStatistics().getMin();
         int lastTick = results.stream().mapToInt(MetricSampler.SamplerResult::getLastTick).summaryStatistics().getMax();

         for (int tick = firstTick; tick <= lastTick; tick++) {
            int finalTick = tick;
            Stream<String> valuesStream = results.stream().map(it -> String.valueOf(it.valueAtTick(finalTick)));
            Object[] row = Stream.concat(Stream.of(String.valueOf(tick)), valuesStream).toArray(String[]::new);
            csvOutput.writeRow(row);
         }

         LOGGER.info("Flushed metrics to {}", file);
      } catch (Exception e) {
         LOGGER.error("Could not save profiler results to {}", file, e);
      } finally {
         IOUtils.closeQuietly(writer);
      }
   }

   private void saveDeviations(final Map<MetricSampler, List<RecordedDeviation>> deviationsBySampler, final Path directory) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS", Locale.UK).withZone(ZoneId.systemDefault());
      deviationsBySampler.forEach(
         (sampler, deviations) -> deviations.forEach(
            deviation -> {
               String timestamp = formatter.format(deviation.timestamp);
               Path deviationLogFile = directory.resolve(Util.sanitizeName(sampler.getName(), Identifier::validPathChar))
                  .resolve(String.format(Locale.ROOT, "%d@%s.txt", deviation.tick, timestamp));
               deviation.profilerResultAtTick.saveResults(deviationLogFile);
            }
         )
      );
   }

   private void saveProfilingTaskExecutionResult(final ProfileResults results, final Path directory) {
      results.saveResults(directory.resolve("profiling.txt"));
   }
}
