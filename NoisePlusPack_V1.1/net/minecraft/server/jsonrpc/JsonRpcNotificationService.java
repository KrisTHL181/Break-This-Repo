package net.minecraft.server.jsonrpc;

import net.minecraft.core.Holder;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.BanlistService;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.server.jsonrpc.methods.IpBanlistService;
import net.minecraft.server.jsonrpc.methods.OperatorService;
import net.minecraft.server.jsonrpc.methods.ServerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public class JsonRpcNotificationService implements NotificationService {
   private final ManagementServer managementServer;
   private final MinecraftApi minecraftApi;

   public JsonRpcNotificationService(MinecraftApi p_426698_, ManagementServer p_428555_) {
      this.minecraftApi = p_426698_;
      this.managementServer = p_428555_;
   }

   @Override
   public void playerJoined(ServerPlayer p_425976_) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_JOINED, PlayerDto.from(p_425976_));
   }

   @Override
   public void playerLeft(ServerPlayer p_428853_) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_LEFT, PlayerDto.from(p_428853_));
   }

   @Override
   public void serverStarted() {
      this.broadcastNotification(OutgoingRpcMethods.SERVER_STARTED);
   }

   @Override
   public void serverShuttingDown() {
      this.broadcastNotification(OutgoingRpcMethods.SERVER_SHUTTING_DOWN);
   }

   @Override
   public void serverSaveStarted() {
      this.broadcastNotification(OutgoingRpcMethods.SERVER_SAVE_STARTED);
   }

   @Override
   public void serverSaveCompleted() {
      this.broadcastNotification(OutgoingRpcMethods.SERVER_SAVE_COMPLETED);
   }

   @Override
   public void serverActivityOccured() {
      this.broadcastNotification(OutgoingRpcMethods.SERVER_ACTIVITY_OCCURRED);
   }

   @Override
   public void playerOped(ServerOpListEntry p_423535_) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_OPED, OperatorService.OperatorDto.from(p_423535_));
   }

   @Override
   public void playerDeoped(ServerOpListEntry p_423675_) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_DEOPED, OperatorService.OperatorDto.from(p_423675_));
   }

   @Override
   public void playerAddedToAllowlist(NameAndId p_429666_) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_ADDED_TO_ALLOWLIST, PlayerDto.from(p_429666_));
   }

   @Override
   public void playerRemovedFromAllowlist(NameAndId p_422544_) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_REMOVED_FROM_ALLOWLIST, PlayerDto.from(p_422544_));
   }

   @Override
   public void ipBanned(IpBanListEntry p_428202_) {
      this.broadcastNotification(OutgoingRpcMethods.IP_BANNED, IpBanlistService.IpBanDto.from(p_428202_));
   }

   @Override
   public void ipUnbanned(String p_430123_) {
      this.broadcastNotification(OutgoingRpcMethods.IP_UNBANNED, p_430123_);
   }

   @Override
   public void playerBanned(UserBanListEntry p_425692_) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_BANNED, BanlistService.UserBanDto.from(p_425692_));
   }

   @Override
   public void playerUnbanned(NameAndId p_427919_) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_UNBANNED, PlayerDto.from(p_427919_));
   }

   @Override
   public <T> void onGameRuleChanged(GameRule<T> p_452819_, T p_456636_) {
      this.broadcastNotification(OutgoingRpcMethods.GAMERULE_CHANGED, GameRulesService.getTypedRule(this.minecraftApi, p_452819_, p_456636_));
   }

   @Override
   public void statusHeartbeat() {
      this.broadcastNotification(OutgoingRpcMethods.STATUS_HEARTBEAT, ServerStateService.status(this.minecraftApi));
   }

   private void broadcastNotification(Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> p_423323_) {
      this.managementServer.forEachConnection(p_425706_ -> p_425706_.sendNotification(p_423323_));
   }

   private <Params> void broadcastNotification(Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> p_426626_, Params p_423171_) {
      this.managementServer.forEachConnection(p_428917_ -> p_428917_.sendNotification(p_426626_, p_423171_));
   }
}
