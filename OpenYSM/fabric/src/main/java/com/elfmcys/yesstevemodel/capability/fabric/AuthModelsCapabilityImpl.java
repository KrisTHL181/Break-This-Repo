package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.fabric.YsmComponents;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class AuthModelsCapabilityImpl {

    private AuthModelsCapabilityImpl() {
    }

    public static Optional<AuthModelsCapability> get(Player player) {
        AuthModelsComponent component = YsmComponents.AUTH_MODELS.getNullable(player);
        return component == null ? Optional.empty() : Optional.of(component.capability());
    }
}
