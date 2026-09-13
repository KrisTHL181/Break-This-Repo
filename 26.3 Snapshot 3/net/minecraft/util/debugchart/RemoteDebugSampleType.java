package net.minecraft.util.debugchart;

import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;

public enum RemoteDebugSampleType {
   TICK_TIME(DebugSubscriptions.DEDICATED_SERVER_TICK_TIME);

   private final DebugSubscription<?> subscription;

   RemoteDebugSampleType(final DebugSubscription<?> subscription) {
      this.subscription = subscription;
   }

   public DebugSubscription<?> subscription() {
      return this.subscription;
   }
}
