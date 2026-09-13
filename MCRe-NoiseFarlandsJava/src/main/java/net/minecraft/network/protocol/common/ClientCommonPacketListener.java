package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.cookie.ClientCookiePacketListener;

public interface ClientCommonPacketListener extends ClientCookiePacketListener {
    void handleKeepAlive(ClientboundKeepAlivePacket packet);

    void handlePing(ClientboundPingPacket packet);

    void handleCustomPayload(ClientboundCustomPayloadPacket packet);

    void handleDisconnect(ClientboundDisconnectPacket packet);

    void handleResourcePackPush(ClientboundResourcePackPushPacket packet);

    void handleResourcePackPop(ClientboundResourcePackPopPacket packet);

    void handleUpdateTags(ClientboundUpdateTagsPacket packet);

    void handleStoreCookie(ClientboundStoreCookiePacket packet);

    void handleTransfer(ClientboundTransferPacket packet);

    void handleCustomReportDetails(ClientboundCustomReportDetailsPacket packet);

    void handleServerLinks(ClientboundServerLinksPacket packet);

    void handleClearDialog(ClientboundClearDialogPacket packet);

    void handleShowDialog(ClientboundShowDialogPacket packet);
}