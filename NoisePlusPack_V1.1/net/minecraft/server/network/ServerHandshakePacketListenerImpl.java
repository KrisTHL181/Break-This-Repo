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

   public ServerHandshakePacketListenerImpl(MinecraftServer p_9969_, Connection p_9970_) {
      this.server = p_9969_;
      this.connection = p_9970_;
   }

   @Override
   public void handleIntention(ClientIntentionPacket p_9975_) {
      switch (p_9975_.intention()) {
         case LOGIN:
            this.beginLogin(p_9975_, false);
            break;
         case STATUS:
            ServerStatus serverstatus = this.server.getStatus();
            this.connection.setupOutboundProtocol(StatusProtocols.CLIENTBOUND);
            if (this.server.repliesToStatus() && serverstatus != null) {
               this.connection.setupInboundProtocol(StatusProtocols.SERVERBOUND, new ServerStatusPacketListenerImpl(serverstatus, this.connection));
            } else {
               this.connection.disconnect(IGNORE_STATUS_REASON);
            }
            break;
         case TRANSFER:
            if (!this.server.acceptsTransfers()) {
               this.connection.setupOutboundProtocol(LoginProtocols.CLIENTBOUND);
               Component component = Component.translatable("multiplayer.disconnect.transfers_disabled");
               this.connection.send(new ClientboundLoginDisconnectPacket(component));
               this.connection.disconnect(component);
            } else {
               this.beginLogin(p_9975_, true);
            }
            break;
         default:
            throw new UnsupportedOperationException("Invalid intention " + p_9975_.intention());
      }
   }

   private void beginLogin(ClientIntentionPacket p_330592_, boolean p_332714_) {
      this.connection.setupOutboundProtocol(LoginProtocols.CLIENTBOUND);
      if (p_330592_.protocolVersion() != SharedConstants.getCurrentVersion().protocolVersion()) {
         Component component;
         if (p_330592_.protocolVersion() < 754) {
            component = Component.translatable("multiplayer.disconnect.outdated_client", SharedConstants.getCurrentVersion().name());
         } else {
            component = Component.translatable("multiplayer.disconnect.incompatible", SharedConstants.getCurrentVersion().name());
         }

         this.connection.send(new ClientboundLoginDisconnectPacket(component));
         this.connection.disconnect(component);
      } else {
         this.connection.setupInboundProtocol(LoginProtocols.SERVERBOUND, new ServerLoginPacketListenerImpl(this.server, this.connection, p_332714_));
      }
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_344131_) {
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }
}
