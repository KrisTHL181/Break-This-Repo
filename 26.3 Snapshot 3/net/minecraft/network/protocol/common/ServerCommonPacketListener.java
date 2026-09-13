package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;

public interface ServerCommonPacketListener extends ServerCookiePacketListener {
   void handleKeepAlive(ServerboundKeepAlivePacket packet);

   void handlePong(ServerboundPongPacket serverboundPongPacket);

   void handleCustomPayload(ServerboundCustomPayloadPacket packet);

   void handleResourcePackResponse(ServerboundResourcePackPacket packet);

   void handleClientInformation(ServerboundClientInformationPacket packet);

   void handleCustomClickAction(ServerboundCustomClickActionPacket packet);
}
