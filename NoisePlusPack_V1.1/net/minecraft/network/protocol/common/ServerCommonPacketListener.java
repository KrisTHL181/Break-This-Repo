package net.minecraft.network.protocol.common;

import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;

public interface ServerCommonPacketListener extends ServerCookiePacketListener {
   void handleKeepAlive(ServerboundKeepAlivePacket var1);

   void handlePong(ServerboundPongPacket var1);

   void handleCustomPayload(ServerboundCustomPayloadPacket var1);

   void handleResourcePackResponse(ServerboundResourcePackPacket var1);

   void handleClientInformation(ServerboundClientInformationPacket var1);

   void handleCustomClickAction(ServerboundCustomClickActionPacket var1);
}
