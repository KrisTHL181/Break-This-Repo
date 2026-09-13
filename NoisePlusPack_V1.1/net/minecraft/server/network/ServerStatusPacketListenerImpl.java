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

   public ServerStatusPacketListenerImpl(ServerStatus p_272864_, Connection p_273586_) {
      this.status = p_272864_;
      this.connection = p_273586_;
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_342663_) {
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   public void handleStatusRequest(ServerboundStatusRequestPacket p_10095_) {
      if (this.hasRequestedStatus) {
         this.connection.disconnect(DISCONNECT_REASON);
      } else {
         this.hasRequestedStatus = true;
         this.connection.send(new ClientboundStatusResponsePacket(this.status));
      }
   }

   @Override
   public void handlePingRequest(ServerboundPingRequestPacket p_333596_) {
      this.connection.send(new ClientboundPongResponsePacket(p_333596_.getTime()));
      this.connection.disconnect(DISCONNECT_REASON);
   }
}
