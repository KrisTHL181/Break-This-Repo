package net.minecraft.server.jsonrpc.internalapi;

import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public interface MinecraftServerSettingsService {
   boolean isAutoSave();

   boolean setAutoSave(boolean var1, ClientInfo var2);

   Difficulty getDifficulty();

   Difficulty setDifficulty(Difficulty var1, ClientInfo var2);

   boolean isEnforceWhitelist();

   boolean setEnforceWhitelist(boolean var1, ClientInfo var2);

   boolean isUsingWhitelist();

   boolean setUsingWhitelist(boolean var1, ClientInfo var2);

   int getMaxPlayers();

   int setMaxPlayers(int var1, ClientInfo var2);

   int getPauseWhenEmptySeconds();

   int setPauseWhenEmptySeconds(int var1, ClientInfo var2);

   int getPlayerIdleTimeout();

   int setPlayerIdleTimeout(int var1, ClientInfo var2);

   boolean allowFlight();

   boolean setAllowFlight(boolean var1, ClientInfo var2);

   int getSpawnProtectionRadius();

   int setSpawnProtectionRadius(int var1, ClientInfo var2);

   String getMotd();

   String setMotd(String var1, ClientInfo var2);

   boolean forceGameMode();

   boolean setForceGameMode(boolean var1, ClientInfo var2);

   GameType getGameMode();

   GameType setGameMode(GameType var1, ClientInfo var2);

   int getViewDistance();

   int setViewDistance(int var1, ClientInfo var2);

   int getSimulationDistance();

   int setSimulationDistance(int var1, ClientInfo var2);

   boolean acceptsTransfers();

   boolean setAcceptsTransfers(boolean var1, ClientInfo var2);

   int getStatusHeartbeatInterval();

   int setStatusHeartbeatInterval(int var1, ClientInfo var2);

   LevelBasedPermissionSet getOperatorUserPermissions();

   LevelBasedPermissionSet setOperatorUserPermissions(LevelBasedPermissionSet var1, ClientInfo var2);

   boolean hidesOnlinePlayers();

   boolean setHidesOnlinePlayers(boolean var1, ClientInfo var2);

   boolean repliesToStatus();

   boolean setRepliesToStatus(boolean var1, ClientInfo var2);

   int getEntityBroadcastRangePercentage();

   int setEntityBroadcastRangePercentage(int var1, ClientInfo var2);
}
