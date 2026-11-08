package rip.ysm.api.network.fabric.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import rip.ysm.api.network.fabric.YSMChannelImpl;

public final class YSMChannelClientImpl {

    private YSMChannelClientImpl() {
    }

    public static void init(ResourceLocation channelId) {
        ClientPlayNetworking.registerGlobalReceiver(channelId, (client, handler, buf, responseSender) -> YSMChannelImpl.dispatch(buf, new ClientPacketContext(client, handler.getConnection())));
    }

    public static void sendToServer(ResourceLocation channelId, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(channelId, buf);
    }

    public static Packet<?> toServerboundPacket(ResourceLocation channelId, FriendlyByteBuf buf) {
        return ClientPlayNetworking.createC2SPacket(channelId, buf);
    }
}
