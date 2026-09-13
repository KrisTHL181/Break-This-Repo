package net.minecraft.server.jsonrpc.internalapi;

import java.util.stream.Stream;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.world.level.gamerules.GameRule;

public interface MinecraftGameRuleService {
   <T> GameRulesService.GameRuleUpdate<T> updateGameRule(GameRulesService.GameRuleUpdate<T> var1, ClientInfo var2);

   <T> T getRuleValue(GameRule<T> var1);

   <T> GameRulesService.GameRuleUpdate<T> getTypedRule(GameRule<T> var1, T var2);

   Stream<GameRule<?>> getAvailableGameRules();
}
