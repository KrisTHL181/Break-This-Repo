package net.minecraft.server.jsonrpc.internalapi;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class MinecraftServerSettingsServiceImpl implements MinecraftServerSettingsService {
   private final DedicatedServer server;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftServerSettingsServiceImpl(DedicatedServer p_430415_, JsonRpcLogger p_429541_) {
      this.server = p_430415_;
      this.jsonrpcLogger = p_429541_;
   }

   @Override
   public boolean isAutoSave() {
      return this.server.isAutoSave();
   }

   @Override
   public boolean setAutoSave(boolean p_427232_, ClientInfo p_422623_) {
      this.jsonrpcLogger.log(p_422623_, "Update autosave from {} to {}", this.isAutoSave(), p_427232_);
      this.server.setAutoSave(p_427232_);
      return this.isAutoSave();
   }

   @Override
   public Difficulty getDifficulty() {
      return this.server.getWorldData().getDifficulty();
   }

   @Override
   public Difficulty setDifficulty(Difficulty p_427494_, ClientInfo p_424266_) {
      this.jsonrpcLogger.log(p_424266_, "Update difficulty from '{}' to '{}'", this.getDifficulty(), p_427494_);
      this.server.setDifficulty(p_427494_);
      return this.getDifficulty();
   }

   @Override
   public boolean isEnforceWhitelist() {
      return this.server.isEnforceWhitelist();
   }

   @Override
   public boolean setEnforceWhitelist(boolean p_428449_, ClientInfo p_428786_) {
      this.jsonrpcLogger.log(p_428786_, "Update enforce allowlist from {} to {}", this.isEnforceWhitelist(), p_428449_);
      this.server.setEnforceWhitelist(p_428449_);
      this.server.kickUnlistedPlayers();
      return this.isEnforceWhitelist();
   }

   @Override
   public boolean isUsingWhitelist() {
      return this.server.isUsingWhitelist();
   }

   @Override
   public boolean setUsingWhitelist(boolean p_430560_, ClientInfo p_427821_) {
      this.jsonrpcLogger.log(p_427821_, "Update using allowlist from {} to {}", this.isUsingWhitelist(), p_430560_);
      this.server.setUsingWhitelist(p_430560_);
      this.server.kickUnlistedPlayers();
      return this.isUsingWhitelist();
   }

   @Override
   public int getMaxPlayers() {
      return this.server.getMaxPlayers();
   }

   @Override
   public int setMaxPlayers(int p_428562_, ClientInfo p_423917_) {
      this.jsonrpcLogger.log(p_423917_, "Update max players from {} to {}", this.getMaxPlayers(), p_428562_);
      this.server.setMaxPlayers(p_428562_);
      return this.getMaxPlayers();
   }

   @Override
   public int getPauseWhenEmptySeconds() {
      return this.server.pauseWhenEmptySeconds();
   }

   @Override
   public int setPauseWhenEmptySeconds(int p_429273_, ClientInfo p_423347_) {
      this.jsonrpcLogger.log(p_423347_, "Update pause when empty from {} seconds to {} seconds", this.getPauseWhenEmptySeconds(), p_429273_);
      this.server.setPauseWhenEmptySeconds(p_429273_);
      return this.getPauseWhenEmptySeconds();
   }

   @Override
   public int getPlayerIdleTimeout() {
      return this.server.playerIdleTimeout();
   }

   @Override
   public int setPlayerIdleTimeout(int p_426911_, ClientInfo p_427509_) {
      this.jsonrpcLogger.log(p_427509_, "Update player idle timeout from {} minutes to {} minutes", this.getPlayerIdleTimeout(), p_426911_);
      this.server.setPlayerIdleTimeout(p_426911_);
      return this.getPlayerIdleTimeout();
   }

   @Override
   public boolean allowFlight() {
      return this.server.allowFlight();
   }

   @Override
   public boolean setAllowFlight(boolean p_424442_, ClientInfo p_425314_) {
      this.jsonrpcLogger.log(p_425314_, "Update allow flight from {} to {}", this.allowFlight(), p_424442_);
      this.server.setAllowFlight(p_424442_);
      return this.allowFlight();
   }

   @Override
   public int getSpawnProtectionRadius() {
      return this.server.spawnProtectionRadius();
   }

   @Override
   public int setSpawnProtectionRadius(int p_422793_, ClientInfo p_431089_) {
      this.jsonrpcLogger.log(p_431089_, "Update spawn protection radius from {} to {}", this.getSpawnProtectionRadius(), p_422793_);
      this.server.setSpawnProtectionRadius(p_422793_);
      return this.getSpawnProtectionRadius();
   }

   @Override
   public String getMotd() {
      return this.server.getMotd();
   }

   @Override
   public String setMotd(String p_426354_, ClientInfo p_424350_) {
      this.jsonrpcLogger.log(p_424350_, "Update MOTD from '{}' to '{}'", this.getMotd(), p_426354_);
      this.server.setMotd(p_426354_);
      return this.getMotd();
   }

   @Override
   public boolean forceGameMode() {
      return this.server.forceGameMode();
   }

   @Override
   public boolean setForceGameMode(boolean p_428526_, ClientInfo p_430148_) {
      this.jsonrpcLogger.log(p_430148_, "Update force game mode from {} to {}", this.forceGameMode(), p_428526_);
      this.server.setForceGameMode(p_428526_);
      return this.forceGameMode();
   }

   @Override
   public GameType getGameMode() {
      return this.server.gameMode();
   }

   @Override
   public GameType setGameMode(GameType p_426507_, ClientInfo p_422347_) {
      this.jsonrpcLogger.log(p_422347_, "Update game mode from '{}' to '{}'", this.getGameMode(), p_426507_);
      this.server.setGameMode(p_426507_);
      return this.getGameMode();
   }

   @Override
   public int getViewDistance() {
      return this.server.viewDistance();
   }

   @Override
   public int setViewDistance(int p_425038_, ClientInfo p_431159_) {
      this.jsonrpcLogger.log(p_431159_, "Update view distance from {} to {}", this.getViewDistance(), p_425038_);
      this.server.setViewDistance(p_425038_);
      return this.getViewDistance();
   }

   @Override
   public int getSimulationDistance() {
      return this.server.simulationDistance();
   }

   @Override
   public int setSimulationDistance(int p_422852_, ClientInfo p_425793_) {
      this.jsonrpcLogger.log(p_425793_, "Update simulation distance from {} to {}", this.getSimulationDistance(), p_422852_);
      this.server.setSimulationDistance(p_422852_);
      return this.getSimulationDistance();
   }

   @Override
   public boolean acceptsTransfers() {
      return this.server.acceptsTransfers();
   }

   @Override
   public boolean setAcceptsTransfers(boolean p_423868_, ClientInfo p_425463_) {
      this.jsonrpcLogger.log(p_425463_, "Update accepts transfers from {} to {}", this.acceptsTransfers(), p_423868_);
      this.server.setAcceptsTransfers(p_423868_);
      return this.acceptsTransfers();
   }

   @Override
   public int getStatusHeartbeatInterval() {
      return this.server.statusHeartbeatInterval();
   }

   @Override
   public int setStatusHeartbeatInterval(int p_429244_, ClientInfo p_428223_) {
      this.jsonrpcLogger.log(p_428223_, "Update status heartbeat interval from {} to {}", this.getStatusHeartbeatInterval(), p_429244_);
      this.server.setStatusHeartbeatInterval(p_429244_);
      return this.getStatusHeartbeatInterval();
   }

   @Override
   public LevelBasedPermissionSet getOperatorUserPermissions() {
      return this.server.operatorUserPermissions();
   }

   @Override
   public LevelBasedPermissionSet setOperatorUserPermissions(LevelBasedPermissionSet p_460159_, ClientInfo p_455403_) {
      this.jsonrpcLogger.log(p_455403_, "Update operator user permission level from {} to {}", this.getOperatorUserPermissions(), p_460159_.level());
      this.server.setOperatorUserPermissions(p_460159_);
      return this.getOperatorUserPermissions();
   }

   @Override
   public boolean hidesOnlinePlayers() {
      return this.server.hidesOnlinePlayers();
   }

   @Override
   public boolean setHidesOnlinePlayers(boolean p_424347_, ClientInfo p_427751_) {
      this.jsonrpcLogger.log(p_427751_, "Update hides online players from {} to {}", this.hidesOnlinePlayers(), p_424347_);
      this.server.setHidesOnlinePlayers(p_424347_);
      return this.hidesOnlinePlayers();
   }

   @Override
   public boolean repliesToStatus() {
      return this.server.repliesToStatus();
   }

   @Override
   public boolean setRepliesToStatus(boolean p_427010_, ClientInfo p_428878_) {
      this.jsonrpcLogger.log(p_428878_, "Update replies to status from {} to {}", this.repliesToStatus(), p_427010_);
      this.server.setRepliesToStatus(p_427010_);
      return this.repliesToStatus();
   }

   @Override
   public int getEntityBroadcastRangePercentage() {
      return this.server.entityBroadcastRangePercentage();
   }

   @Override
   public int setEntityBroadcastRangePercentage(int p_424809_, ClientInfo p_429165_) {
      this.jsonrpcLogger.log(p_429165_, "Update entity broadcast range percentage from {}% to {}%", this.getEntityBroadcastRangePercentage(), p_424809_);
      this.server.setEntityBroadcastRangePercentage(p_424809_);
      return this.getEntityBroadcastRangePercentage();
   }
}
