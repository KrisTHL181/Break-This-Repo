package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class VehicleModelComponent implements Component {

    private final VehicleModelCapability capability = new VehicleModelCapability();

    public VehicleModelCapability capability() {
        return capability;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("VehicleModel", Tag.TAG_COMPOUND)) {
            capability.deserializeNBT(tag.getCompound("VehicleModel"));
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("VehicleModel", capability.serializeNBT());
    }
}
