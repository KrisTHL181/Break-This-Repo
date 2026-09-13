package net.minecraft.client.renderer;

public class RunningTrimmedMean {
   private final long[] values;
   private int count;
   private int cursor;

   public RunningTrimmedMean(final int maxCount) {
      this.values = new long[maxCount];
   }

   public long registerValueAndGetMean(final long value) {
      if (this.count < this.values.length) {
         this.count++;
      }

      this.values[this.cursor] = value;
      this.cursor = (this.cursor + 1) % this.values.length;
      long min = Long.MAX_VALUE;
      long max = Long.MIN_VALUE;
      long total = 0L;

      for (int i = 0; i < this.count; i++) {
         long current = this.values[i];
         total += current;
         min = Math.min(min, current);
         max = Math.max(max, current);
      }

      if (this.count > 2) {
         total -= min + max;
         return total / (this.count - 2);
      } else {
         return total > 0L ? this.count / total : 0L;
      }
   }
}
