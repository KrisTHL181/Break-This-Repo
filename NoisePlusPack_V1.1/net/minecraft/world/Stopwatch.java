package net.minecraft.world;

public record Stopwatch(long creationTime, long accumulatedElapsedTime) {
   public Stopwatch(long p_457945_) {
      this(p_457945_, 0L);
   }

   public long elapsedMilliseconds(long p_452723_) {
      long i = p_452723_ - this.creationTime;
      return this.accumulatedElapsedTime + i;
   }

   public double elapsedSeconds(long p_458876_) {
      return this.elapsedMilliseconds(p_458876_) / 1000.0;
   }
}
