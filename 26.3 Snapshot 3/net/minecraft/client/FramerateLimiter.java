package net.minecraft.client;

import java.util.concurrent.locks.LockSupport;

public class FramerateLimiter {
   private static final double OVERSHOOT_SMOOTHING = 0.1;
   private static final long MAX_CURRENT_OVERSHOOT_NS = 25000000L;
   private static final long MAX_AVERAGE_OVERSHOOT_NS = 2000000L;
   private static final long ONE_SECOND_IN_NANOSECONDS = 1000000000L;
   private static final long SPIN_SAFETY_BUFFER_NS = 500000L;
   private static long lastFrameTime = System.nanoTime();
   private static long averageOvershootNs = 0L;
   private static int lastFramerateLimit;

   public static void limitDisplayFPS(final int framerateLimit) {
      long targetTimePerFrame = 1000000000L / framerateLimit;
      long targetTimeNs = lastFrameTime + targetTimePerFrame;
      if (framerateLimit != lastFramerateLimit) {
         averageOvershootNs = 0L;
         lastFramerateLimit = framerateLimit;
      }

      long remainingTimeNs;
      while ((remainingTimeNs = targetTimeNs - System.nanoTime()) > 0L) {
         if (remainingTimeNs > averageOvershootNs + 500000L) {
            long sleepStartTimeNs = System.nanoTime();
            long expectedSleepTimeNs = remainingTimeNs - averageOvershootNs - 500000L;
            if (!Thread.interrupted()) {
               LockSupport.parkNanos(expectedSleepTimeNs);
               long sleepDurationNs = System.nanoTime() - sleepStartTimeNs;
               long currentOvershootNs = sleepDurationNs - expectedSleepTimeNs;
               if (currentOvershootNs > 0L && currentOvershootNs < 25000000L) {
                  averageOvershootNs = (long)(0.1 * currentOvershootNs + 0.9 * averageOvershootNs);
                  averageOvershootNs = Math.min(averageOvershootNs, 2000000L);
               }
            }
         } else {
            Thread.onSpinWait();
         }
      }

      lastFrameTime = System.nanoTime();
   }
}
