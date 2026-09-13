package net.minecraft.util.profiling.metrics.profiling;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;

public class ProfilerSamplerAdapter {
   private final Set<String> previouslyFoundSamplerNames = new ObjectOpenHashSet();

   public Set<MetricSampler> newSamplersFoundInProfiler(final Supplier<ProfileCollector> profiler) {
      Set<MetricSampler> newSamplers = profiler.get()
         .getChartedPaths()
         .stream()
         .filter(pathAndCategory -> !this.previouslyFoundSamplerNames.contains(pathAndCategory.getFirst()))
         .map(pathAndCategory -> samplerForProfilingPath(profiler, (String)pathAndCategory.getFirst(), (MetricCategory)pathAndCategory.getSecond()))
         .collect(Collectors.toSet());

      for (MetricSampler sampler : newSamplers) {
         this.previouslyFoundSamplerNames.add(sampler.getName());
      }

      return newSamplers;
   }

   private static MetricSampler samplerForProfilingPath(final Supplier<ProfileCollector> profiler, final String profilerPath, final MetricCategory category) {
      return MetricSampler.create(profilerPath, category, () -> {
         ActiveProfiler.PathEntry entry = profiler.get().getEntry(profilerPath);
         return entry == null ? 0.0 : (double)entry.getMaxDuration() / TimeUtil.NANOSECONDS_PER_MILLISECOND;
      });
   }
}
