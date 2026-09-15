package com.mojang.blaze3d.audio;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface DeviceTracker {
    DeviceList currentDevices();

    void tick();

    void forceRefresh();
}