package com.mojang.blaze3d.systems;

import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DeviceInfo(
    String name,
    String vendorName,
    String driverInfo,
    boolean isZZeroToOne,
    String backendName,
    float timestampPeriod,
    DeviceLimits limits,
    DeviceFeatures features,
    Set<String> underlyingExtensions,
    HintsAndWorkarounds hintsAndWorkarounds,
    DeviceType type
) {
}