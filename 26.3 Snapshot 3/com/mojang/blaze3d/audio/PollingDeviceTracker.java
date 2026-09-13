package com.mojang.blaze3d.audio;

import net.minecraft.util.Util;

public class PollingDeviceTracker extends AbstractDeviceTracker {
   private static final long DEFAULT_DEVICE_CHECK_INTERVAL_MS = 1000L;
   private long lastDeviceCheckTime;

   public PollingDeviceTracker(final DeviceList deviceList) {
      super(deviceList);
   }

   @Override
   protected boolean isUpdateRequested() {
      return Util.getMillis() - this.lastDeviceCheckTime >= 1000L;
   }

   @Override
   protected void discardUpdateRequest() {
      this.lastDeviceCheckTime = Util.getMillis();
   }
}
