package com.elfmcys.yesstevemodel.network.message;

import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public record C2SModelUploadFinishPacket(long uploadId) {

    public static void encode(C2SModelUploadFinishPacket message, FriendlyByteBuf buf) {
        buf.writeVarLong(message.uploadId);
    }

    public static C2SModelUploadFinishPacket decode(FriendlyByteBuf buf) {
        return new C2SModelUploadFinishPacket(buf.readVarLong());
    }

    public static void handle(C2SModelUploadFinishPacket message, PacketContext ctx) {
    }
}
