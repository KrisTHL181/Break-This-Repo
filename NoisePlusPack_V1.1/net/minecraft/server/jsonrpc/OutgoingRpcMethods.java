package net.minecraft.server.jsonrpc;

import net.minecraft.core.Holder;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.methods.BanlistService;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.server.jsonrpc.methods.IpBanlistService;
import net.minecraft.server.jsonrpc.methods.OperatorService;
import net.minecraft.server.jsonrpc.methods.ServerStateService;

public class OutgoingRpcMethods {
   public static final Holder.Reference<OutgoingRpcMethod<Void, Void>> SERVER_STARTED = OutgoingRpcMethod.notification()
      .description("Server started")
      .register("server/started");
   public static final Holder.Reference<OutgoingRpcMethod<Void, Void>> SERVER_SHUTTING_DOWN = OutgoingRpcMethod.notification()
      .description("Server shutting down")
      .register("server/stopping");
   public static final Holder.Reference<OutgoingRpcMethod<Void, Void>> SERVER_SAVE_STARTED = OutgoingRpcMethod.notification()
      .description("Server save started")
      .register("server/saving");
   public static final Holder.Reference<OutgoingRpcMethod<Void, Void>> SERVER_SAVE_COMPLETED = OutgoingRpcMethod.notification()
      .description("Server save completed")
      .register("server/saved");
   public static final Holder.Reference<OutgoingRpcMethod<Void, Void>> SERVER_ACTIVITY_OCCURRED = OutgoingRpcMethod.notification()
      .description("Server activity occurred. Rate limited to 1 notification per 30 seconds")
      .register("server/activity");
   public static final Holder.Reference<OutgoingRpcMethod<PlayerDto, Void>> PLAYER_JOINED = OutgoingRpcMethod.<PlayerDto>notificationWithParams()
      .param("player", Schema.PLAYER_SCHEMA.asRef())
      .description("Player joined")
      .register("players/joined");
   public static final Holder.Reference<OutgoingRpcMethod<PlayerDto, Void>> PLAYER_LEFT = OutgoingRpcMethod.<PlayerDto>notificationWithParams()
      .param("player", Schema.PLAYER_SCHEMA.asRef())
      .description("Player left")
      .register("players/left");
   public static final Holder.Reference<OutgoingRpcMethod<OperatorService.OperatorDto, Void>> PLAYER_OPED = OutgoingRpcMethod.<OperatorService.OperatorDto>notificationWithParams()
      .param("player", Schema.OPERATOR_SCHEMA.asRef())
      .description("Player was oped")
      .register("operators/added");
   public static final Holder.Reference<OutgoingRpcMethod<OperatorService.OperatorDto, Void>> PLAYER_DEOPED = OutgoingRpcMethod.<OperatorService.OperatorDto>notificationWithParams()
      .param("player", Schema.OPERATOR_SCHEMA.asRef())
      .description("Player was deoped")
      .register("operators/removed");
   public static final Holder.Reference<OutgoingRpcMethod<PlayerDto, Void>> PLAYER_ADDED_TO_ALLOWLIST = OutgoingRpcMethod.<PlayerDto>notificationWithParams()
      .param("player", Schema.PLAYER_SCHEMA.asRef())
      .description("Player was added to allowlist")
      .register("allowlist/added");
   public static final Holder.Reference<OutgoingRpcMethod<PlayerDto, Void>> PLAYER_REMOVED_FROM_ALLOWLIST = OutgoingRpcMethod.<PlayerDto>notificationWithParams()
      .param("player", Schema.PLAYER_SCHEMA.asRef())
      .description("Player was removed from allowlist")
      .register("allowlist/removed");
   public static final Holder.Reference<OutgoingRpcMethod<IpBanlistService.IpBanDto, Void>> IP_BANNED = OutgoingRpcMethod.<IpBanlistService.IpBanDto>notificationWithParams()
      .param("player", Schema.IP_BAN_SCHEMA.asRef())
      .description("Ip was added to ip ban list")
      .register("ip_bans/added");
   public static final Holder.Reference<OutgoingRpcMethod<String, Void>> IP_UNBANNED = OutgoingRpcMethod.<String>notificationWithParams()
      .param("player", Schema.STRING_SCHEMA)
      .description("Ip was removed from ip ban list")
      .register("ip_bans/removed");
   public static final Holder.Reference<OutgoingRpcMethod<BanlistService.UserBanDto, Void>> PLAYER_BANNED = OutgoingRpcMethod.<BanlistService.UserBanDto>notificationWithParams()
      .param("player", Schema.PLAYER_BAN_SCHEMA.asRef())
      .description("Player was added to ban list")
      .register("bans/added");
   public static final Holder.Reference<OutgoingRpcMethod<PlayerDto, Void>> PLAYER_UNBANNED = OutgoingRpcMethod.<PlayerDto>notificationWithParams()
      .param("player", Schema.PLAYER_SCHEMA.asRef())
      .description("Player was removed from ban list")
      .register("bans/removed");
   public static final Holder.Reference<OutgoingRpcMethod<GameRulesService.GameRuleUpdate<?>, Void>> GAMERULE_CHANGED = OutgoingRpcMethod.<GameRulesService.GameRuleUpdate<?>>notificationWithParams()
      .param("gamerule", Schema.TYPED_GAME_RULE_SCHEMA.asRef())
      .description("Gamerule was changed")
      .register("gamerules/updated");
   public static final Holder.Reference<OutgoingRpcMethod<ServerStateService.ServerState, Void>> STATUS_HEARTBEAT = OutgoingRpcMethod.<ServerStateService.ServerState>notificationWithParams()
      .param("status", Schema.SERVER_STATE_SCHEMA.asRef())
      .description("Server status heartbeat")
      .register("server/status");
}
