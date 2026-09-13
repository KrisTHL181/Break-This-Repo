package net.minecraft.server.jsonrpc.methods;

import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class ServerSettingsService {
   public static boolean autosave(MinecraftApi p_424353_) {
      return p_424353_.serverSettingsService().isAutoSave();
   }

   public static boolean setAutosave(MinecraftApi p_425396_, boolean p_422958_, ClientInfo p_426312_) {
      return p_425396_.serverSettingsService().setAutoSave(p_422958_, p_426312_);
   }

   public static Difficulty difficulty(MinecraftApi p_431007_) {
      return p_431007_.serverSettingsService().getDifficulty();
   }

   public static Difficulty setDifficulty(MinecraftApi p_429908_, Difficulty p_429862_, ClientInfo p_428486_) {
      return p_429908_.serverSettingsService().setDifficulty(p_429862_, p_428486_);
   }

   public static boolean enforceAllowlist(MinecraftApi p_426260_) {
      return p_426260_.serverSettingsService().isEnforceWhitelist();
   }

   public static boolean setEnforceAllowlist(MinecraftApi p_429881_, boolean p_428362_, ClientInfo p_430917_) {
      return p_429881_.serverSettingsService().setEnforceWhitelist(p_428362_, p_430917_);
   }

   public static boolean usingAllowlist(MinecraftApi p_424570_) {
      return p_424570_.serverSettingsService().isUsingWhitelist();
   }

   public static boolean setUsingAllowlist(MinecraftApi p_430571_, boolean p_423739_, ClientInfo p_422777_) {
      return p_430571_.serverSettingsService().setUsingWhitelist(p_423739_, p_422777_);
   }

   public static int maxPlayers(MinecraftApi p_426532_) {
      return p_426532_.serverSettingsService().getMaxPlayers();
   }

   public static int setMaxPlayers(MinecraftApi p_430198_, int p_431396_, ClientInfo p_429214_) {
      return p_430198_.serverSettingsService().setMaxPlayers(p_431396_, p_429214_);
   }

   public static int pauseWhenEmpty(MinecraftApi p_431533_) {
      return p_431533_.serverSettingsService().getPauseWhenEmptySeconds();
   }

   public static int setPauseWhenEmpty(MinecraftApi p_429343_, int p_430889_, ClientInfo p_431429_) {
      return p_429343_.serverSettingsService().setPauseWhenEmptySeconds(p_430889_, p_431429_);
   }

   public static int playerIdleTimeout(MinecraftApi p_425517_) {
      return p_425517_.serverSettingsService().getPlayerIdleTimeout();
   }

   public static int setPlayerIdleTimeout(MinecraftApi p_425672_, int p_422753_, ClientInfo p_426024_) {
      return p_425672_.serverSettingsService().setPlayerIdleTimeout(p_422753_, p_426024_);
   }

   public static boolean allowFlight(MinecraftApi p_429808_) {
      return p_429808_.serverSettingsService().allowFlight();
   }

   public static boolean setAllowFlight(MinecraftApi p_423299_, boolean p_426950_, ClientInfo p_425679_) {
      return p_423299_.serverSettingsService().setAllowFlight(p_426950_, p_425679_);
   }

   public static int spawnProtection(MinecraftApi p_429919_) {
      return p_429919_.serverSettingsService().getSpawnProtectionRadius();
   }

   public static int setSpawnProtection(MinecraftApi p_425492_, int p_430403_, ClientInfo p_430278_) {
      return p_425492_.serverSettingsService().setSpawnProtectionRadius(p_430403_, p_430278_);
   }

   public static String motd(MinecraftApi p_423995_) {
      return p_423995_.serverSettingsService().getMotd();
   }

   public static String setMotd(MinecraftApi p_429404_, String p_426946_, ClientInfo p_429469_) {
      return p_429404_.serverSettingsService().setMotd(p_426946_, p_429469_);
   }

   public static boolean forceGameMode(MinecraftApi p_424867_) {
      return p_424867_.serverSettingsService().forceGameMode();
   }

   public static boolean setForceGameMode(MinecraftApi p_427301_, boolean p_425732_, ClientInfo p_425501_) {
      return p_427301_.serverSettingsService().setForceGameMode(p_425732_, p_425501_);
   }

   public static GameType gameMode(MinecraftApi p_423954_) {
      return p_423954_.serverSettingsService().getGameMode();
   }

   public static GameType setGameMode(MinecraftApi p_430856_, GameType p_426526_, ClientInfo p_429500_) {
      return p_430856_.serverSettingsService().setGameMode(p_426526_, p_429500_);
   }

   public static int viewDistance(MinecraftApi p_431313_) {
      return p_431313_.serverSettingsService().getViewDistance();
   }

   public static int setViewDistance(MinecraftApi p_429904_, int p_428677_, ClientInfo p_428266_) {
      return p_429904_.serverSettingsService().setViewDistance(p_428677_, p_428266_);
   }

   public static int simulationDistance(MinecraftApi p_426052_) {
      return p_426052_.serverSettingsService().getSimulationDistance();
   }

   public static int setSimulationDistance(MinecraftApi p_422634_, int p_423088_, ClientInfo p_424737_) {
      return p_422634_.serverSettingsService().setSimulationDistance(p_423088_, p_424737_);
   }

   public static boolean acceptTransfers(MinecraftApi p_425856_) {
      return p_425856_.serverSettingsService().acceptsTransfers();
   }

   public static boolean setAcceptTransfers(MinecraftApi p_422379_, boolean p_423709_, ClientInfo p_425374_) {
      return p_422379_.serverSettingsService().setAcceptsTransfers(p_423709_, p_425374_);
   }

   public static int statusHeartbeatInterval(MinecraftApi p_431451_) {
      return p_431451_.serverSettingsService().getStatusHeartbeatInterval();
   }

   public static int setStatusHeartbeatInterval(MinecraftApi p_423091_, int p_425586_, ClientInfo p_428204_) {
      return p_423091_.serverSettingsService().setStatusHeartbeatInterval(p_425586_, p_428204_);
   }

   public static PermissionLevel operatorUserPermissionLevel(MinecraftApi p_426510_) {
      return p_426510_.serverSettingsService().getOperatorUserPermissions().level();
   }

   public static PermissionLevel setOperatorUserPermissionLevel(MinecraftApi p_427522_, PermissionLevel p_455568_, ClientInfo p_422880_) {
      return p_427522_.serverSettingsService().setOperatorUserPermissions(LevelBasedPermissionSet.forLevel(p_455568_), p_422880_).level();
   }

   public static boolean hidesOnlinePlayers(MinecraftApi p_431751_) {
      return p_431751_.serverSettingsService().hidesOnlinePlayers();
   }

   public static boolean setHidesOnlinePlayers(MinecraftApi p_425877_, boolean p_422951_, ClientInfo p_425243_) {
      return p_425877_.serverSettingsService().setHidesOnlinePlayers(p_422951_, p_425243_);
   }

   public static boolean repliesToStatus(MinecraftApi p_424273_) {
      return p_424273_.serverSettingsService().repliesToStatus();
   }

   public static boolean setRepliesToStatus(MinecraftApi p_430970_, boolean p_425276_, ClientInfo p_427060_) {
      return p_430970_.serverSettingsService().setRepliesToStatus(p_425276_, p_427060_);
   }

   public static int entityBroadcastRangePercentage(MinecraftApi p_427737_) {
      return p_427737_.serverSettingsService().getEntityBroadcastRangePercentage();
   }

   public static int setEntityBroadcastRangePercentage(MinecraftApi p_427823_, int p_430173_, ClientInfo p_422969_) {
      return p_427823_.serverSettingsService().setEntityBroadcastRangePercentage(p_430173_, p_422969_);
   }
}
