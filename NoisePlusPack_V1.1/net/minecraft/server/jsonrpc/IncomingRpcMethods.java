package net.minecraft.server.jsonrpc;

import net.minecraft.core.Registry;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.methods.AllowlistService;
import net.minecraft.server.jsonrpc.methods.BanlistService;
import net.minecraft.server.jsonrpc.methods.DiscoveryService;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.server.jsonrpc.methods.IpBanlistService;
import net.minecraft.server.jsonrpc.methods.OperatorService;
import net.minecraft.server.jsonrpc.methods.PlayerService;
import net.minecraft.server.jsonrpc.methods.ServerSettingsService;
import net.minecraft.server.jsonrpc.methods.ServerStateService;

public class IncomingRpcMethods {
   public static IncomingRpcMethod<?, ?> bootstrap(Registry<IncomingRpcMethod<?, ?>> p_429047_) {
      registerAllowListService(p_429047_);
      registerBanlistService(p_429047_);
      registerIpBanlistService(p_429047_);
      registerPlayerService(p_429047_);
      registerOperatorService(p_429047_);
      registerServerStateService(p_429047_);
      registerServerSettingsService(p_429047_);
      registerGameRuleService(p_429047_);
      return IncomingRpcMethod.<DiscoveryService.DiscoverResponse>method(p_423947_ -> DiscoveryService.discover(Schema.getSchemaRegistry()))
         .undiscoverable()
         .notOnMainThread()
         .response("result", Schema.DISCOVERY_SCHEMA)
         .register(p_429047_, "rpc.discover");
   }

   private static void registerAllowListService(Registry<IncomingRpcMethod<?, ?>> p_431611_) {
      IncomingRpcMethod.method(AllowlistService::get)
         .description("Get the allowlist")
         .response("allowlist", Schema.PLAYER_SCHEMA.asArray())
         .register(p_431611_, "allowlist");
      IncomingRpcMethod.method(AllowlistService::set)
         .description("Set the allowlist")
         .param("players", Schema.PLAYER_SCHEMA.asArray())
         .response("allowlist", Schema.PLAYER_SCHEMA.asArray())
         .register(p_431611_, "allowlist/set");
      IncomingRpcMethod.method(AllowlistService::add)
         .description("Add players to allowlist")
         .param("add", Schema.PLAYER_SCHEMA.asArray())
         .response("allowlist", Schema.PLAYER_SCHEMA.asArray())
         .register(p_431611_, "allowlist/add");
      IncomingRpcMethod.method(AllowlistService::remove)
         .description("Remove players from allowlist")
         .param("remove", Schema.PLAYER_SCHEMA.asArray())
         .response("allowlist", Schema.PLAYER_SCHEMA.asArray())
         .register(p_431611_, "allowlist/remove");
      IncomingRpcMethod.method(AllowlistService::clear)
         .description("Clear all players in allowlist")
         .response("allowlist", Schema.PLAYER_SCHEMA.asArray())
         .register(p_431611_, "allowlist/clear");
   }

   private static void registerBanlistService(Registry<IncomingRpcMethod<?, ?>> p_425539_) {
      IncomingRpcMethod.method(BanlistService::get)
         .description("Get the ban list")
         .response("banlist", Schema.PLAYER_BAN_SCHEMA.asArray())
         .register(p_425539_, "bans");
      IncomingRpcMethod.method(BanlistService::set)
         .description("Set the banlist")
         .param("bans", Schema.PLAYER_BAN_SCHEMA.asArray())
         .response("banlist", Schema.PLAYER_BAN_SCHEMA.asArray())
         .register(p_425539_, "bans/set");
      IncomingRpcMethod.method(BanlistService::add)
         .description("Add players to ban list")
         .param("add", Schema.PLAYER_BAN_SCHEMA.asArray())
         .response("banlist", Schema.PLAYER_BAN_SCHEMA.asArray())
         .register(p_425539_, "bans/add");
      IncomingRpcMethod.method(BanlistService::remove)
         .description("Remove players from ban list")
         .param("remove", Schema.PLAYER_SCHEMA.asArray())
         .response("banlist", Schema.PLAYER_BAN_SCHEMA.asArray())
         .register(p_425539_, "bans/remove");
      IncomingRpcMethod.method(BanlistService::clear)
         .description("Clear all players in ban list")
         .response("banlist", Schema.PLAYER_BAN_SCHEMA.asArray())
         .register(p_425539_, "bans/clear");
   }

