package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import rip.ysm.api.PlatformAPI;
import rip.ysm.compat.touhoulittlemaid.TouhouMaidCompat;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import dev.architectury.event.events.common.LifecycleEvent;

import java.io.IOException;

public final class CommonEvent {

    private CommonEvent() {
    }

    public static Object nativeInit() {
        try {
            ServerModelManager.reloadPacks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void register() {
        LifecycleEvent.SETUP.register(() -> {
            if (!YesSteveModel.isAvailable()) {
                YesSteveModel.LOGGER.error(YesSteveModel.getErrorMessage());
                return;
            }
            NetworkHandler.init();
            TouhouMaidCompat.init();
            nativeInit();
        });
    }
}