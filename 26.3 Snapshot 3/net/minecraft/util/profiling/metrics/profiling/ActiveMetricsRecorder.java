package net.minecraft.util.profiling.metrics.profiling;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsSamplerProvider;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.profiling.metrics.storage.RecordedDeviation;
import org.jspecify.annotations.Nullable;

public class ActiveMetricsRecorder implements MetricsRecorder {
   public static final int PROFILING_MAX_DURATION_SECONDS = 10;
   private static @Nullable Consumer<Path> globalOnReportFinished = null;
   private final Map<MetricSampler, List<RecordedDeviation>> deviationsBySampler = new Object2ObjectOpenHashMap();
   private final ContinuousProfiler taskProfiler;
   private final Executor ioExecutor;
   private final MetricsPersister metricsPersister;
   private final Consumer<ProfileResults> onProfilingEnd;
   private final Consumer<Path> onReportFinished;
   private final MetricsSamplerProvider metricsSamplerProvider;
   private final LongSupplier wallTimeSource;
   private final long deadlineNano;
   private int currentTick;
   private ProfileCollector singleTickProfiler;
   private volatile boolean killSwitch;
   private Set<MetricSampler> thisTickSamplers = ImmutableSet.of();

   private ActiveMetricsRecorder(
      final MetricsSamplerProvider metricsSamplerProvider,
      final LongSupplier timeSource,
      final Executor ioExecutor,
      final MetricsPersister metricsPersister,
      final Consumer<ProfileResults> onProfilingEnd,
      final Consumer<Path> onReportFinished
   ) {
      this.metricsSamplerProvider = metricsSamplerProvider;
      this.wallTimeSource = timeSource;
      this.taskProfiler = new ContinuousProfiler(timeSource, () -> this.currentTick, () -> false);
      this.ioExecutor = ioExecutor;
      this.metricsPersister = metricsPersister;
      this.onProfilingEnd = onProfilingEnd;
      this.onReportFinished = globalOnReportFinished == null ? onReportFinished : onReportFinished.andThen(globalOnReportFinished);
      this.deadlineNano = timeSource.getAsLong() + TimeUnit.NANOSECONDS.convert(10L, TimeUnit.SECONDS);
      this.singleTickProfiler = new ActiveProfiler(this.wallTimeSource, () -> this.currentTick, () -> true);
      this.taskProfiler.enable();
   }

   public static ActiveMetricsRecorder createStarted(
      final MetricsSamplerProvider metricsSamplerProvider,
      final LongSupplier timeSource,
      final Executor ioExecutor,
      final MetricsPersister metricsPersister,
      final Consumer<ProfileResults> onProfilingEnd,
      final Consumer<Path> onReportFinished
   ) {
      return new ActiveMetricsRecorder(metricsSamplerProvider, timeSource, ioExecutor, metricsPersister, onProfilingEnd, onReportFinished);
   }

   @Override
   public synchronized void end() {
      if (this.isRecording()) {
         this.killSwitch = true;
      }
   }

   @Override
   public synchronized void cancel() {
      if (this.isRecording()) {
         this.singleTickProfiler = InactiveProfiler.INSTANCE;
         this.onProfilingEnd.accept(EmptyProfileResults.EMPTY);
         this.cleanup(this.thisTickSamplers);
      }
   }

   @Override
   public void startTick() {
      this.verifyStarted();
      this.thisTickSamplers = this.metricsSamplerProvider.samplers(() -> this.singleTickProfiler);

      for (MetricSampler sampler : this.thisTickSamplers) {
         sampler.onStartTick();
      }

      this.currentTick++;
   }

   @Override
   public void sampleDuringExtract() {
      this.sample(MetricSampler.SamplingPhase.EXTRACT);
   }

   @Override
   public void endTick() {
      this.sample(MetricSampler.SamplingPhase.END_TICK);
      if (!this.killSwitch && this.wallTimeSource.getAsLong() <= this.deadlineNano) {
         this.singleTickProfiler = new ActiveProfiler(this.wallTimeSource, () -> this.currentTick, () -> true);
      } else {
         this.killSwitch = false;
         ProfileResults results = this.taskProfiler.getResults();
         this.singleTickProfiler = InactiveProfiler.INSTANCE;
         this.onProfilingEnd.accept(results);
         this.scheduleSaveResults(results);
      }
   }

   private void sample(final MetricSampler.SamplingPhase samplingPhase) {
      this.verifyStarted();
      if (this.currentTick != 0) {
         for (MetricSampler sampler : this.thisTickSamplers) {
            if (sampler.samplingPhase() == samplingPhase) {
               sampler.onEndTick(this.currentTick);
               if (sampler.triggersThreshold()) {
                  RecordedDeviation recordedDeviation = new RecordedDeviation(Instant.now(), this.currentTick, this.singleTickProfiler.getResults());
                  this.deviationsBySampler.computeIfAbsent(sampler, ignored -> Lists.newArrayList()).add(recordedDeviation);
               }
            }
         }
      }
   }

   @Override
   public boolean isRecording() {
      return this.taskProfiler.isEnabled();
   }

   @Override
   public ProfilerFiller getProfiler() {
      return ProfilerFiller.combine(this.taskProfiler.getFiller(), this.singleTickProfiler);
   }

   private void verifyStarted() {
      if (!this.isRecording()) {
         throw new IllegalStateException("Not started!");
      }
   }

   private void scheduleSaveResults(final ProfileResults profilerResults) {
      HashSet<MetricSampler> metricSamplers = new HashSet<>(this.thisTickSamplers);
      this.ioExecutor.execute(() -> {
         Path pathToLogs = this.metricsPersister.saveReports(metricSamplers, this.deviationsBySampler, profilerResults);
         this.cleanup(metricSamplers);
         this.onReportFinished.accept(pathToLogs);
      });
   }

   private void cleanup(final Collection<MetricSampler> metricSamplers) {
      for (MetricSampler sampler : metricSamplers) {
         sampler.onFinished();
      }

      this.deviationsBySampler.clear();
      this.taskProfiler.disable();
   }

   public static void registerGlobalCompletionCallback(final Consumer<Path> onFinished) {
      globalOnReportFinished = onFinished;
   }
}
