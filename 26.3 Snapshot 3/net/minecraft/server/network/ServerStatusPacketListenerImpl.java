package net.minecraft.server.network;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerStatusPacketListener;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;

public class ServerStatusPacketListenerImpl implements ServerStatusPacketListener {
   private static final Component DISCONNECT_REASON = Component.translatable("multiplayer.status.request_handled");
   private final ServerStatus status;
   private final Connection connection;
   private boolean hasRequestedStatus;

   public ServerStatusPacketListenerImpl(final ServerStatus status, final Connection connection) {
      this.status = status;
      this.connection = connection;
   }

   @Override
   public void onDisconnect(final DisconnectionDetails details) {
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   public void handleStatusRequest(final ServerboundStatusRequestPacket packet) {
      if (this.hasRequestedStatus) {
         this.connection.disconnect(DISCONNECT_REASON);
      } else {
         this.hasRequestedStatus = true;
         this.connection.send(new ClientboundStatusResponsePacket(this.status));
      }
   }

   @Override
   public void handlePingRequest(final ServerboundPingRequestPacket packet) {
      this.connection.send(new ClientboundPongResponsePacket(packet.getTime()));
      this.connection.disconnect(DISCONNECT_REASON);
   }
}
