package net.minecraft.util.debugchart;

public class LocalSampleLogger extends AbstractSampleLogger implements SampleStorage {
   public static final int CAPACITY = 240;
   private final long[][] samples;
   private int start;
   private int size;

   public LocalSampleLogger(int p_334158_) {
      this(p_334158_, new long[p_334158_]);
   }

   public LocalSampleLogger(int p_330975_, long[] p_333573_) {
      super(p_330975_, p_333573_);
      this.samples = new long[240][p_330975_];
   }

   @Override
   protected void useSample() {
      int i = this.wrapIndex(this.start + this.size);
      System.arraycopy(this.sample, 0, this.samples[i], 0, this.sample.length);
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
   public long get(int p_334223_) {
      return this.get(p_334223_, 0);
   }

   @Override
   public long get(int p_335582_, int p_331656_) {
      if (p_335582_ >= 0 && p_335582_ < this.size) {
         long[] along = this.samples[this.wrapIndex(this.start + p_335582_)];
         if (p_331656_ >= 0 && p_331656_ < along.length) {
            return along[p_331656_];
         } else {
            throw new IndexOutOfBoundsException(p_331656_ + " out of bounds for dimensions " + along.length);
         }
      } else {
         throw new IndexOutOfBoundsException(p_335582_ + " out of bounds for length " + this.size);
      }
   }

   private int wrapIndex(int p_330672_) {
      return p_330672_ % 240;
   }

   @Override
   public void reset() {
      this.start = 0;
      this.size = 0;
   }
}
