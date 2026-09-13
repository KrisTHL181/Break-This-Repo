package net.minecraft.util.debugchart;

public class LocalSampleLogger extends AbstractSampleLogger implements SampleStorage {
   public static final int CAPACITY = 240;
   private final long[][] samples;
   private int start;
   private int size;

   public LocalSampleLogger(final int dimensions) {
      this(dimensions, new long[dimensions]);
   }

   public LocalSampleLogger(final int dimensions, final long[] defaults) {
      super(dimensions, defaults);
      this.samples = new long[240][dimensions];
   }

   @Override
   protected void useSample() {
      int nextIndex = this.wrapIndex(this.start + this.size);
      System.arraycopy(this.sample, 0, this.samples[nextIndex], 0, this.sample.length);
      if (this.size < 240) {
         this.size++;
      } else {
         this.start = this.wrapIndex(this.start + 1);
      }
   }

   @Override
   public int capacity() {
      return this.samples.length;
   }

   @Override
   public int size() {
      return this.size;
   }

   @Override
   public long get(final int index) {
      return this.get(index, 0);
   }

   @Override
   public long get(final int index, final int dimension) {
      if (index >= 0 && index < this.size) {
         long[] sampleArray = this.samples[this.wrapIndex(this.start + index)];
         if (dimension >= 0 && dimension < sampleArray.length) {
            return sampleArray[dimension];
         } else {
            throw new IndexOutOfBoundsException(dimension + " out of bounds for dimensions " + sampleArray.length);
         }
      } else {
         throw new IndexOutOfBoundsException(index + " out of bounds for length " + this.size);
      }
   }

   private int wrapIndex(final int index) {
      return index % 240;
   }

   @Override
   public void reset() {
      this.start = 0;
      this.size = 0;
   }
}
