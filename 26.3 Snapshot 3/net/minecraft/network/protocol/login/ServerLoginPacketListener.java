package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.cookie.ServerCookiePacketListener;

public interface ServerLoginPacketListener extends ServerCookiePacketListener {
   @Override
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.LOGIN;
   }

   void handleHello(ServerboundHelloPacket packet);

   void handleKey(ServerboundKeyPacket packet);

   void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet);

   void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket packet);
}
