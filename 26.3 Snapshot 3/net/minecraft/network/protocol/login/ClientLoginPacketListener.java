package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.cookie.ClientCookiePacketListener;

public interface ClientLoginPacketListener extends ClientCookiePacketListener {
   @Override
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.LOGIN;
   }

   void handleHello(ClientboundHelloPacket packet);

   void handleLoginFinished(ClientboundLoginFinishedPacket packet);

   void handleDisconnect(ClientboundLoginDisconnectPacket packet);

   void handleCompression(ClientboundLoginCompressionPacket packet);

   void handleCustomQuery(ClientboundCustomQueryPacket packet);
}