   private static void registerIpBanlistService(Registry<IncomingRpcMethod<?, ?>> p_429036_) {
      IncomingRpcMethod.method(IpBanlistService::get)
         .description("Get the ip ban list")
         .response("banlist", Schema.IP_BAN_SCHEMA.asArray())
         .register(p_429036_, "ip_bans");
      IncomingRpcMethod.method(IpBanlistService::set)
         .description("Set the ip banlist")
         .param("banlist", Schema.IP_BAN_SCHEMA.asArray())
         .response("banlist", Schema.IP_BAN_SCHEMA.asArray())
         .register(p_429036_, "ip_bans/set");
      IncomingRpcMethod.method(IpBanlistService::add)
         .description("Add ip to ban list")
         .param("add", Schema.INCOMING_IP_BAN_SCHEMA.asArray())
         .response("banlist", Schema.IP_BAN_SCHEMA.asArray())
         .register(p_429036_, "ip_bans/add");
      IncomingRpcMethod.method(IpBanlistService::remove)
         .description("Remove ip from ban list")
         .param("ip", Schema.STRING_SCHEMA.asArray())
         .response("banlist", Schema.IP_BAN_SCHEMA.asArray())
         .register(p_429036_, "ip_bans/remove");
      IncomingRpcMethod.method(IpBanlistService::clear)
         .description("Clear all ips in ban list")
         .response("banlist", Schema.IP_BAN_SCHEMA.asArray())
         .register(p_429036_, "ip_bans/clear");
   }

   private static void registerPlayerService(Registry<IncomingRpcMethod<?, ?>> p_430004_) {
      IncomingRpcMethod.method(PlayerService::get)
         .description("Get all connected players")
         .response("players", Schema.PLAYER_SCHEMA.asArray())
         .register(p_430004_, "players");
      IncomingRpcMethod.method(PlayerService::kick)
         .description("Kick players")
         .param("kick", Schema.KICK_PLAYER_SCHEMA.asArray())
         .response("kicked", Schema.PLAYER_SCHEMA.asArray())
         .register(p_430004_, "players/kick");
   }

   private static void registerOperatorService(Registry<IncomingRpcMethod<?, ?>> p_425870_) {
      IncomingRpcMethod.method(OperatorService::get)
         .description("Get all oped players")
         .response("operators", Schema.OPERATOR_SCHEMA.asArray())
         .register(p_425870_, "operators");
      IncomingRpcMethod.method(OperatorService::set)
         .description("Set all oped players")
         .param("operators", Schema.OPERATOR_SCHEMA.asArray())
         .response("operators", Schema.OPERATOR_SCHEMA.asArray())
         .register(p_425870_, "operators/set");
      IncomingRpcMethod.method(OperatorService::add)
         .description("Op players")
         .param("add", Schema.OPERATOR_SCHEMA.asArray())
         .response("operators", Schema.OPERATOR_SCHEMA.asArray())
         .register(p_425870_, "operators/add");
      IncomingRpcMethod.method(OperatorService::remove)
         .description("Deop players")
         .param("remove", Schema.PLAYER_SCHEMA.asArray())
         .response("operators", Schema.OPERATOR_SCHEMA.asArray())
         .register(p_425870_, "operators/remove");
      IncomingRpcMethod.method(OperatorService::clear)
         .description("Deop all players")
         .response("operators", Schema.OPERATOR_SCHEMA.asArray())
         .register(p_425870_, "operators/clear");
   }

