package net.minecraft.util.profiling.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.ToDoubleFunction;
import org.jspecify.annotations.Nullable;

public class MetricSampler {
   private final String name;
   private final MetricSampler.SamplingPhase samplingPhase;
   private final MetricCategory category;
   private final DoubleSupplier sampler;
   private final ByteBuf ticks;
   private final ByteBuf values;
   private volatile boolean isRunning;
   private final @Nullable Runnable beforeTick;
   final MetricSampler.@Nullable ThresholdTest thresholdTest;
   private double currentValue;

   protected MetricSampler(
      final String name,
      final MetricSampler.SamplingPhase samplingPhase,
      final MetricCategory category,
      final DoubleSupplier sampler,
      final @Nullable Runnable beforeTick,
      final MetricSampler.@Nullable ThresholdTest thresholdTest
   ) {
      this.name = name;
      this.samplingPhase = samplingPhase;
      this.category = category;
      this.beforeTick = beforeTick;
      this.sampler = sampler;
      this.thresholdTest = thresholdTest;
      this.values = ByteBufAllocator.DEFAULT.buffer();
      this.ticks = ByteBufAllocator.DEFAULT.buffer();
      this.isRunning = true;
   }

   public static MetricSampler create(final String name, final MetricCategory category, final DoubleSupplier sampler) {
      return new MetricSampler(name, MetricSampler.SamplingPhase.END_TICK, category, sampler, null, null);
   }

   public static MetricSampler createExtractSampler(final String name, final MetricCategory category, final DoubleSupplier sampler) {
      return new MetricSampler(name, MetricSampler.SamplingPhase.EXTRACT, category, sampler, null, null);
   }

   public static <T> MetricSampler.MetricSamplerBuilder<T> builder(
      final String metricName, final MetricCategory category, final ToDoubleFunction<T> sampler, final T context
   ) {
      if (sampler == null) {
         throw new IllegalStateException();
      } else {
         return new MetricSampler.MetricSamplerBuilder<>(metricName, category, sampler, context);
      }
   }

   public void onStartTick() {
      if (!this.isRunning) {
         throw new IllegalStateException("Not running");
      }

      if (this.beforeTick != null) {
         this.beforeTick.run();
      }
   }

   public void onEndTick(final int currentTick) {
      this.verifyRunning();
      this.currentValue = this.sampler.getAsDouble();
      this.values.writeDouble(this.currentValue);
      this.ticks.writeInt(currentTick);
   }

   public void onFinished() {
      this.verifyRunning();
      this.values.release();
      this.ticks.release();
      this.isRunning = false;
   }

   private void verifyRunning() {
      if (!this.isRunning) {
         throw new IllegalStateException(String.format(Locale.ROOT, "Sampler for metric %s not started!", this.name));
      }
   }

   public DoubleSupplier getSampler() {
      return this.sampler;
   }

   public String getName() {
      return this.name;
   }

   public MetricSampler.SamplingPhase samplingPhase() {
      return this.samplingPhase;
   }

   public MetricCategory getCategory() {
      return this.category;
   }

   public MetricSampler.SamplerResult result() {
      Int2DoubleMap result = new Int2DoubleOpenHashMap();
      int firstTick = Integer.MIN_VALUE;
      int lastTick = Integer.MIN_VALUE;

      while (this.values.isReadable(8)) {
         int currentTick = this.ticks.readInt();
         if (firstTick == Integer.MIN_VALUE) {
            firstTick = currentTick;
         }

         result.put(currentTick, this.values.readDouble());
         lastTick = currentTick;
      }

      return new MetricSampler.SamplerResult(firstTick, lastTick, result);
   }

   public boolean triggersThreshold() {
      return this.thresholdTest != null && this.thresholdTest.test(this.currentValue);
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         MetricSampler that = (MetricSampler)o;
         return this.name.equals(that.name) && this.category.equals(that.category);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.name.hashCode();
   }

   public static class MetricSamplerBuilder<T> {
      private final String name;
      private final MetricCategory category;
      private final DoubleSupplier sampler;
      private final T context;
      private MetricSampler.SamplingPhase samplingPhase = MetricSampler.SamplingPhase.END_TICK;
      private @Nullable Runnable beforeTick;
      private MetricSampler.@Nullable ThresholdTest thresholdTest;

      public MetricSamplerBuilder(final String name, final MetricCategory category, final ToDoubleFunction<T> sampler, final T context) {
         this.name = name;
         this.category = category;
         this.sampler = () -> sampler.applyAsDouble(context);
         this.context = context;
      }

      public MetricSampler.MetricSamplerBuilder<T> withBeforeTick(final Consumer<T> beforeTick) {
         this.beforeTick = () -> beforeTick.accept(this.context);
         return this;
      }

      public MetricSampler.MetricSamplerBuilder<T> withThresholdAlert(final MetricSampler.ThresholdTest thresholdTest) {
         this.thresholdTest = thresholdTest;
         return this;
      }

      public MetricSampler.MetricSamplerBuilder<T> withSamplingPhase(final MetricSampler.SamplingPhase samplingPhase) {
         this.samplingPhase = samplingPhase;
         return this;
      }

      public MetricSampler build() {
         return new MetricSampler(this.name, this.samplingPhase, this.category, this.sampler, this.beforeTick, this.thresholdTest);
      }
   }

   public static class SamplerResult {
      private final Int2DoubleMap recording;
      private final int firstTick;
      private final int lastTick;

      public SamplerResult(final int firstTick, final int lastTick, final Int2DoubleMap recording) {
         this.firstTick = firstTick;
         this.lastTick = lastTick;
         this.recording = recording;
      }

      public double valueAtTick(final int tick) {
         return this.recording.get(tick);
      }

      public int getFirstTick() {
         return this.firstTick;
      }

      public int getLastTick() {
         return this.lastTick;
      }
   }

   public enum SamplingPhase {
      EXTRACT,
      END_TICK;
   }

   public interface ThresholdTest {
      boolean test(final double value);
   }

   public static class ValueIncreasedByPercentage implements MetricSampler.ThresholdTest {
      private final float percentageIncreaseThreshold;
      private double previousValue = Double.MIN_VALUE;

      public ValueIncreasedByPercentage(final float percentageIncreaseThreshold) {
         this.percentageIncreaseThreshold = percentageIncreaseThreshold;
      }

      @Override
      public boolean test(final double value) {
         boolean result;
         if (this.previousValue != Double.MIN_VALUE && !(value <= this.previousValue)) {
            result = (value - this.previousValue) / this.previousValue >= this.percentageIncreaseThreshold;
         } else {
            result = false;
         }

         this.previousValue = value;
         return result;
      }
   }
}
