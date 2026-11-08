package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

import java.nio.ByteBuffer;

public class S2CModelSyncPayload {

    private final ByteBuffer data;

    public S2CModelSyncPayload(ByteBuffer data) {
        this.data = data;
    }

    public static void encode(S2CModelSyncPayload message, FriendlyByteBuf buf) {
        buf.writeBytes(message.data.duplicate());
    }

    public static S2CModelSyncPayload decode(FriendlyByteBuf buf) {
        ByteBuffer data = ByteBuffer.allocateDirect(buf.readableBytes());
        buf.readBytes(data);
        return new S2CModelSyncPayload(data);
    }

    public static void handle(S2CModelSyncPayload message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> ClientModelManager.startSync(ctx.getConnection(), message.data));
        }
    }
}
