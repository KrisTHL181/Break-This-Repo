package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class ProjectileModelComponent implements Component {

    private final ProjectileModelCapability capability = new ProjectileModelCapability();

    public ProjectileModelCapability capability() {
        return capability;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("ProjectileModel", Tag.TAG_COMPOUND)) {
            capability.deserializeNBT(tag.getCompound("ProjectileModel"));
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("ProjectileModel", capability.serializeNBT());
    }
}
