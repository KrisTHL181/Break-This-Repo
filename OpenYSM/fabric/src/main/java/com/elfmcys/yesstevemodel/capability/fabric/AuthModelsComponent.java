package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class AuthModelsComponent implements Component {

    private final AuthModelsCapability capability = new AuthModelsCapability();

    public AuthModelsCapability capability() {
        return capability;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        ListTag list = tag.getList("AuthModels", Tag.TAG_STRING);
        capability.deserializeNBT(list);
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.put("AuthModels", capability.serializeNBT());
    }
}
