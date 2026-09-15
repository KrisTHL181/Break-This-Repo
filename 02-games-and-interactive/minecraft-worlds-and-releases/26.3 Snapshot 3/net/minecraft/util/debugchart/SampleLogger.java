package net.minecraft.util.debugchart;

public interface SampleLogger {
   void logFullSample(final long[] sample);

   void logSample(final long sample);

   void logPartialSample(final long sample, final int dimension);
}
