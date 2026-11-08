package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import rip.ysm.api.network.PacketContext;

public class C2SRequestSwitchModelPacket {

    private final String modelId;

    private final String textureId;

    public C2SRequestSwitchModelPacket(String modelId, String textureId) {
        this.modelId = modelId;
        this.textureId = textureId;
    }

    public static void encode(C2SRequestSwitchModelPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.modelId);
        buf.writeUtf(message.textureId);
    }

    public static C2SRequestSwitchModelPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestSwitchModelPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(C2SRequestSwitchModelPacket message, PacketContext ctx) {
        if (ctx.isServerSide()) {
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null && ServerConfig.CAN_SWITCH_MODEL.get()) {
                    handleCapability(message, sender);
                }
            });
        }
    }

    private static void handleCapability(C2SRequestSwitchModelPacket message, ServerPlayer sender) {
        ModelInfoCapability.get(sender).ifPresent(cap -> {
            AuthModelsCapability.get(sender).ifPresent(cap2 -> {
                String str = message.modelId;
                if (!ServerModelManager.getServerModelInfo().containsKey(str) || ((ServerModelManager.getAuthModels().contains(str) && !cap2.containsModel(message.modelId)) || !ServerModelManager.getServerModelInfo().get(str).getModelInfo().getTextures().contains(message.textureId))) {
                    cap.resetToDefault();
                } else {
                    cap.setModelAndTexture(message.modelId, message.textureId);
                }
                cap.stopAnimation(sender);
            });
        });
    }
}