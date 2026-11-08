package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.S2CSetModelAndTexturePacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncAuthModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncStarModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CVersionCheckPacket;
import dev.architectury.event.events.common.PlayerEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class EnterServerEvent {

    private EnterServerEvent() {
    }

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            NetworkHandler.sendToClientPlayer(new S2CVersionCheckPacket(), player);
            CapabilityEvent.getModelInfoCap(player).ifPresent(modelInfoCap -> {
                if (!NetworkHandler.isPlayerConnected(player) && !modelInfoCap.isMandatory()) {
                    modelInfoCap.markDirty();
                    return;
                }
                modelInfoCap.stopAnimation(player);
                Optional<S2CSetModelAndTexturePacket> optional = modelInfoCap.createSyncMessage(player, false);
                Consumer<? super S2CSetModelAndTexturePacket> consumer = message -> NetworkHandler.sendToClientPlayer(message, player);
                Objects.requireNonNull(modelInfoCap);
                optional.ifPresentOrElse(consumer, modelInfoCap::markDirty);
            });
            CapabilityEvent.getAuthModelsCap(player).ifPresent(authModelsCap -> {
                for (String modelId : ServerModelManager.getAuthModels()) {
                    authModelsCap.addModel(modelId);
                }
                NetworkHandler.sendToClientPlayer(new S2CSyncAuthModelsPacket(authModelsCap.getAuthModels()), player);
            });
            CapabilityEvent.getStarModelsCap(player).ifPresent(starModelsCap -> NetworkHandler.sendToClientPlayer(new S2CSyncStarModelsPacket(starModelsCap.getStarModels()), player));
        });
    }
}