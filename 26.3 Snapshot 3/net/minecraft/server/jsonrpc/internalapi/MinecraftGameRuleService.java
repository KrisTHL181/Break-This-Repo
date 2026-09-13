package net.minecraft.server.jsonrpc.internalapi;

import java.util.stream.Stream;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.world.level.gamerules.GameRule;

public interface MinecraftGameRuleService {
   <T> GameRulesService.GameRuleUpdate<T> updateGameRule(GameRulesService.GameRuleUpdate<T> update, ClientInfo clientInfo);

   <T> T getRuleValue(GameRule<T> gameRule);

   <T> GameRulesService.GameRuleUpdate<T> getTypedRule(GameRule<T> gameRule, T value);

   Stream<GameRule<?>> getAvailableGameRules();
}
