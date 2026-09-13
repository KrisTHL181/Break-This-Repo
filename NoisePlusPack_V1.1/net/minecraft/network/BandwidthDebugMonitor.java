package net.minecraft.network;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.debugchart.LocalSampleLogger;

public class BandwidthDebugMonitor {
   private final AtomicInteger bytesReceived = new AtomicInteger();
   private final LocalSampleLogger bandwidthLogger;

   public BandwidthDebugMonitor(LocalSampleLogger p_335457_) {
      this.bandwidthLogger = p_335457_;
   }

   public void onReceive(int p_300834_) {
      this.bytesReceived.getAndAdd(p_300834_);
   }

   public void tick() {
      this.bandwidthLogger.logSample(this.bytesReceived.getAndSet(0));
   }
}
