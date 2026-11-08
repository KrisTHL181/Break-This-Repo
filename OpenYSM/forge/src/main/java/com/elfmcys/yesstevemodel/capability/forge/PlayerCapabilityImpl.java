package com.elfmcys.yesstevemodel.capability.forge;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.forge.capability.PlayerCapabilityProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerCapabilityImpl {

    private PlayerCapabilityImpl() {
    }

    public static Optional<PlayerCapability> get(Player player) {
        return player.getCapability(PlayerCapabilityProvider.PLAYER_CAP).resolve();
    }

    public static Optional<PlayerCapability> get(Entity entity) {
        return entity.getCapability(PlayerCapabilityProvider.PLAYER_CAP).resolve();
    }
}
