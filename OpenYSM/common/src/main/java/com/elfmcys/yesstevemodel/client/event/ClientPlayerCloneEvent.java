package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.client.player.LocalPlayer;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.capability.CapabilityLifecycle;

public final class ClientPlayerCloneEvent {

    private ClientPlayerCloneEvent() {
    }

    public static void register() {
        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register(ClientPlayerCloneEvent::onClientPlayerRespawn);
    }

    private static void onClientPlayerRespawn(LocalPlayer oldPlayer, LocalPlayer newPlayer) {
        if (!YesSteveModel.isAvailable() || !NetworkHandler.isClientConnected()) {
            return;
        }
        CapabilityLifecycle.revive(oldPlayer);
        PlayerCapability.get(oldPlayer).ifPresent(cap -> PlayerCapability.get(newPlayer).ifPresent(cap2 -> cap2.copyFrom(cap)));
        CapabilityLifecycle.invalidate(oldPlayer);
    }
}