package net.minecraft.server.jsonrpc.internalapi;

import java.util.Objects;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class MinecraftServerSettingsServiceImpl implements MinecraftServerSettingsService {
   private final NotificationManager notificationManager;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftServerSettingsServiceImpl(final NotificationManager notificationManager, final JsonRpcLogger jsonrpcLogger) {
      this.notificationManager = notificationManager;
      this.jsonrpcLogger = jsonrpcLogger;
   }

   private DedicatedServer server() {
      return Objects.requireNonNull(this.notificationManager.server());
   }

   @Override
   public boolean isAutoSave() {
      return this.server().isAutoSave();
   }

   @Override
   public boolean setAutoSave(final boolean enabled, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update autosave from {} to {}", this.isAutoSave(), enabled);
      this.server().setAutoSave(enabled);
      return this.isAutoSave();
   }

   @Override
   public Difficulty getDifficulty() {
      return this.server().getWorldData().getDifficulty();
   }

   @Override
   public Difficulty setDifficulty(final Difficulty difficulty, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update difficulty from '{}' to '{}'", this.getDifficulty(), difficulty);
      this.server().setDifficulty(difficulty);
      return this.getDifficulty();
   }

   @Override
   public boolean isEnforceWhitelist() {
      return this.server().isEnforceWhitelist();
   }

   @Override
   public boolean setEnforceWhitelist(final boolean enforce, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update enforce allowlist from {} to {}", this.isEnforceWhitelist(), enforce);
      this.server().setEnforceWhitelist(enforce);
      this.server().kickUnlistedPlayers();
      return this.isEnforceWhitelist();
   }

   @Override
   public boolean isUsingWhitelist() {
      return this.server().isUsingWhitelist();
   }

   @Override
   public boolean setUsingWhitelist(final boolean use, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update using allowlist from {} to {}", this.isUsingWhitelist(), use);
      this.server().setUsingWhitelist(use);
      this.server().kickUnlistedPlayers();
      return this.isUsingWhitelist();
   }

   @Override
   public int getMaxPlayers() {
      return this.server().getMaxPlayers();
   }

   @Override
   public int setMaxPlayers(final int maxPlayers, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update max players from {} to {}", this.getMaxPlayers(), maxPlayers);
      this.server().setMaxPlayers(maxPlayers);
      return this.getMaxPlayers();
   }

   @Override
   public int getPauseWhenEmptySeconds() {
      return this.server().pauseWhenEmptySeconds();
   }

   @Override
   public int setPauseWhenEmptySeconds(final int emptySeconds, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update pause when empty from {} seconds to {} seconds", this.getPauseWhenEmptySeconds(), emptySeconds);
      this.server().setPauseWhenEmptySeconds(emptySeconds);
      return this.getPauseWhenEmptySeconds();
   }

   @Override
   public int getPlayerIdleTimeout() {
      return this.server().playerIdleTimeout();
   }

   @Override
   public int setPlayerIdleTimeout(final int idleTime, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update player idle timeout from {} minutes to {} minutes", this.getPlayerIdleTimeout(), idleTime);
      this.server().setPlayerIdleTimeout(idleTime);
      return this.getPlayerIdleTimeout();
   }

   @Override
   public boolean allowFlight() {
      return this.server().allowFlight();
   }

   @Override
   public boolean setAllowFlight(final boolean allow, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update allow flight from {} to {}", this.allowFlight(), allow);
      this.server().setAllowFlight(allow);
      return this.allowFlight();
   }

   @Override
   public int getSpawnProtectionRadius() {
      return this.server().spawnProtectionRadius();
   }

   @Override
   public int setSpawnProtectionRadius(final int spawnProtection, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update spawn protection radius from {} to {}", this.getSpawnProtectionRadius(), spawnProtection);
      this.server().setSpawnProtectionRadius(spawnProtection);
      return this.getSpawnProtectionRadius();
   }

   @Override
   public String getMotd() {
      return this.server().getMotd();
   }

   @Override
   public String setMotd(final String motd, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update MOTD from '{}' to '{}'", this.getMotd(), motd);
      this.server().setMotd(motd);
      return this.getMotd();
   }

   @Override
   public boolean forceGameMode() {
      return this.server().forceGameMode();
   }

   @Override
   public boolean setForceGameMode(final boolean force, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update force game mode from {} to {}", this.forceGameMode(), force);
      this.server().setForceGameMode(force);
      return this.forceGameMode();
   }

   @Override
   public GameType getGameMode() {
      return this.server().gameMode();
   }

   @Override
   public GameType setGameMode(final GameType gameMode, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update game mode from '{}' to '{}'", this.getGameMode(), gameMode);
      this.server().setGameMode(gameMode);
      return this.getGameMode();
   }

   @Override
   public int getViewDistance() {
      return this.server().viewDistance();
   }

   @Override
   public int setViewDistance(final int viewDistance, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update view distance from {} to {}", this.getViewDistance(), viewDistance);
      this.server().setViewDistance(viewDistance);
      return this.getViewDistance();
   }

   @Override
   public int getSimulationDistance() {
      return this.server().simulationDistance();
   }

   @Override
   public int setSimulationDistance(final int simulationDistance, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update simulation distance from {} to {}", this.getSimulationDistance(), simulationDistance);
      this.server().setSimulationDistance(simulationDistance);
      return this.getSimulationDistance();
   }

   @Override
   public boolean acceptsTransfers() {
      return this.server().acceptsTransfers();
   }

   @Override
   public boolean setAcceptsTransfers(final boolean accept, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update accepts transfers from {} to {}", this.acceptsTransfers(), accept);
      this.server().setAcceptsTransfers(accept);
      return this.acceptsTransfers();
   }

   @Override
   public int getStatusHeartbeatInterval() {
      return this.server().statusHeartbeatInterval();
   }

   @Override
   public int setStatusHeartbeatInterval(final int statusHeartbeatInterval, final ClientInfo clientInfo) {
      if (this.server().setStatusHeartbeatInterval(statusHeartbeatInterval)) {
         this.jsonrpcLogger.log(clientInfo, "Updated status heartbeat interval from {} to {}", this.getStatusHeartbeatInterval(), statusHeartbeatInterval);
      } else {
         this.jsonrpcLogger
            .log(
               clientInfo,
               "The heartbeat interval was not updated to the new value of {} seconds as the json rpc server is null or a pre-existing heartbeat failed to get canceled",
               statusHeartbeatInterval
            );
      }

      return this.getStatusHeartbeatInterval();
   }

   @Override
   public LevelBasedPermissionSet getOperatorUserPermissions() {
      return this.server().operatorUserPermissions();
   }

   @Override
   public LevelBasedPermissionSet setOperatorUserPermissions(final LevelBasedPermissionSet permissions, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update operator user permission level from {} to {}", this.getOperatorUserPermissions(), permissions.level());
      this.server().setOperatorUserPermissions(permissions);
      return this.getOperatorUserPermissions();
   }

   @Override
   public boolean hidesOnlinePlayers() {
      return this.server().hidesOnlinePlayers();
   }

   @Override
   public boolean setHidesOnlinePlayers(final boolean hide, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update hides online players from {} to {}", this.hidesOnlinePlayers(), hide);
      this.server().setHidesOnlinePlayers(hide);
      return this.hidesOnlinePlayers();
   }

   @Override
   public boolean repliesToStatus() {
      return this.server().repliesToStatus();
   }

   @Override
   public boolean setRepliesToStatus(final boolean enable, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update replies to status from {} to {}", this.repliesToStatus(), enable);
      this.server().setRepliesToStatus(enable);
      return this.repliesToStatus();
   }

   @Override
   public int getEntityBroadcastRangePercentage() {
      return this.server().entityBroadcastRangePercentage();
   }

   @Override
   public int setEntityBroadcastRangePercentage(final int percentage, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Update entity broadcast range percentage from {}% to {}%", this.getEntityBroadcastRangePercentage(), percentage);
      this.server().setEntityBroadcastRangePercentage(percentage);
      return this.getEntityBroadcastRangePercentage();
   }
}
