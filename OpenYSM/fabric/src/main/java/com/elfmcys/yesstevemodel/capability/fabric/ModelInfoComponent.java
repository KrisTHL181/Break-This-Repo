package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class ModelInfoComponent implements Component {

    private final ModelInfoCapability capability = new ModelInfoCapability();

    public ModelInfoCapability capability() {
        return capability;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("ModelInfo", Tag.TAG_COMPOUND)) {
            capability.deserializeNBT(tag.getCompound("ModelInfo"));
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("ModelInfo", capability.serializeNBT());
    }
}
