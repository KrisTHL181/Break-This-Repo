package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import rip.ysm.api.network.PacketContext;

public class C2SVersionCheckPacket {

    private static final String MODEL_SYNC_FRAGMENT_BRAND = "open_ysm:model_sync_fragments_v1";

    private final String version;
    private final boolean supportsModelSyncFragments;

    public C2SVersionCheckPacket() {
        this(NetworkHandler.VERSION, true);
    }

    public C2SVersionCheckPacket(String version) {
        this(version, false);
    }

    private C2SVersionCheckPacket(String version, boolean supportsModelSyncFragments) {
        this.version = version;
        this.supportsModelSyncFragments = supportsModelSyncFragments;
    }

    public static C2SVersionCheckPacket decode(FriendlyByteBuf buf) {
        String version = buf.readUtf();
        boolean supportsModelSyncFragments = buf.readableBytes() > 0 && MODEL_SYNC_FRAGMENT_BRAND.equals(buf.readUtf());
        return new C2SVersionCheckPacket(version, supportsModelSyncFragments);
    }

    public static void encode(C2SVersionCheckPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.version);
        if (message.supportsModelSyncFragments) {
            buf.writeUtf(MODEL_SYNC_FRAGMENT_BRAND);
        }
    }

    public static void handle(C2SVersionCheckPacket message, PacketContext ctx) {
        ServerPlayer sender = ctx.getSender();
        if (sender != null && NetworkHandler.setChannelVersion(ctx.getConnection(), message.version)) {
            NetworkHandler.setClientSupportsModelSyncFragments(sender.getUUID(), message.supportsModelSyncFragments);
            ServerModelManager.validatePlayerModel(sender);
            ModelInfoCapability.get(sender).ifPresent(cap -> {
                cap.setMandatory(false);
                cap.stopAnimation(sender);
            });
            AuthModelsCapability.get(sender).ifPresent(cap -> {
                NetworkHandler.sendToClientPlayer(new S2CSyncAuthModelsPacket(cap.getAuthModels()), sender);
            });
            StarModelsCapability.get(sender).ifPresent(cap -> {
                NetworkHandler.sendToClientPlayer(new S2CSyncStarModelsPacket(cap.getStarModels()), sender);
            });
            ServerModelManager.requestPlayerAuth(sender, null);
        }
    }
}