package net.minecraft.client.multiplayer.p2p;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.FriendToast;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.p2p.client.SignalingServiceClient;
import net.minecraft.client.network.webrtc.RtcHandshake;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class FriendJoinHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final long JOIN_REQUEST_TIMEOUT_MINUTES = 1L;
   private final Minecraft minecraft;
   private final SignalingServiceClient signaling;
   private final P2PManager manager;
   private final ConcurrentHashMap<UUID, String> incomingJoinRequests = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<UUID, FriendJoinHandler.OutgoingJoinRequest> outgoingJoinRequests = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<UUID, String> acceptedAwaitingOffer = new ConcurrentHashMap<>();
   private final CopyOnWriteArrayList<Runnable> joinStateListeners = new CopyOnWriteArrayList<>();
   private final SignalingServiceClient.ConnectionListener connectionListener = new SignalingServiceClient.ConnectionListener() {
      @Override
      public void onSignalingError(final @Nullable UUID peerPmid, final SignalingException cause) {
         if (peerPmid != null) {
            FriendJoinHandler.OutgoingJoinRequest pending = FriendJoinHandler.this.outgoingJoinRequests.get(peerPmid);
            if (pending != null) {
               pending.result().completeExceptionally(cause);
            }
         }
      }
   };

   public FriendJoinHandler(final Minecraft minecraft, final SignalingServiceClient signaling, final P2PManager manager) {
      this.minecraft = minecraft;
      this.signaling = signaling;
      this.manager = manager;
      signaling.setFriendJoinHandler(this::handle);
      signaling.addConnectionListener(this.connectionListener);
   }

   public CompletableFuture<Void> joinPlayer(final String peerPmid) {
      UUID parsed;
      try {
         parsed = UUID.fromString(peerPmid);
      } catch (IllegalArgumentException e) {
         LOGGER.debug("Invalid peer PMID");
         return CompletableFuture.failedFuture(e);
      }

      FriendJoinHandler.OutgoingJoinRequest existing = this.outgoingJoinRequests.get(parsed);
      if (existing != null) {
         return existing.result();
      }

      if (this.manager.hasHandshake(parsed)) {
         return CompletableFuture.completedFuture(null);
      }

      String signalingSessionId = UUID.randomUUID().toString();
      CompletableFuture<Void> result = new CompletableFuture<>();
      FriendJoinHandler.OutgoingJoinRequest request = new FriendJoinHandler.OutgoingJoinRequest(signalingSessionId, result, new AtomicBoolean());
      FriendJoinHandler.OutgoingJoinRequest raced = this.outgoingJoinRequests.putIfAbsent(parsed, request);
      if (raced != null) {
         return raced.result();
      }

      result.whenComplete((var3x, var4x) -> {
         if (this.outgoingJoinRequests.remove(parsed, request)) {
            this.notifyJoinStateChanged();
            this.manager.maybeDisconnectSignaling();
         }
      });
      this.notifyJoinStateChanged();
      this.manager.ensureSignalingConnected();
      CompletableFuture.delayedExecutor(1L, TimeUnit.MINUTES).execute(() -> {
         if (!request.sdpStarted().get() && result.completeExceptionally(new TimeoutException("Join request timed out"))) {
            LOGGER.debug("Join request timed out (session={})", signalingSessionId);
            this.showJoinInviteExpiredToast(parsed);
         }
      });
      this.signaling.sendClientMessage(parsed, SignalingMessage.joinRequest(signalingSessionId)).whenComplete((var2x, error) -> {
         if (error != null) {
            LOGGER.debug("Failed to send join request (session={})", signalingSessionId, error);
            result.completeExceptionally(error);
         }
      });
      return result;
   }

   private void handle(final UUID fromPmid, final SignalingMessage.FriendJoin msg) {
      switch (msg) {
         case SignalingMessage.FriendJoin.Request request:
            this.handleJoinRequest(fromPmid, request.sessionId());
            break;
         case SignalingMessage.FriendJoin.Accepted accepted:
            this.handleJoinAccepted(fromPmid, accepted.sessionId());
            break;
         case SignalingMessage.FriendJoin.Rejected rejected:
            this.handleJoinRejected(fromPmid, rejected.sessionId());
            break;
         case SignalingMessage.FriendJoin.InviteDeclined var8:
            this.handleInviteDeclined(fromPmid);
            break;
         default:
            throw new MatchException(null, null);
      }
   }

   public void clearHostJoinRequestState() {
      if (!this.incomingJoinRequests.isEmpty()) {
         this.incomingJoinRequests.clear();
         this.notifyJoinStateChanged();
      }

      this.acceptedAwaitingOffer.clear();
   }

   public void cancelOutgoingJoins() {
      this.outgoingJoinRequests.forEach((peerPmid, request) -> {
         if (this.outgoingJoinRequests.remove(peerPmid, request)) {
            request.result().completeExceptionally(new CancellationException("join request cancelled"));
            this.notifyJoinStateChanged();
         }
      });
   }

   public void shutdown() {
      this.signaling.removeConnectionListener(this.connectionListener);
      this.incomingJoinRequests.clear();
      this.outgoingJoinRequests.forEach((var0, request) -> request.result().completeExceptionally(new CancellationException("shutdown")));
      this.outgoingJoinRequests.clear();
      this.acceptedAwaitingOffer.clear();
      this.notifyJoinStateChanged();
   }

   public boolean hasIncomingJoinRequest(final UUID peerPmid) {
      return this.incomingJoinRequests.containsKey(peerPmid);
   }

   public boolean hasOutgoingJoinRequests() {
      return !this.outgoingJoinRequests.isEmpty();
   }

   public FriendJoinHandler.OutgoingJoinState outgoingJoinState(final UUID peerPmid) {
      FriendJoinHandler.OutgoingJoinRequest pending = this.outgoingJoinRequests.get(peerPmid);
      if (pending != null) {
         return pending.sdpStarted().get() ? FriendJoinHandler.OutgoingJoinState.CONNECTING : FriendJoinHandler.OutgoingJoinState.AWAITING_HOST_ACCEPT;
      }

      RtcHandshake handshake = this.manager.getHandshake(peerPmid);
      return handshake != null && handshake.isInitiator() ? FriendJoinHandler.OutgoingJoinState.CONNECTED : FriendJoinHandler.OutgoingJoinState.NONE;
   }

   public @Nullable UUID connectingOutgoingJoinPmid() {
      for (Entry<UUID, FriendJoinHandler.OutgoingJoinRequest> entry : this.outgoingJoinRequests.entrySet()) {
         if (entry.getValue().sdpStarted().get()) {
            return entry.getKey();
         }
      }

      return null;
   }

   public void acceptIncomingJoinRequest(final UUID peerPmid) {
      String sessionId = this.incomingJoinRequests.remove(peerPmid);
      if (sessionId != null) {
         this.notifyJoinStateChanged();
         if (this.manager.isHostingOnline() && this.minecraft.getPlayerSocialManager().isFriendsPmid(peerPmid)) {
            this.sendJoinAccepted(peerPmid, sessionId).exceptionally(var0 -> null);
         } else {
            this.signaling.sendClientMessage(peerPmid, SignalingMessage.joinRejected(sessionId)).exceptionally(err -> {
               LOGGER.debug("Failed to reject pending join request (session={})", sessionId, err);
               return null;
            });
         }
      }
   }

   public void rejectIncomingJoinRequest(final UUID peerPmid) {
      String sessionId = this.incomingJoinRequests.remove(peerPmid);
      if (sessionId != null) {
         this.acceptedAwaitingOffer.remove(peerPmid, sessionId);
         this.signaling.sendClientMessage(peerPmid, SignalingMessage.joinRejected(sessionId)).exceptionally(err -> {
            LOGGER.debug("Failed to send join rejection (session={})", sessionId, err);
            return null;
         });
         this.notifyJoinStateChanged();
      }

      RtcHandshake existing = this.manager.getHandshake(peerPmid);
      if (existing != null && !existing.isInitiator()) {
         existing.abort("join request rejected");
      }
   }

   public CompletableFuture<@Nullable Void> declineInvite(final UUID hostPmid) {
      this.manager.ensureSignalingConnected();
      return this.signaling
         .sendClientMessage(hostPmid, SignalingMessage.inviteDeclined())
         .whenComplete((var1, var2) -> this.manager.maybeDisconnectSignaling())
         .exceptionallyCompose(err -> {
            LOGGER.debug("Failed to decline invite", err);
            return CompletableFuture.failedFuture(err);
         });
   }

   public boolean consumeAcceptedJoinRequest(final UUID peerPmid, final String sessionId) {
      return this.acceptedAwaitingOffer.remove(peerPmid, sessionId);
   }

   public void addJoinStateListener(final Runnable listener) {
      this.joinStateListeners.add(listener);
   }

   public void removeJoinStateListener(final Runnable listener) {
      this.joinStateListeners.remove(listener);
   }

   private void handleJoinRequest(final UUID fromPmid, final String sessionId) {
      if (!this.manager.isHostingOnline()) {
         this.signaling.sendClientMessage(fromPmid, SignalingMessage.joinRejected(sessionId)).exceptionally(err -> {
            LOGGER.debug("Failed to reject join request (session={})", sessionId, err);
            return null;
         });
      } else if (!this.minecraft.getPlayerSocialManager().isFriendsPmid(fromPmid)) {
         LOGGER.debug("Ignoring join request from non-friend");
      } else if (this.minecraft.getPlayerSocialManager().getPresenceHandler().isInvitedPmid(fromPmid)) {
         this.sendJoinAccepted(fromPmid, sessionId).thenRun(() -> this.clearHostInvite(fromPmid)).exceptionally(var0 -> null);
      } else {
         this.incomingJoinRequests.put(fromPmid, sessionId);
         this.notifyJoinStateChanged();
         LOGGER.debug("Awaiting user response to join request (session={})", sessionId);
         UUID peerProfileId = this.minecraft.getPlayerSocialManager().getPresenceHandler().getProfileIdFromPmid(fromPmid);
         Optional<PlayerSocialManager.PlayerData> friendData = this.minecraft
            .getPlayerSocialManager()
            .getFriends()
            .stream()
            .filter(playerData -> playerData.id().equals(peerProfileId))
            .findAny();
         friendData.ifPresent(friend -> this.minecraft.execute(() -> {
            PlayerSkin friendSkin = this.minecraft.playerSkinRenderCache().getOrDefault(ResolvableProfile.createUnresolved(friend.id())).playerSkin();
            FriendToast.showFriendJoinRequest(this.minecraft, friend.name(), friendSkin);
         }));
      }
   }

   private void handleJoinAccepted(final UUID fromPmid, final String sessionId) {
      FriendJoinHandler.OutgoingJoinRequest request = this.outgoingJoinRequests.get(fromPmid);
      if (request == null) {
         LOGGER.debug("Ignoring join acceptance for session={} (no pending join request)", sessionId);
      } else if (!request.sessionId().equals(sessionId)) {
         LOGGER.debug("Ignoring stale join acceptance for session={} (pending={})", sessionId, request.sessionId());
      } else if (!request.sdpStarted().compareAndSet(false, true)) {
         LOGGER.trace("Ignoring duplicate join acceptance for session={}", sessionId);
      } else {
         this.notifyJoinStateChanged();
         if (this.manager.hasHandshake(fromPmid)) {
            request.result().completeExceptionally(new IllegalStateException("Handshake already in progress"));
         } else {
            this.manager.startHandshake(fromPmid, sessionId).whenComplete((var2, error) -> {
               if (error != null) {
                  request.result().completeExceptionally(error);
               } else {
                  request.result().complete(null);
                  this.manager.cancelOutgoingJoins();
               }
            });
         }
      }
   }

   private void handleJoinRejected(final UUID fromPmid, final String sessionId) {
      FriendJoinHandler.OutgoingJoinRequest request = this.outgoingJoinRequests.get(fromPmid);
      if (request != null && request.sessionId().equals(sessionId)) {
         LOGGER.debug("Join request rejected by peer (session={})", sessionId);
         request.result().completeExceptionally(new RuntimeException("Join request rejected"));
      } else {
         LOGGER.debug("Ignoring join rejection for session={} (no matching pending request)", sessionId);
      }
   }

   private void handleInviteDeclined(final UUID fromPmid) {
      if (!this.minecraft.getPlayerSocialManager().isFriendsPmid(fromPmid)) {
         LOGGER.debug("Ignoring invite decline from non-friend");
      } else {
         this.clearHostInvite(fromPmid);
      }
   }

   private void clearHostInvite(final UUID peerPmid) {
      this.minecraft.getPlayerSocialManager().getPresenceHandler().clearInviteForPmid(peerPmid);
   }

   private CompletableFuture<@Nullable Void> sendJoinAccepted(final UUID peerPmid, final String sessionId) {
      this.acceptedAwaitingOffer.put(peerPmid, sessionId);
      return this.signaling.sendClientMessage(peerPmid, SignalingMessage.joinAccepted(sessionId)).exceptionallyCompose(err -> {
         this.acceptedAwaitingOffer.remove(peerPmid, sessionId);
         LOGGER.debug("Failed to send join acceptance (session={})", sessionId, err);
         return CompletableFuture.failedFuture(err);
      });
   }

   void notifyJoinStateChanged() {
      this.minecraft.execute(() -> this.joinStateListeners.forEach(Runnable::run));
   }

   private void showJoinInviteExpiredToast(final UUID peerPmid) {
      this.minecraft
         .execute(
            () -> {
               UUID peerProfileId = this.minecraft.getPlayerSocialManager().getPresenceHandler().getProfileIdFromPmid(peerPmid);
               if (peerProfileId != null) {
                  this.minecraft
                     .getPlayerSocialManager()
                     .getFriends()
                     .stream()
                     .filter(playerData -> playerData.id().equals(peerProfileId))
                     .findAny()
                     .ifPresent(friend -> {
                        PlayerSkin friendSkin = this.minecraft
                           .playerSkinRenderCache()
                           .getOrDefault(ResolvableProfile.createUnresolved(friend.id()))
                           .playerSkin();
                        FriendToast.showJoinInviteExpired(this.minecraft, friend.name(), friendSkin);
                     });
               }
            }
         );
   }

   private record OutgoingJoinRequest(String sessionId, CompletableFuture<@Nullable Void> result, AtomicBoolean sdpStarted) {
   }

   public enum OutgoingJoinState {
      NONE,
      AWAITING_HOST_ACCEPT,
      CONNECTING,
      CONNECTED;
   }
}
