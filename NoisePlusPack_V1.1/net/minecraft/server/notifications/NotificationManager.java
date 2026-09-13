package net.minecraft.server.notifications;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public class NotificationManager implements NotificationService {
   private final List<NotificationService> notificationServices = Lists.newArrayList();

   public void registerService(NotificationService p_424571_) {
      this.notificationServices.add(p_424571_);
   }

   @Override
   public void playerJoined(ServerPlayer p_430504_) {
      this.notificationServices.forEach(p_428033_ -> p_428033_.playerJoined(p_430504_));
   }

   @Override
   public void playerLeft(ServerPlayer p_424060_) {
      this.notificationServices.forEach(p_425371_ -> p_425371_.playerLeft(p_424060_));
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
   public void playerOped(ServerOpListEntry p_425535_) {
      this.notificationServices.forEach(p_431655_ -> p_431655_.playerOped(p_425535_));
   }

   @Override
   public void playerDeoped(ServerOpListEntry p_430768_) {
      this.notificationServices.forEach(p_422313_ -> p_422313_.playerDeoped(p_430768_));
   }

   @Override
   public void playerAddedToAllowlist(NameAndId p_429159_) {
      this.notificationServices.forEach(p_425490_ -> p_425490_.playerAddedToAllowlist(p_429159_));
   }

   @Override
   public void playerRemovedFromAllowlist(NameAndId p_425138_) {
      this.notificationServices.forEach(p_430717_ -> p_430717_.playerRemovedFromAllowlist(p_425138_));
   }

   @Override
   public void ipBanned(IpBanListEntry p_426179_) {
      this.notificationServices.forEach(p_428013_ -> p_428013_.ipBanned(p_426179_));
   }

   @Override
   public void ipUnbanned(String p_429529_) {
      this.notificationServices.forEach(p_424395_ -> p_424395_.ipUnbanned(p_429529_));
   }

   @Override
   public void playerBanned(UserBanListEntry p_424235_) {
      this.notificationServices.forEach(p_422534_ -> p_422534_.playerBanned(p_424235_));
   }

   @Override
   public void playerUnbanned(NameAndId p_428436_) {
      this.notificationServices.forEach(p_427067_ -> p_427067_.playerUnbanned(p_428436_));
   }

   @Override
   public <T> void onGameRuleChanged(GameRule<T> p_458442_, T p_456935_) {
      this.notificationServices.forEach(p_449160_ -> p_449160_.onGameRuleChanged(p_458442_, p_456935_));
   }

   @Override
   public void statusHeartbeat() {
      this.notificationServices.forEach(NotificationService::statusHeartbeat);
   }
}
