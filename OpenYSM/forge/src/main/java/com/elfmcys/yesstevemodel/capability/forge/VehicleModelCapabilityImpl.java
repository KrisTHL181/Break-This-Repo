package com.elfmcys.yesstevemodel.capability.forge;

import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import com.elfmcys.yesstevemodel.forge.capability.VehicleModelCapabilityProvider;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class VehicleModelCapabilityImpl {

    private VehicleModelCapabilityImpl() {
    }

    public static Optional<VehicleModelCapability> get(Entity entity) {
        return entity.getCapability(VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP).resolve();
    }
}