   private static void registerServerStateService(Registry<IncomingRpcMethod<?, ?>> p_430357_) {
      IncomingRpcMethod.method(ServerStateService::status)
         .description("Get server status")
         .response("status", Schema.SERVER_STATE_SCHEMA.asRef())
         .register(p_430357_, "server/status");
      IncomingRpcMethod.method(ServerStateService::save)
         .description("Save server state")
         .param("flush", Schema.BOOL_SCHEMA)
         .response("saving", Schema.BOOL_SCHEMA)
         .register(p_430357_, "server/save");
      IncomingRpcMethod.method(ServerStateService::stop).description("Stop server").response("stopping", Schema.BOOL_SCHEMA).register(p_430357_, "server/stop");
      IncomingRpcMethod.method(ServerStateService::systemMessage)
         .description("Send a system message")
         .param("message", Schema.SYSTEM_MESSAGE_SCHEMA.asRef())
         .response("sent", Schema.BOOL_SCHEMA)
         .register(p_430357_, "server/system_message");
   }

   private static void registerServerSettingsService(Registry<IncomingRpcMethod<?, ?>> p_423752_) {
      IncomingRpcMethod.method(ServerSettingsService::autosave)
         .description("Get whether automatic world saving is enabled on the server")
         .response("enabled", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/autosave");
      IncomingRpcMethod.method(ServerSettingsService::setAutosave)
         .description("Enable or disable automatic world saving on the server")
         .param("enable", Schema.BOOL_SCHEMA)
         .response("enabled", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/autosave/set");
      IncomingRpcMethod.method(ServerSettingsService::difficulty)
         .description("Get the current difficulty level of the server")
         .response("difficulty", Schema.DIFFICULTY_SCHEMA.asRef())
         .register(p_423752_, "serversettings/difficulty");
      IncomingRpcMethod.method(ServerSettingsService::setDifficulty)
         .description("Set the difficulty level of the server")
         .param("difficulty", Schema.DIFFICULTY_SCHEMA.asRef())
         .response("difficulty", Schema.DIFFICULTY_SCHEMA.asRef())
         .register(p_423752_, "serversettings/difficulty/set");
      IncomingRpcMethod.method(ServerSettingsService::enforceAllowlist)
         .description("Get whether allowlist enforcement is enabled (kicks players immediately when removed from allowlist)")
         .response("enforced", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/enforce_allowlist");
      IncomingRpcMethod.method(ServerSettingsService::setEnforceAllowlist)
         .description("Enable or disable allowlist enforcement (when enabled, players are kicked immediately upon removal from allowlist)")
         .param("enforce", Schema.BOOL_SCHEMA)
         .response("enforced", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/enforce_allowlist/set");
      IncomingRpcMethod.method(ServerSettingsService::usingAllowlist)
         .description("Get whether the allowlist is enabled on the server")
         .response("used", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/use_allowlist");
      IncomingRpcMethod.method(ServerSettingsService::setUsingAllowlist)
         .description("Enable or disable the allowlist on the server (controls whether only allowlisted players can join)")
         .param("use", Schema.BOOL_SCHEMA)
         .response("used", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/use_allowlist/set");
      IncomingRpcMethod.method(ServerSettingsService::maxPlayers)
         .description("Get the maximum number of players allowed to connect to the server")
         .response("max", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/max_players");
      IncomingRpcMethod.method(ServerSettingsService::setMaxPlayers)
         .description("Set the maximum number of players allowed to connect to the server")
         .param("max", Schema.INT_SCHEMA)
         .response("max", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/max_players/set");
      IncomingRpcMethod.method(ServerSettingsService::pauseWhenEmpty)
         .description("Get the number of seconds before the game is automatically paused when no players are online")
         .response("seconds", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/pause_when_empty_seconds");
      IncomingRpcMethod.method(ServerSettingsService::setPauseWhenEmpty)
         .description("Set the number of seconds before the game is automatically paused when no players are online")
         .param("seconds", Schema.INT_SCHEMA)
         .response("seconds", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/pause_when_empty_seconds/set");
      IncomingRpcMethod.method(ServerSettingsService::playerIdleTimeout)
         .description("Get the number of seconds before idle players are automatically kicked from the server")
         .response("seconds", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/player_idle_timeout");
      IncomingRpcMethod.method(ServerSettingsService::setPlayerIdleTimeout)
         .description("Set the number of seconds before idle players are automatically kicked from the server")
         .param("seconds", Schema.INT_SCHEMA)
         .response("seconds", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/player_idle_timeout/set");
      IncomingRpcMethod.method(ServerSettingsService::allowFlight)
         .description("Get whether flight is allowed for players in Survival mode")
         .response("allowed", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/allow_flight");
      IncomingRpcMethod.method(ServerSettingsService::setAllowFlight)
         .description("Allow or disallow flight for players in Survival mode")
         .param("allow", Schema.BOOL_SCHEMA)
         .response("allowed", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/allow_flight/set");
      IncomingRpcMethod.method(ServerSettingsService::motd)
         .description("Get the server's message of the day displayed to players")
         .response("message", Schema.STRING_SCHEMA)
         .register(p_423752_, "serversettings/motd");
      IncomingRpcMethod.method(ServerSettingsService::setMotd)
         .description("Set the server's message of the day displayed to players")
         .param("message", Schema.STRING_SCHEMA)
         .response("message", Schema.STRING_SCHEMA)
         .register(p_423752_, "serversettings/motd/set");
      IncomingRpcMethod.method(ServerSettingsService::spawnProtection)
         .description("Get the spawn protection radius in blocks (only operators can edit within this area)")
         .response("radius", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/spawn_protection_radius");
      IncomingRpcMethod.method(ServerSettingsService::setSpawnProtection)
         .description("Set the spawn protection radius in blocks (only operators can edit within this area)")
         .param("radius", Schema.INT_SCHEMA)
         .response("radius", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/spawn_protection_radius/set");
      IncomingRpcMethod.method(ServerSettingsService::forceGameMode)
         .description("Get whether players are forced to use the server's default game mode")
         .response("forced", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/force_game_mode");
      IncomingRpcMethod.method(ServerSettingsService::setForceGameMode)
         .description("Enable or disable forcing players to use the server's default game mode")
         .param("force", Schema.BOOL_SCHEMA)
         .response("forced", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/force_game_mode/set");
      IncomingRpcMethod.method(ServerSettingsService::gameMode)
         .description("Get the server's default game mode")
         .response("mode", Schema.GAME_TYPE_SCHEMA.asRef())
         .register(p_423752_, "serversettings/game_mode");
      IncomingRpcMethod.method(ServerSettingsService::setGameMode)
         .description("Set the server's default game mode")
         .param("mode", Schema.GAME_TYPE_SCHEMA.asRef())
         .response("mode", Schema.GAME_TYPE_SCHEMA.asRef())
         .register(p_423752_, "serversettings/game_mode/set");
      IncomingRpcMethod.method(ServerSettingsService::viewDistance)
         .description("Get the server's view distance in chunks")
         .response("distance", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/view_distance");
      IncomingRpcMethod.method(ServerSettingsService::setViewDistance)
         .description("Set the server's view distance in chunks")
         .param("distance", Schema.INT_SCHEMA)
         .response("distance", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/view_distance/set");
      IncomingRpcMethod.method(ServerSettingsService::simulationDistance)
         .description("Get the server's simulation distance in chunks")
         .response("distance", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/simulation_distance");
      IncomingRpcMethod.method(ServerSettingsService::setSimulationDistance)
         .description("Set the server's simulation distance in chunks")
         .param("distance", Schema.INT_SCHEMA)
         .response("distance", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/simulation_distance/set");
      IncomingRpcMethod.method(ServerSettingsService::acceptTransfers)
         .description("Get whether the server accepts player transfers from other servers")
         .response("accepted", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/accept_transfers");
      IncomingRpcMethod.method(ServerSettingsService::setAcceptTransfers)
         .description("Enable or disable accepting player transfers from other servers")
         .param("accept", Schema.BOOL_SCHEMA)
         .response("accepted", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/accept_transfers/set");
      IncomingRpcMethod.method(ServerSettingsService::statusHeartbeatInterval)
         .description("Get the interval in seconds between server status heartbeats")
         .response("seconds", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/status_heartbeat_interval");
      IncomingRpcMethod.method(ServerSettingsService::setStatusHeartbeatInterval)
         .description("Set the interval in seconds between server status heartbeats")
         .param("seconds", Schema.INT_SCHEMA)
         .response("seconds", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/status_heartbeat_interval/set");
      IncomingRpcMethod.method(ServerSettingsService::operatorUserPermissionLevel)
         .description("Get default operator permission level")
         .response("level", Schema.PERMISSION_LEVEL_SCHEMA)
         .register(p_423752_, "serversettings/operator_user_permission_level");
      IncomingRpcMethod.method(ServerSettingsService::setOperatorUserPermissionLevel)
         .description("Set default operator permission level")
         .param("level", Schema.PERMISSION_LEVEL_SCHEMA)
         .response("level", Schema.PERMISSION_LEVEL_SCHEMA)
         .register(p_423752_, "serversettings/operator_user_permission_level/set");
      IncomingRpcMethod.method(ServerSettingsService::hidesOnlinePlayers)
         .description("Get whether the server hides online player information from status queries")
         .response("hidden", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/hide_online_players");
      IncomingRpcMethod.method(ServerSettingsService::setHidesOnlinePlayers)
         .description("Enable or disable hiding online player information from status queries")
         .param("hide", Schema.BOOL_SCHEMA)
         .response("hidden", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/hide_online_players/set");
      IncomingRpcMethod.method(ServerSettingsService::repliesToStatus)
         .description("Get whether the server responds to connection status requests")
         .response("enabled", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/status_replies");
      IncomingRpcMethod.method(ServerSettingsService::setRepliesToStatus)
         .description("Enable or disable the server responding to connection status requests")
         .param("enable", Schema.BOOL_SCHEMA)
         .response("enabled", Schema.BOOL_SCHEMA)
         .register(p_423752_, "serversettings/status_replies/set");
      IncomingRpcMethod.method(ServerSettingsService::entityBroadcastRangePercentage)
         .description("Get the entity broadcast range as a percentage")
         .response("percentage_points", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/entity_broadcast_range");
      IncomingRpcMethod.method(ServerSettingsService::setEntityBroadcastRangePercentage)
         .description("Set the entity broadcast range as a percentage")
         .param("percentage_points", Schema.INT_SCHEMA)
         .response("percentage_points", Schema.INT_SCHEMA)
         .register(p_423752_, "serversettings/entity_broadcast_range/set");
   }

   private static void registerGameRuleService(Registry<IncomingRpcMethod<?, ?>> p_428893_) {
      IncomingRpcMethod.method(GameRulesService::get)
         .description("Get the available game rule keys and their current values")
         .response("gamerules", Schema.TYPED_GAME_RULE_SCHEMA.asRef().asArray())
         .register(p_428893_, "gamerules");
      IncomingRpcMethod.method(GameRulesService::update)
         .description("Update game rule value")
         .param("gamerule", Schema.UNTYPED_GAME_RULE_SCHEMA.asRef())
         .response("gamerule", Schema.TYPED_GAME_RULE_SCHEMA.asRef())
         .register(p_428893_, "gamerules/update");
   }
}
