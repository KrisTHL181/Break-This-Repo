package net.minecraft.server.jsonrpc.methods;

import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class ServerSettingsService {
   public static boolean autosave(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().isAutoSave();
   }

   public static boolean setAutosave(final MinecraftApi minecraftApi, final boolean enabled, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setAutoSave(enabled, clientInfo);
   }

   public static Difficulty difficulty(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getDifficulty();
   }

   public static Difficulty setDifficulty(final MinecraftApi minecraftApi, final Difficulty difficulty, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setDifficulty(difficulty, clientInfo);
   }

   public static boolean enforceAllowlist(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().isEnforceWhitelist();
   }

   public static boolean setEnforceAllowlist(final MinecraftApi minecraftApi, final boolean enforce, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setEnforceWhitelist(enforce, clientInfo);
   }

   public static boolean usingAllowlist(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().isUsingWhitelist();
   }

   public static boolean setUsingAllowlist(final MinecraftApi minecraftApi, final boolean use, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setUsingWhitelist(use, clientInfo);
   }

   public static int maxPlayers(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getMaxPlayers();
   }

   public static int setMaxPlayers(final MinecraftApi minecraftApi, final int maxPlayers, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setMaxPlayers(maxPlayers, clientInfo);
   }

   public static int pauseWhenEmpty(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getPauseWhenEmptySeconds();
   }

   public static int setPauseWhenEmpty(final MinecraftApi minecraftApi, final int emptySeconds, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setPauseWhenEmptySeconds(emptySeconds, clientInfo);
   }

   public static int playerIdleTimeout(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getPlayerIdleTimeout();
   }

   public static int setPlayerIdleTimeout(final MinecraftApi minecraftApi, final int idleTime, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setPlayerIdleTimeout(idleTime, clientInfo);
   }

   public static boolean allowFlight(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().allowFlight();
   }

   public static boolean setAllowFlight(final MinecraftApi minecraftApi, final boolean allow, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setAllowFlight(allow, clientInfo);
   }

   public static int spawnProtection(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getSpawnProtectionRadius();
   }

   public static int setSpawnProtection(final MinecraftApi minecraftApi, final int spawnProtection, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setSpawnProtectionRadius(spawnProtection, clientInfo);
   }

   public static String motd(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getMotd();
   }

   public static String setMotd(final MinecraftApi minecraftApi, final String motd, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setMotd(motd, clientInfo);
   }

   public static boolean forceGameMode(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().forceGameMode();
   }

   public static boolean setForceGameMode(final MinecraftApi minecraftApi, final boolean force, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setForceGameMode(force, clientInfo);
   }

   public static GameType gameMode(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getGameMode();
   }

   public static GameType setGameMode(final MinecraftApi minecraftApi, final GameType gameMode, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setGameMode(gameMode, clientInfo);
   }

   public static int viewDistance(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getViewDistance();
   }

   public static int setViewDistance(final MinecraftApi minecraftApi, final int viewDistance, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setViewDistance(viewDistance, clientInfo);
   }

   public static int simulationDistance(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getSimulationDistance();
   }

   public static int setSimulationDistance(final MinecraftApi minecraftApi, final int simulationDistance, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setSimulationDistance(simulationDistance, clientInfo);
   }

   public static boolean acceptTransfers(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().acceptsTransfers();
   }

   public static boolean setAcceptTransfers(final MinecraftApi minecraftApi, final boolean accept, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setAcceptsTransfers(accept, clientInfo);
   }

   public static int statusHeartbeatInterval(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getStatusHeartbeatInterval();
   }

   public static int setStatusHeartbeatInterval(final MinecraftApi minecraftApi, final int statusHeartbeatInterval, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setStatusHeartbeatInterval(statusHeartbeatInterval, clientInfo);
   }

   public static PermissionLevel operatorUserPermissionLevel(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getOperatorUserPermissions().level();
   }

   public static PermissionLevel setOperatorUserPermissionLevel(final MinecraftApi minecraftApi, final PermissionLevel level, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setOperatorUserPermissions(LevelBasedPermissionSet.forLevel(level), clientInfo).level();
   }

   public static boolean hidesOnlinePlayers(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().hidesOnlinePlayers();
   }

   public static boolean setHidesOnlinePlayers(final MinecraftApi minecraftApi, final boolean hide, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setHidesOnlinePlayers(hide, clientInfo);
   }

   public static boolean repliesToStatus(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().repliesToStatus();
   }

   public static boolean setRepliesToStatus(final MinecraftApi minecraftApi, final boolean enable, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setRepliesToStatus(enable, clientInfo);
   }

   public static int entityBroadcastRangePercentage(final MinecraftApi minecraftApi) {
      return minecraftApi.serverSettingsService().getEntityBroadcastRangePercentage();
   }

   public static int setEntityBroadcastRangePercentage(final MinecraftApi minecraftApi, final int percentage, final ClientInfo clientInfo) {
      return minecraftApi.serverSettingsService().setEntityBroadcastRangePercentage(percentage, clientInfo);
   }
}
