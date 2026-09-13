package net.minecraft.server.notifications;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public class EmptyNotificationService implements NotificationService {
   @Override
   public void playerJoined(ServerPlayer p_430080_) {
   }

   @Override
   public void playerLeft(ServerPlayer p_427884_) {
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
   public void playerOped(ServerOpListEntry p_429396_) {
   }

   @Override
   public void playerDeoped(ServerOpListEntry p_428376_) {
   }

   @Override
   public void playerAddedToAllowlist(NameAndId p_422419_) {
   }

   @Override
   public void playerRemovedFromAllowlist(NameAndId p_423522_) {
   }

   @Override
   public void ipBanned(IpBanListEntry p_422456_) {
   }

   @Override
   public void ipUnbanned(String p_431536_) {
   }

   @Override
   public void playerBanned(UserBanListEntry p_430537_) {
   }

   @Override
   public void playerUnbanned(NameAndId p_425895_) {
   }

   @Override
   public <T> void onGameRuleChanged(GameRule<T> p_451324_, T p_455696_) {
   }

   @Override
   public void statusHeartbeat() {
   }
}
