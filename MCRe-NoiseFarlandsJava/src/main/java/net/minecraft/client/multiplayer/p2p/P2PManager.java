package net.minecraft.client.multiplayer.p2p;

import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.p2p.client.SignalingServiceClient;
import net.minecraft.client.network.webrtc.RtcHandshake;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class P2PManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Duration SIGNALING_RECONNECT_MAX_DELAY = Duration.ofSeconds(30L);
   private final Minecraft minecraft;
   private final SignalingServiceClient signaling;
   private final FriendJoinHandler friendJoinHandler;
   private final RtcHandshakeHandler rtcHandshakeHandler;
   private final RandomSource random = RandomSource.create();
   private int signalingReconnectAttempts;
   private volatile boolean shutdown;

   public P2PManager(final Minecraft minecraft, final User user) {
      this.minecraft = minecraft;
      this.signaling = new SignalingServiceClient(user);
      this.friendJoinHandler = new FriendJoinHandler(minecraft, this.signaling, this);
      this.rtcHandshakeHandler = new RtcHandshakeHandler(minecraft, this.signaling, this);
      this.signaling.addConnectionListener(new SignalingServiceClient.ConnectionListener() {
         @Override
         public void onSignalingConnected() {
            P2PManager.this.signalingReconnectAttempts = 0;
         }

         @Override
         public void onSignalingDisconnected() {
            P2PManager.this.reconnectSignaling();
         }

         @Override
         public void onSignalingConnectFailed() {
            P2PManager.this.reconnectSignaling();
         }
      });
   }

   private void reconnectSignaling() {
      if (!this.shutdown && this.needsSignaling()) {
         long delayMs = this.signalingReconnectDelayMs(this.signalingReconnectAttempts++);
         LOGGER.warn("Signaling unavailable while still needed, retry #{} in {}ms", this.signalingReconnectAttempts, delayMs);
         CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> {
            if (!this.shutdown && this.needsSignaling()) {
               this.ensureSignalingConnected();
            }
         });
      } else {
         this.signalingReconnectAttempts = 0;
      }
   }

   private long signalingReconnectDelayMs(final int attempt) {
      long capMs = SIGNALING_RECONNECT_MAX_DELAY.toMillis();
      long backoffMs = Math.min(capMs, TimeUnit.SECONDS.toMillis(1L << Math.min(attempt, 30)));
      return this.random.nextInt((int)backoffMs);
   }

   private boolean needsSignaling() {
      return this.isHostingOnline() || this.friendJoinHandler.hasOutgoingJoinRequests();
   }

   public boolean isHostingOnline() {
      IntegratedServer server = this.minecraft.getSingleplayerServer();
      return server != null && server.isPublishedOnline();
   }

   public void ensureSignalingConnected() {
      this.signaling.connect();
   }

   public void maybeDisconnectSignaling() {
      if (!this.needsSignaling()) {
         this.signaling.disconnect();
      }
   }

   public void onHostScopeChanged(final MinecraftServer.MultiplayerScope scope) {
      LOGGER.debug("Host scope changed to {}", scope);
      if (scope == MinecraftServer.MultiplayerScope.ONLINE) {
         this.ensureSignalingConnected();
      } else {
         this.teardownHostState();
      }
   }

   private void teardownHostState() {
      this.rtcHandshakeHandler.closeHostHandshakes();
      this.friendJoinHandler.clearHostJoinRequestState();
      this.maybeDisconnectSignaling();
   }

   public CompletableFuture<@Nullable Void> joinPlayer(final String peerPmid) {
      return this.friendJoinHandler.joinPlayer(peerPmid);
   }

   public void cancelOutgoingJoins() {
      this.friendJoinHandler.cancelOutgoingJoins();
      this.rtcHandshakeHandler.cancelInitiatorHandshakes();
      this.maybeDisconnectSignaling();
   }

   public boolean hasIncomingJoinRequest(final UUID peerPmid) {
      return this.friendJoinHandler.hasIncomingJoinRequest(peerPmid);
   }

   public boolean hasOutgoingJoinRequest() {
      return this.friendJoinHandler.hasOutgoingJoinRequests();
   }

   public FriendJoinHandler.OutgoingJoinState outgoingJoinState(final UUID peerPmid) {
      return this.friendJoinHandler.outgoingJoinState(peerPmid);
   }

   public @Nullable UUID connectingOutgoingJoinPmid() {
      return this.friendJoinHandler.connectingOutgoingJoinPmid();
   }

   public void acceptIncomingJoinRequest(final UUID peerPmid) {
      this.friendJoinHandler.acceptIncomingJoinRequest(peerPmid);
   }

   public void rejectIncomingJoinRequest(final UUID peerPmid) {
      this.friendJoinHandler.rejectIncomingJoinRequest(peerPmid);
   }

   public CompletableFuture<@Nullable Void> declineInvite(final UUID hostPmid) {
      return this.friendJoinHandler.declineInvite(hostPmid);
   }

   public void addJoinStateListener(final Runnable listener) {
      this.friendJoinHandler.addJoinStateListener(listener);
   }

   public void removeJoinStateListener(final Runnable listener) {
      this.friendJoinHandler.removeJoinStateListener(listener);
   }

   public void notifyJoinStateChanged() {
      this.friendJoinHandler.notifyJoinStateChanged();
   }

   public synchronized void shutdown() {
      this.shutdown = true;
      this.signaling.clearHandlers();
      this.rtcHandshakeHandler.shutdown();
      this.friendJoinHandler.shutdown();
      this.signaling.disconnect();
   }

   public boolean hasHandshake(final UUID peerPmid) {
      return this.rtcHandshakeHandler.hasHandshake(peerPmid);
   }

   public @Nullable RtcHandshake getHandshake(final UUID peerPmid) {
      return this.rtcHandshakeHandler.getHandshake(peerPmid);
   }

   public CompletableFuture<@Nullable Void> startHandshake(final UUID peerPmid, final String sessionId) {
      return this.rtcHandshakeHandler.startHandshake(peerPmid, sessionId);
   }

   public boolean consumeAcceptedJoinRequest(final UUID peerPmid, final String sessionId) {
      return this.friendJoinHandler.consumeAcceptedJoinRequest(peerPmid, sessionId);
   }
}
