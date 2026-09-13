package net.minecraft.client.gui.screens.social;

import com.mojang.authlib.services.FriendsService;
import com.mojang.authlib.services.response.PresenceResponse;
import com.mojang.authlib.services.response.PresenceStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PresenceSharing;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;

public class PresenceHandler {
   private static final Duration PRESENCE_UPDATE_INTERVAL = Duration.ofMinutes(1L);
   private static final long MAX_PRESENCE_INTERVAL_MULTIPLIER = 5L;
   private final Minecraft minecraft;
   private final FriendsService friendsService;
   private PresenceResponse latestPresence = new PresenceResponse(new ArrayList());
   private Instant lastPresencePost = Instant.now();
   private boolean updatePresence;

   public PresenceHandler(final Minecraft minecraft, final FriendsService friendsService) {
      this.minecraft = minecraft;
      this.friendsService = friendsService;
      this.updatePresence = true;
   }

   private void updatePresence() {
      this.updatePresence = false;
      this.lastPresencePost = Instant.now();
      PresenceStatus publicPresenceStatus = this.getPublicPresenceStatus();
      CompletableFuture.runAsync(() -> {
         PresenceResponse newPresence = this.friendsService.presence(publicPresenceStatus.name());
         this.minecraft.execute(() -> {
            boolean refreshPresence = !Objects.equals(this.latestPresence, newPresence);
            this.latestPresence = newPresence;
            if (refreshPresence && this.minecraft.gui.screen() instanceof FriendsOverlayScreen friendsOverlayScreen) {
               friendsOverlayScreen.applyPresenceUpdate();
            }
         });
      }, Util.nonCriticalIoPool());
   }

   private boolean shouldRefreshPresence() {
      PlayerSocialManager socialManager = this.minecraft.getPlayerSocialManager();
      if (socialManager.isFriendListEnabled() && !socialManager.getFriends().isEmpty()) {
         Duration sinceLastPresence = Duration.between(this.lastPresencePost, Instant.now());
         Optional<Duration> presencePollInterval = this.friendsService.getPresencePollInterval();
         Duration interval = !presencePollInterval.isEmpty() && presencePollInterval.get().isPositive() ? presencePollInterval.get() : PRESENCE_UPDATE_INTERVAL;
         Duration maxInterval = interval.multipliedBy(5L);
         return this.updatePresence && sinceLastPresence.compareTo(interval) >= 0 || sinceLastPresence.compareTo(maxInterval) >= 0;
      } else {
         return false;
      }
   }

   public void tick() {
      if (this.shouldRefreshPresence()) {
         this.updatePresence();
      }
   }

   public void tryUpdatePresence() {
      this.updatePresence = true;
   }

   public PresenceResponse getLatestPresence() {
      return this.latestPresence;
   }

   private PresenceStatus getPublicPresenceStatus() {
      return switch ((PresenceSharing)this.minecraft.options.sharePresence().get()) {
         case NONE -> PresenceStatus.OFFLINE;
         case LIMITED -> PresenceStatus.ONLINE;
         case ALL -> this.getPresenceStatus();
      };
   }

   private PresenceStatus getPresenceStatus() {
      IntegratedServer singleplayerServer = this.minecraft.getSingleplayerServer();
      if (singleplayerServer != null) {
         return singleplayerServer.getMultiplayerScope() == MinecraftServer.MultiplayerScope.LAN
            ? PresenceStatus.PLAYING_HOSTED_SERVER
            : PresenceStatus.PLAYING_OFFLINE;
      } else {
         ServerData server = this.minecraft.getCurrentServer();
         if (server != null) {
            return server.isRealm() ? PresenceStatus.PLAYING_REALMS : PresenceStatus.PLAYING_SERVER;
         } else {
            return PresenceStatus.ONLINE;
         }
      }
   }
}
