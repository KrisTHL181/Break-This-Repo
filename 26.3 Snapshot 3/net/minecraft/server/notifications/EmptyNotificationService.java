package net.minecraft.server.notifications;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public class EmptyNotificationService implements NotificationService {
   @Override
   public void playerJoined(final ServerPlayer player) {
   }

   @Override
   public void playerLeft(final ServerPlayer player) {
   }

   @Override
   public void serverStarted() {
   }

   @Override
   public void serverShuttingDown() {
   }

   @Override
   public void serverSaveStarted() {
   }

   @Override
   public void serverSaveCompleted() {
   }

   @Override
   public void serverActivityOccured() {
   }

   @Override
   public void worldUpgradeStarted() {
   }

   @Override
   public void worldUpgradeProgress(final float progressPercentage) {
   }

   @Override
   public void worldUpgradeFinished() {
   }

   @Override
   public void worldUpgradeFailed(final String reason) {
   }

   @Override
   public void playerOped(final ServerOpListEntry operator) {
   }

   @Override
   public void playerDeoped(final ServerOpListEntry operator) {
   }

   @Override
   public void playerAddedToAllowlist(final NameAndId player) {
   }

   @Override
   public void playerRemovedFromAllowlist(final NameAndId player) {
   }

   @Override
   public void ipBanned(final IpBanListEntry ban) {
   }

   @Override
   public void ipUnbanned(final String ip) {
   }

   @Override
   public void playerBanned(final UserBanListEntry ban) {
   }

   @Override
   public void playerUnbanned(final NameAndId player) {
   }

   @Override
   public <T> void onGameRuleChanged(final GameRule<T> gameRule, final T value) {
   }

   @Override
   public void statusHeartbeat() {
   }
}
