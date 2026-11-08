package com.elfmcys.yesstevemodel.client;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ClientOnlyMode {

    private static volatile boolean standalone = false;
    private static volatile boolean catalogLoaded = false;

    private ClientOnlyMode() {
    }

    public static boolean isForced() {
        return GeneralConfig.FORCE_CLIENT_MODE != null && GeneralConfig.FORCE_CLIENT_MODE.get();
    }

    public static boolean isActive() {
        return standalone || isForced();
    }

    public static void activateStandalone() {
        if (standalone) {
            return;
        }
        standalone = true;
        YesSteveModel.LOGGER.info("[YSM] No server-side mod detected, entering client-only mode.");
        ClientModelManager.enterClientOnlyMode();
    }

    public static void leaveStandalone() {
        if (!standalone || isForced()) {
            return;
        }
        standalone = false;
        YesSteveModel.LOGGER.info("[YSM] Server-side mod responded late, leaving client-only mode.");
    }

    public static void reset() {
        standalone = false;
        catalogLoaded = false;
    }

    public static boolean markCatalogLoaded() {
        if (catalogLoaded) {
            return false;
        }
        catalogLoaded = true;
        return true;
    }
}
