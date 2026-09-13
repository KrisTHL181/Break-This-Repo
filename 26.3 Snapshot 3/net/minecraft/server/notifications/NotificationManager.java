package net.minecraft.server.notifications;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;
import org.jspecify.annotations.Nullable;

public class NotificationManager implements NotificationService {
   private final List<NotificationService> notificationServices = Lists.newArrayList();
   private @Nullable DedicatedServer server;

   public void registerService(final NotificationService notificationService) {
      this.notificationServices.add(notificationService);
   }

   public void setServer(final DedicatedServer server) {
      if (this.server != null) {
         throw new IllegalStateException("Server already set");
      }

      this.server = server;
   }

   public @Nullable DedicatedServer server() {
      return this.server;
   }

   @Override
   public void playerJoined(final ServerPlayer player) {
      this.notificationServices.forEach(notificationService -> notificationService.playerJoined(player));
   }

   @Override
   public void playerLeft(final ServerPlayer player) {
      this.notificationServices.forEach(notificationService -> notificationService.playerLeft(player));
   }

   @Override
   public void serverStarted() {
      this.notificationServices.forEach(NotificationService::serverStarted);
   }

   @Override
   public void serverShuttingDown() {
      this.notificationServices.forEach(NotificationService::serverShuttingDown);
   }

   @Override
   public void serverSaveStarted() {
      this.notificationServices.forEach(NotificationService::serverSaveStarted);
   }

   @Override
   public void serverSaveCompleted() {
      this.notificationServices.forEach(NotificationService::serverSaveCompleted);
   }

   @Override
   public void serverActivityOccured() {
      this.notificationServices.forEach(NotificationService::serverActivityOccured);
   }

   @Override
   public void worldUpgradeStarted() {
      this.notificationServices.forEach(notificationService -> notificationService.worldUpgradeStarted());
   }

   @Override
   public void worldUpgradeProgress(final float progressPercentage) {
      this.notificationServices.forEach(notificationService -> notificationService.worldUpgradeProgress(progressPercentage));
   }

   @Override
   public void worldUpgradeFinished() {
      this.notificationServices.forEach(notificationService -> notificationService.worldUpgradeFinished());
   }

   @Override
   public void worldUpgradeFailed(final String reason) {
      this.notificationServices.forEach(notificationService -> notificationService.worldUpgradeFailed(reason));
   }

   @Override
   public void playerOped(final ServerOpListEntry operator) {
      this.notificationServices.forEach(notificationService -> notificationService.playerOped(operator));
   }

   @Override
   public void playerDeoped(final ServerOpListEntry operator) {
      this.notificationServices.forEach(notificationService -> notificationService.playerDeoped(operator));
   }

   @Override
   public void playerAddedToAllowlist(final NameAndId player) {
      this.notificationServices.forEach(notificationService -> notificationService.playerAddedToAllowlist(player));
   }

   @Override
   public void playerRemovedFromAllowlist(final NameAndId player) {
      this.notificationServices.forEach(notificationService -> notificationService.playerRemovedFromAllowlist(player));
   }

   @Override
   public void ipBanned(final IpBanListEntry ban) {
      this.notificationServices.forEach(notificationService -> notificationService.ipBanned(ban));
   }

   @Override
   public void ipUnbanned(final String ip) {
      this.notificationServices.forEach(notificationService -> notificationService.ipUnbanned(ip));
   }

   @Override
   public void playerBanned(final UserBanListEntry ban) {
      this.notificationServices.forEach(notificationService -> notificationService.playerBanned(ban));
   }

   @Override
   public void playerUnbanned(final NameAndId player) {
      this.notificationServices.forEach(notificationService -> notificationService.playerUnbanned(player));
   }

   @Override
   public <T> void onGameRuleChanged(final GameRule<T> gameRule, final T value) {
      this.notificationServices.forEach(notificationService -> notificationService.onGameRuleChanged(gameRule, value));
   }

   @Override
   public void statusHeartbeat() {
      this.notificationServices.forEach(NotificationService::statusHeartbeat);
   }
}
