package net.minecraft.util.debugchart;

import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;

public enum RemoteDebugSampleType {
   TICK_TIME(DebugSubscriptions.DEDICATED_SERVER_TICK_TIME);

   private final DebugSubscription<?> subscription;

   RemoteDebugSampleType(final DebugSubscription<?> p_422975_) {
      this.subscription = p_422975_;
   }

   public DebugSubscription<?> subscription() {
      return this.subscription;
   }
}
