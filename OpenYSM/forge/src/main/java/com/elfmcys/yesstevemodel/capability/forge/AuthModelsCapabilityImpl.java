package com.elfmcys.yesstevemodel.capability.forge;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.forge.capability.AuthModelsCapabilityProvider;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class AuthModelsCapabilityImpl {

    private AuthModelsCapabilityImpl() {
    }

    public static Optional<AuthModelsCapability> get(Player player) {
        return player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).resolve();
    }
}
