package net.minecraft.util.profiling.jfr.stats;

import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

public final class IoSummary<T> {
   private final IoSummary.CountAndSize totalCountAndSize;
   private final List<Pair<T, IoSummary.CountAndSize>> largestSizeContributors;
   private final Duration recordingDuration;

   public IoSummary(final Duration recordingDuration, final List<Pair<T, IoSummary.CountAndSize>> packetStats) {
      this.recordingDuration = recordingDuration;
      this.totalCountAndSize = packetStats.stream()
         .<IoSummary.CountAndSize>map(Pair::getSecond)
         .reduce(new IoSummary.CountAndSize(0L, 0L), IoSummary.CountAndSize::add);
      this.largestSizeContributors = packetStats.stream()
         .sorted(Comparator.comparing(Pair::getSecond, IoSummary.CountAndSize.SIZE_THEN_COUNT))
         .limit(10L)
         .toList();
   }

   public double getCountsPerSecond() {
      return (double)this.totalCountAndSize.totalCount / this.recordingDuration.getSeconds();
   }

   public double getSizePerSecond() {
      return (double)this.totalCountAndSize.totalSize / this.recordingDuration.getSeconds();
   }

   public long getTotalCount() {
      return this.totalCountAndSize.totalCount;
   }

   public long getTotalSize() {
      return this.totalCountAndSize.totalSize;
   }

   public List<Pair<T, IoSummary.CountAndSize>> largestSizeContributors() {
      return this.largestSizeContributors;
   }

   public record CountAndSize(long totalCount, long totalSize) {
      private static final Comparator<IoSummary.CountAndSize> SIZE_THEN_COUNT = Comparator.comparing(IoSummary.CountAndSize::totalSize)
         .thenComparing(IoSummary.CountAndSize::totalCount)
         .reversed();

      public IoSummary.CountAndSize add(final IoSummary.CountAndSize that) {
         return new IoSummary.CountAndSize(this.totalCount + that.totalCount, this.totalSize + that.totalSize);
      }

      public float averageSize() {
         return (float)this.totalSize / (float)this.totalCount;
      }
   }
}
