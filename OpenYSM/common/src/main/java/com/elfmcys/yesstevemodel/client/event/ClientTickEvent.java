package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.audio.ObjectPool;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.upload.ModelUploadSession;
import com.elfmcys.yesstevemodel.client.upload.UploadManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import rip.ysm.api.PlatformAPI;

public final class ClientTickEvent {

    private static int tickCount;

    private static int refreshRate = 60;

    private ClientTickEvent() {
    }

    public static void register() {
        dev.architectury.event.events.client.ClientTickEvent.CLIENT_PRE.register(ClientTickEvent::onClientPreTick);
    }

    private static void onClientPreTick(Minecraft client) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        tickCount++;
        UploadManager.processPendingUploads();
        ModelUploadSession.tickCurrent();
        ClientModelManager.updateModelLoadingMode();
        ClientModelManager.flushPendingModels();
        ObjectPool.cleanup();
        refreshRate = client.getWindow().getRefreshRate();
        LocalPlayer localPlayer = client.player;
        if (localPlayer != null) {
            PlayerCapability.get(localPlayer).ifPresent(cap -> cap.tickAnimations());
        }
    }

    public static int getTickCount() {
        return tickCount;
    }

    public static int getRefreshRate() {
        return refreshRate;
    }
}
