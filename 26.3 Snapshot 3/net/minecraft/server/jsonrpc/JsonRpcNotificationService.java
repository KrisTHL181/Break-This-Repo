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

   public JsonRpcNotificationService(final MinecraftApi minecraftApi, final ManagementServer managementServer) {
      this.minecraftApi = minecraftApi;
      this.managementServer = managementServer;
   }

   @Override
   public void playerJoined(final ServerPlayer player) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_JOINED, PlayerDto.from(player));
   }

   @Override
   public void playerLeft(final ServerPlayer player) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_LEFT, PlayerDto.from(player));
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
   public void worldUpgradeStarted() {
      this.broadcastNotification(OutgoingRpcMethods.WORLD_UPGRADE_STARTED);
   }

   @Override
   public void worldUpgradeProgress(final float progressPercentage) {
      this.broadcastNotification(OutgoingRpcMethods.WORLD_UPGRADE_PROGRESS, progressPercentage);
   }

   @Override
   public void worldUpgradeFinished() {
      this.broadcastNotification(OutgoingRpcMethods.WORLD_UPGRADE_FINISHED);
   }

   @Override
   public void worldUpgradeFailed(final String reason) {
      this.broadcastNotification(OutgoingRpcMethods.WORLD_UPGRADE_FAILED, reason);
   }

   @Override
   public void playerOped(final ServerOpListEntry operator) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_OPED, OperatorService.OperatorDto.from(operator));
   }

   @Override
   public void playerDeoped(final ServerOpListEntry operator) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_DEOPED, OperatorService.OperatorDto.from(operator));
   }

   @Override
   public void playerAddedToAllowlist(final NameAndId player) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_ADDED_TO_ALLOWLIST, PlayerDto.from(player));
   }

   @Override
   public void playerRemovedFromAllowlist(final NameAndId player) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_REMOVED_FROM_ALLOWLIST, PlayerDto.from(player));
   }

   @Override
   public void ipBanned(final IpBanListEntry ban) {
      this.broadcastNotification(OutgoingRpcMethods.IP_BANNED, IpBanlistService.IpBanDto.from(ban));
   }

   @Override
   public void ipUnbanned(final String ip) {
      this.broadcastNotification(OutgoingRpcMethods.IP_UNBANNED, ip);
   }

   @Override
   public void playerBanned(final UserBanListEntry ban) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_BANNED, BanlistService.UserBanDto.from(ban));
   }

   @Override
   public void playerUnbanned(final NameAndId player) {
      this.broadcastNotification(OutgoingRpcMethods.PLAYER_UNBANNED, PlayerDto.from(player));
   }

   @Override
   public <T> void onGameRuleChanged(final GameRule<T> gameRule, final T value) {
      this.broadcastNotification(OutgoingRpcMethods.GAMERULE_CHANGED, GameRulesService.getTypedRule(this.minecraftApi, gameRule, value));
   }

   @Override
   public void statusHeartbeat() {
      this.broadcastNotification(OutgoingRpcMethods.STATUS_HEARTBEAT, ServerStateService.status(this.minecraftApi));
   }

   private void broadcastNotification(final Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> method) {
      this.managementServer.forEachConnection(connection -> connection.sendNotification(method));
   }

   private <Params> void broadcastNotification(final Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> method, final Params params) {
      this.managementServer.forEachConnection(connection -> connection.sendNotification(method, params));
   }
}
