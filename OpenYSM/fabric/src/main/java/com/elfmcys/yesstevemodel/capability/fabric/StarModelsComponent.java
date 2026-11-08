package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import dev.onyxstudios.cca.api.v3.component.Component;

public final class StarModelsComponent implements Component {

    private final StarModelsCapability capability = new StarModelsCapability();

    public StarModelsCapability capability() {
        return capability;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        ListTag list = tag.getList("StarModels", Tag.TAG_STRING);
        capability.deserializeNBT(list);
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("StarModels", capability.serializeNBT());
    }
}
