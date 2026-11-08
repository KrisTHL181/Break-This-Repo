package com.elfmcys.yesstevemodel.capability.forge;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.forge.capability.VehicleCapabilityProvider;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class VehicleCapabilityImpl {

    private VehicleCapabilityImpl() {
    }

    public static Optional<VehicleCapability> get(Entity entity) {
        return entity.getCapability(VehicleCapabilityProvider.VEHICLE_CAP).resolve();
    }
}
