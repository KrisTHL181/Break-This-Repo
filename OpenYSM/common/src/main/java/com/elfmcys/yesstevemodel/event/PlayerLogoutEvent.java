package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import dev.architectury.event.events.common.PlayerEvent;

public final class PlayerLogoutEvent {

    private PlayerLogoutEvent() {
    }

    public static void register() {
        PlayerEvent.PLAYER_QUIT.register(player -> {
            NetworkHandler.clearClientModelSyncFragments(player.getUUID());
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            ServerModelManager.syncModelToPlayer(player.getUUID());
        });
    }
}
