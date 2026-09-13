package net.minecraft.util.debugchart;

public abstract class AbstractSampleLogger implements SampleLogger {
   protected final long[] defaults;
   protected final long[] sample;

   protected AbstractSampleLogger(final int dimensions, final long[] defaults) {
      if (defaults.length != dimensions) {
         throw new IllegalArgumentException("defaults have incorrect length of " + defaults.length);
      }

      this.sample = new long[dimensions];
      this.defaults = defaults;
   }

   @Override
   public void logFullSample(final long[] sample) {
      System.arraycopy(sample, 0, this.sample, 0, sample.length);
      this.useSample();
      this.resetSample();
   }

   @Override
   public void logSample(final long sample) {
      this.sample[0] = sample;
      this.useSample();
      this.resetSample();
   }

   @Override
   public void logPartialSample(final long sample, final int dimension) {
      if (dimension >= 1 && dimension < this.sample.length) {
         this.sample[dimension] = sample;
      } else {
         throw new IndexOutOfBoundsException(dimension + " out of bounds for dimensions " + this.sample.length);
      }
   }

   protected abstract void useSample();

   protected void resetSample() {
      System.arraycopy(this.defaults, 0, this.sample, 0, this.defaults.length);
   }
}
