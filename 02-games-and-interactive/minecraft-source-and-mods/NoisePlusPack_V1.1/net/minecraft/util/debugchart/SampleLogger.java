package net.minecraft.util.debugchart;

public interface SampleLogger {
   void logFullSample(long[] var1);

   void logSample(long var1);

   void logPartialSample(long var1, int var3);
}
