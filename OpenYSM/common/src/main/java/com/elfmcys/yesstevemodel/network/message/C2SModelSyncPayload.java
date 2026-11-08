package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.util.YSMThreadPool;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.nio.ByteBuffer;
import java.util.UUID;

public class C2SModelSyncPayload {

    private final ByteBuffer data;

    public C2SModelSyncPayload(ByteBuffer data) {
        this.data = data;
    }

    public static void encode(C2SModelSyncPayload message, FriendlyByteBuf buf) {
        buf.writeBytes(message.data.duplicate());
    }

    public static C2SModelSyncPayload decode(FriendlyByteBuf buf) {
        ByteBuffer data = ByteBuffer.allocateDirect(buf.readableBytes());
        buf.readBytes(data);
        return new C2SModelSyncPayload(data);
    }

    public static void handle(C2SModelSyncPayload message, PacketContext ctx) {
        if (ctx.isServerSide() && ctx.getSender() != null) {
            UUID playerId = ctx.getSender().getUUID();
            YSMThreadPool.submitSync(() -> ServerModelManager.nativeSendModelData(playerId, message.data));
        }
    }
}
