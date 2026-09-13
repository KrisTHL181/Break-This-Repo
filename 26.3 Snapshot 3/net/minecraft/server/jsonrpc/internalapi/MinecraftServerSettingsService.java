package net.minecraft.server.jsonrpc.internalapi;

import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public interface MinecraftServerSettingsService {
   boolean isAutoSave();

   boolean setAutoSave(boolean enabled, ClientInfo clientInfo);

   Difficulty getDifficulty();

   Difficulty setDifficulty(Difficulty difficulty, ClientInfo clientInfo);

   boolean isEnforceWhitelist();

   boolean setEnforceWhitelist(boolean enforce, ClientInfo clientInfo);

   boolean isUsingWhitelist();

   boolean setUsingWhitelist(boolean use, ClientInfo clientInfo);

   int getMaxPlayers();

   int setMaxPlayers(int maxPlayers, ClientInfo clientInfo);

   int getPauseWhenEmptySeconds();

   int setPauseWhenEmptySeconds(int emptySeconds, ClientInfo clientInfo);

   int getPlayerIdleTimeout();

   int setPlayerIdleTimeout(int idleTime, ClientInfo clientInfo);

   boolean allowFlight();

   boolean setAllowFlight(boolean allow, ClientInfo clientInfo);

   int getSpawnProtectionRadius();

   int setSpawnProtectionRadius(int spawnProtection, ClientInfo clientInfo);

   String getMotd();

   String setMotd(String motd, ClientInfo clientInfo);

   boolean forceGameMode();

   boolean setForceGameMode(boolean force, ClientInfo clientInfo);

   GameType getGameMode();

   GameType setGameMode(GameType gameMode, ClientInfo clientInfo);

   int getViewDistance();

   int setViewDistance(int viewDistance, ClientInfo clientInfo);

   int getSimulationDistance();

   int setSimulationDistance(int simulationDistance, ClientInfo clientInfo);

   boolean acceptsTransfers();

   boolean setAcceptsTransfers(boolean accept, ClientInfo clientInfo);

   int getStatusHeartbeatInterval();

   int setStatusHeartbeatInterval(int statusHeartbeatInterval, ClientInfo clientInfo);

   LevelBasedPermissionSet getOperatorUserPermissions();

   LevelBasedPermissionSet setOperatorUserPermissions(LevelBasedPermissionSet level, ClientInfo clientInfo);

   boolean hidesOnlinePlayers();

   boolean setHidesOnlinePlayers(boolean hide, ClientInfo clientInfo);

   boolean repliesToStatus();

   boolean setRepliesToStatus(boolean enable, ClientInfo clientInfo);

   int getEntityBroadcastRangePercentage();

   int setEntityBroadcastRangePercentage(int percentage, ClientInfo clientInfo);
}
