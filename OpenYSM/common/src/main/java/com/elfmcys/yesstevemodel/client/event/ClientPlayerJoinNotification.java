package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class ClientPlayerJoinNotification {

    private static final int HANDSHAKE_PROBE_COUNT = 3;

    private static boolean notified = false;
    private static int handshakeProbeIndex = -1;
    private static int handshakeProbeDelay;
    private static int handshakeElapsed;
    private static int sessionId;

    private ClientPlayerJoinNotification() {
    }

    private static int handshakeTimeoutTicks() {
        if (GeneralConfig.HANDSHAKE_TIMEOUT == null) {
            return 100;
        }
        return Math.max(HANDSHAKE_PROBE_COUNT, (int) Math.round(GeneralConfig.HANDSHAKE_TIMEOUT.get() * 20.0d));
    }

    private static int probeInterval() {
        return Math.max(1, handshakeTimeoutTicks() / (HANDSHAKE_PROBE_COUNT + 1));
    }

    public static void register() {
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(ClientPlayerJoinNotification::onPlayerJoin);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(ClientPlayerJoinNotification::onPlayerQuit);
        ClientTickEvent.CLIENT_PRE.register(ClientPlayerJoinNotification::onClientTick);
    }

    private static void onPlayerJoin(LocalPlayer player) {
        if (notified) {
            return;
        }
        ClientModelManager.runPendingModelCallback();
        notified = true;
        if (!YesSteveModel.isAvailable()) {
            YesSteveModel.sendUnavailableMessage();
            return;
        }
        if (Minecraft.getInstance().isLocalServer()) {
            return;
        }
        if (ClientOnlyMode.isForced()) {
            ClientOnlyMode.activateStandalone();
            return;
        }
        handshakeProbeIndex = 0;
        handshakeProbeDelay = 0;
        handshakeElapsed = 0;
        int currentSession = ++sessionId;
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(60000L);
                Minecraft.getInstance().execute(() -> {
                    if (currentSession != sessionId || ClientOnlyMode.isActive()) {
                        return;
                    }
                    LocalPlayer localPlayer = Minecraft.getInstance().player;
                    if (localPlayer != null && localPlayer.connection.isAcceptingMessages() && !NetworkHandler.isConnectionValid(localPlayer.connection.getConnection())) {
                        localPlayer.sendSystemMessage(Component.translatable("message.yes_steve_model.client.server_not_found"));
                    }
                });
            } catch (InterruptedException ignored) {
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void onClientTick(Minecraft client) {
        ClientModelManager.applyClientOnlySelection();
        if (handshakeProbeIndex < 0 || client.isLocalServer()) {
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || !player.connection.isAcceptingMessages()) {
            return;
        }
        if (NetworkHandler.isConnectionValid(player.connection.getConnection())) {
            handshakeProbeIndex = -1;
            return;
        }
        handshakeElapsed++;
        if (handshakeElapsed >= handshakeTimeoutTicks()) {
            handshakeProbeIndex = -1;
            ClientOnlyMode.activateStandalone();
            return;
        }
        if (handshakeProbeIndex >= HANDSHAKE_PROBE_COUNT) {
            return;
        }
        if (handshakeProbeDelay > 0) {
            handshakeProbeDelay--;
            return;
        }
        NetworkHandler.sendVersionCheck(player.connection.getConnection());
        handshakeProbeIndex++;
        handshakeProbeDelay = probeInterval();
    }

    private static void onPlayerQuit(LocalPlayer player) {
        handshakeProbeIndex = -1;
        sessionId++;
        if (notified) {
            notified = false;
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            ClientModelManager.resetSync();
        }
    }
}
