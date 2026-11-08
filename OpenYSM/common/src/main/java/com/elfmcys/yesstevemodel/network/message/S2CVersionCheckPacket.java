package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public class S2CVersionCheckPacket {

    private static final String OPEN_YSM_BRAND = "open_ysm:v1";
    private static final String MODEL_SYNC_FRAGMENT_BRAND = "open_ysm:model_sync_fragments_v1";

    private final String version;
    private final boolean supportsModelSyncFragments;

    public S2CVersionCheckPacket() {
        this(NetworkHandler.VERSION, true);
    }

    private S2CVersionCheckPacket(String version, boolean supportsModelSyncFragments) {
        this.version = version;
        this.supportsModelSyncFragments = supportsModelSyncFragments;
    }

    public static S2CVersionCheckPacket decode(FriendlyByteBuf buf) {
        String version = buf.readUtf();
        boolean supportsModelSyncFragments = false;
        if(buf.readableBytes() > 0){
            String brand = buf.readUtf();
            if(brand.equals(OPEN_YSM_BRAND)){
                ClientModelManager.setOysmServer(true);
                ClientModelManager.setAllowUpload(buf.readBoolean());
            } else if (brand.equals(MODEL_SYNC_FRAGMENT_BRAND)) {
                supportsModelSyncFragments = true;
            }
        }
        return new S2CVersionCheckPacket(version, supportsModelSyncFragments);
    }

    public static void encode(S2CVersionCheckPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.version);
        if (message.supportsModelSyncFragments) {
            buf.writeUtf(MODEL_SYNC_FRAGMENT_BRAND);
        }
    }

    public static void handle(S2CVersionCheckPacket message, PacketContext ctx) {
        NetworkHandler.setServerSupportsModelSyncFragments(message.supportsModelSyncFragments);
        if (NetworkHandler.setChannelVersion(ctx.getConnection(), message.version)) {
            ctx.enqueueWork(() -> {
                ClientOnlyMode.leaveStandalone();
                ClientModelManager.onSyncConnected();
            });
        }
        if (NetworkHandler.VERSION.equals(message.version)) {
            NetworkHandler.markClientHandshakeComplete();
        }
        NetworkHandler.sendVersionCheck(ctx.getConnection());
    }
}
