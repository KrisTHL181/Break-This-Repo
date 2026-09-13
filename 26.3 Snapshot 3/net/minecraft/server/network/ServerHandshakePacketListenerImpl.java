package net.minecraft.server.network;

import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.minecraft.server.MinecraftServer;

public class ServerHandshakePacketListenerImpl implements ServerHandshakePacketListener {
   private static final Component IGNORE_STATUS_REASON = Component.translatable("disconnect.ignoring_status_request");
   private final MinecraftServer server;
   private final Connection connection;

   public ServerHandshakePacketListenerImpl(final MinecraftServer server, final Connection connection) {
      this.server = server;
      this.connection = connection;
   }

   @Override
   public void handleIntention(final ClientIntentionPacket packet) {
      switch (packet.intention()) {
         case LOGIN:
            this.beginLogin(packet, false);
            break;
         case STATUS:
            ServerStatus status = this.server.getStatus();
            this.connection.setupOutboundProtocol(StatusProtocols.CLIENTBOUND);
            if (this.server.repliesToStatus() && status != null) {
               this.connection.setupInboundProtocol(StatusProtocols.SERVERBOUND, new ServerStatusPacketListenerImpl(status, this.connection));
            } else {
               this.connection.disconnect(IGNORE_STATUS_REASON);
            }
            break;
         case TRANSFER:
            if (!this.server.acceptsTransfers()) {
               this.connection.setupOutboundProtocol(LoginProtocols.CLIENTBOUND);
               Component reason = Component.translatable("multiplayer.disconnect.transfers_disabled");
               this.connection.send(new ClientboundLoginDisconnectPacket(reason));
               this.connection.disconnect(reason);
            } else {
               this.beginLogin(packet, true);
            }
            break;
         default:
            throw new UnsupportedOperationException("Invalid intention " + packet.intention());
      }
   }

   private void beginLogin(final ClientIntentionPacket packet, final boolean transfer) {
      this.connection.setupOutboundProtocol(LoginProtocols.CLIENTBOUND);
      if (packet.protocolVersion() != SharedConstants.getCurrentVersion().protocolVersion()) {
         Component reason;
         if (packet.protocolVersion() < 754) {
            reason = Component.translatable("multiplayer.disconnect.outdated_client", SharedConstants.getCurrentVersion().name());
         } else {
            reason = Component.translatable("multiplayer.disconnect.incompatible", SharedConstants.getCurrentVersion().name());
         }

         this.connection.send(new ClientboundLoginDisconnectPacket(reason));
         this.connection.disconnect(reason);
      } else {
         this.connection.setupInboundProtocol(LoginProtocols.SERVERBOUND, new ServerLoginPacketListenerImpl(this.server, this.connection, transfer));
      }
   }

   @Override
   public void onDisconnect(final DisconnectionDetails details) {
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }
}
