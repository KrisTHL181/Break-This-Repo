package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.cookie.ClientCookiePacketListener;

public interface ClientLoginPacketListener extends ClientCookiePacketListener {
   @Override
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.LOGIN;
   }

   void handleHello(ClientboundHelloPacket var1);

   void handleLoginFinished(ClientboundLoginFinishedPacket var1);

   void handleDisconnect(ClientboundLoginDisconnectPacket var1);

   void handleCompression(ClientboundLoginCompressionPacket var1);

   void handleCustomQuery(ClientboundCustomQueryPacket var1);
}
