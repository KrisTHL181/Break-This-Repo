package com.mojang.blaze3d.audio;

public interface DeviceTracker {
   DeviceList currentDevices();

   void tick();

   void forceRefresh();
}
