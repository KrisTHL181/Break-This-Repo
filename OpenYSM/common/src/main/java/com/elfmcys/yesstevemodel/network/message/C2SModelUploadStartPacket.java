package com.elfmcys.yesstevemodel.network.message;

import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public record C2SModelUploadStartPacket(String modelId, int totalBytes, String sha256) {

    public static void encode(C2SModelUploadStartPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.modelId);
        buf.writeVarInt(message.totalBytes);
        buf.writeUtf(message.sha256);
    }

    public static C2SModelUploadStartPacket decode(FriendlyByteBuf buf) {
        return new C2SModelUploadStartPacket(buf.readUtf(), buf.readVarInt(), buf.readUtf());
    }

    public static void handle(C2SModelUploadStartPacket message, PacketContext ctx) {
    }
}
