package net.minecraft.client.gui.screens.social;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.FriendsService;
import com.mojang.authlib.services.FriendsService.ResultCode;
import com.mojang.authlib.services.response.FriendDto;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public class PlayerSocialManager {
   private static final Component FRIEND_ACTION_FAILED_MESSAGE = Component.translatable("gui.friends.error.failed.message");
   private static final Component FRIEND_ACTION_RATE_LIMITED_MESSAGE = Component.translatable("gui.friends.error.rateLimited.message");
   private static final Component FRIEND_ACTION_FORBIDDEN_MESSAGE = Component.translatable("gui.friends.error.forbidden.message");
   private static final Component FRIEND_ACTION_UNKNOWN_PROFILE = Component.translatable("gui.friends.error.user_may_lack_active_profile");
   private static final Component FRIEND_ACTION_UNAUTHORIZED = Component.translatable("gui.friends.error.unauthorized");
   private static final Component FRIEND_ACTION_UNAVAILABLE_MESSAGE = Component.translatable("gui.friends.error.unavailable.message");
   private final Minecraft minecraft;
   private final Set<UUID> hiddenPlayers = Sets.newHashSet();
   private final UserApiService service;
   private final FriendsService friendsService;
   private final PresenceHandler presenceHandler;
   private final Map<String, UUID> discoveredNamesToUUID = Maps.newHashMap();
   private boolean onlineMode;
   private CompletableFuture<?> pendingBlockListRefresh = CompletableFuture.completedFuture(null);
   private final RemoteFriendListUpdateHandler remoteFriendListUpdateHandler;
   private boolean friendListEnabled;
   private boolean allowFriendRequests;

   public PlayerSocialManager(
      final Minecraft minecraft,
      final UserApiService service,
      final FriendsService friendsService,
      final RemoteFriendListUpdateHandler remoteFriendListUpdateHandler
   ) {
      this.minecraft = minecraft;
      this.service = service;
      this.friendsService = friendsService;
      this.remoteFriendListUpdateHandler = remoteFriendListUpdateHandler;
      this.friendListEnabled = minecraft.friendsEnabled();
      this.allowFriendRequests = minecraft.allowFriendRequests();
      this.presenceHandler = new PresenceHandler(minecraft, friendsService);
   }

   public void addFriendListUpdateListener(final Runnable listener) {
      this.remoteFriendListUpdateHandler.addUpdateListener(listener);
   }

   public void removeFriendListUpdateListener(final Runnable listener) {
      this.remoteFriendListUpdateHandler.removeUpdateListener(listener);
   }

   public List<PlayerSocialManager.PlayerData> getFriends() {
      return remap(this.remoteFriendListUpdateHandler.getLatestFriendData().friends());
   }

   public List<PlayerSocialManager.PlayerData> getIncomingRequests() {
      return remap(this.remoteFriendListUpdateHandler.getLatestFriendData().incomingRequests());
   }

   public List<PlayerSocialManager.PlayerData> getOutgoingRequests() {
      return remap(this.remoteFriendListUpdateHandler.getLatestFriendData().outgoingRequests());
   }

   public RemoteFriendListUpdateHandler.State getFriendListState() {
      return this.remoteFriendListUpdateHandler.getState();
   }

   public void hidePlayer(final UUID id) {
      this.hiddenPlayers.add(id);
   }

   public void showPlayer(final UUID id) {
      this.hiddenPlayers.remove(id);
   }

   public boolean shouldHideMessageFrom(final UUID id) {
      return this.isHidden(id) || this.isBlocked(id);
   }

   public boolean isHidden(final UUID id) {
      return this.hiddenPlayers.contains(id);
   }

   public void startOnlineMode() {
      this.onlineMode = true;
      this.pendingBlockListRefresh = this.pendingBlockListRefresh.thenRunAsync(this.service::refreshBlockList, Util.nonCriticalIoPool());
   }

   public void stopOnlineMode() {
      this.onlineMode = false;
   }

   public boolean isBlocked(final UUID id) {
      if (!this.onlineMode) {
         return false;
      }

      this.pendingBlockListRefresh.join();
      return this.service.isBlockedPlayer(id);
   }

   public Set<UUID> getHiddenPlayers() {
      return this.hiddenPlayers;
   }

   public UUID getDiscoveredUUID(final String name) {
      return this.discoveredNamesToUUID.getOrDefault(name, Util.NIL_UUID);
   }

   public void addPlayer(final PlayerInfo info) {
      GameProfile gameProfile = info.getProfile();
      this.discoveredNamesToUUID.put(gameProfile.name(), gameProfile.id());
      if (this.minecraft.gui.screen() instanceof SocialInteractionsScreen screen) {
         screen.onAddPlayer(info);
      }
   }

   public CompletableFuture<ResultCode> sendFriendRequest(final String name) {
      return this.runAction(() -> this.friendsService.sendFriendRequest(name));
   }

   public void removePlayer(final UUID id) {
      if (this.minecraft.gui.screen() instanceof SocialInteractionsScreen screen) {
         screen.onRemovePlayer(id);
      }
   }

   public CompletableFuture<ResultCode> removeFriend(final UUID id) {
      return this.runAction(() -> this.friendsService.removeFriend(id));
   }

   public CompletableFuture<ResultCode> acceptIncomingFriendRequest(final UUID id) {
      return this.runAction(() -> this.friendsService.acceptIncomingFriendRequest(id));
   }

   public CompletableFuture<ResultCode> declineIncomingFriendRequest(final UUID id) {
      return this.runAction(() -> this.friendsService.declineIncomingFriendRequest(id));
   }

   public CompletableFuture<ResultCode> revokeOutgoingFriendRequest(final UUID id) {
      return this.runAction(() -> this.friendsService.revokeOutgoingFriendRequest(id));
   }

   public CompletableFuture<ResultCode> updateFriendSettings(final boolean friendsListEnabled, final boolean allowInvites) {
      return this.runAction(() -> this.friendsService.updateFriendSettings(friendsListEnabled, allowInvites));
   }

   private CompletableFuture<ResultCode> runAction(final Supplier<ResultCode> action) {
      return CompletableFuture.<ResultCode>supplyAsync(() -> {
            ResultCode result = action.get();
            this.handleResult(result);
            return result;
         }, Util.ioPool())
         .thenComposeAsync(
            result -> result == ResultCode.SUCCESS
               ? this.remoteFriendListUpdateHandler.forceUpdate().thenApply(var1x -> result)
               : CompletableFuture.completedFuture(result),
            Util.ioPool()
         );
   }

   private void handleResult(final ResultCode result) {
      if (result != ResultCode.SUCCESS) {
         this.showFailureToast(result);
      }
   }

   private void showFailureToast(final ResultCode resultCode) {
      Component title = switch (resultCode) {
         case TOO_MANY_REQUESTS -> FRIEND_ACTION_RATE_LIMITED_MESSAGE;
         case UNKNOWN_PROFILE -> FRIEND_ACTION_UNKNOWN_PROFILE;
         case UNAUTHORIZED -> FRIEND_ACTION_UNAUTHORIZED;
         case FORBIDDEN -> FRIEND_ACTION_FORBIDDEN_MESSAGE;
         case SERVICE_NOT_AVAILABLE -> FRIEND_ACTION_UNAVAILABLE_MESSAGE;
         case ERROR -> FRIEND_ACTION_FAILED_MESSAGE;
         case SUCCESS, UPGRADE_NEEDED, CONNECTION_ISSUE, TEMPORARY_UNAVAILABLE, GENERIC_ERROR -> null;
         default -> throw new MatchException(null, null);
      };
      if (title != null) {
         this.minecraft
            .execute(() -> SystemToast.addOrUpdate(this.minecraft.gui.toastManager(), SystemToast.SystemToastId.FRIEND_SYSTEM_NOTIFICATION, title, null));
      }
   }

   public boolean isFriendListEnabled() {
      return this.friendListEnabled;
   }

   public void setFriendListEnabled(final boolean friendListEnabled) {
      this.friendListEnabled = friendListEnabled;
      if (friendListEnabled) {
         this.remoteFriendListUpdateHandler.start();
      } else {
         this.remoteFriendListUpdateHandler.stop();
      }
   }

   public boolean isAllowFriendRequests() {
      return this.allowFriendRequests;
   }

   public void setAllowFriendRequests(final boolean allowFriendRequests) {
      this.allowFriendRequests = allowFriendRequests;
   }

   public PresenceHandler getPresenceHandler() {
      return this.presenceHandler;
   }

   public boolean isFriend(final UUID uuid) {
      for (PlayerSocialManager.PlayerData playerData : this.getFriends()) {
         if (playerData.id.equals(uuid)) {
            return true;
         }
      }

      return false;
   }

   private static List<PlayerSocialManager.PlayerData> remap(final List<FriendDto> friends) {
      return friends.stream().map(friend -> new PlayerSocialManager.PlayerData(friend.profileId(), friend.name())).toList();
   }

   public record PlayerData(UUID id, String name) {
   }
}
