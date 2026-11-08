package com.mojang.renderpearl.api.device;

import java.util.Set;

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
