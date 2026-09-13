package net.minecraft.server.jsonrpc.internalapi;

import java.util.stream.Stream;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

public class MinecraftGameRuleServiceImpl implements MinecraftGameRuleService {
   private final DedicatedServer server;
   private final GameRules gameRules;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftGameRuleServiceImpl(DedicatedServer p_422375_, JsonRpcLogger p_428405_) {
      this.server = p_422375_;
      this.gameRules = p_422375_.getWorldData().getGameRules();
      this.jsonrpcLogger = p_428405_;
   }

   @Override
   public <T> GameRulesService.GameRuleUpdate<T> updateGameRule(GameRulesService.GameRuleUpdate<T> p_455076_, ClientInfo p_425938_) {
      GameRule<T> gamerule = p_455076_.gameRule();
      T t = this.gameRules.get(gamerule);
      T t1 = p_455076_.value();
      this.gameRules.set(gamerule, t1, this.server);
      this.jsonrpcLogger.log(p_425938_, "Game rule '{}' updated from '{}' to '{}'", gamerule.id(), gamerule.serialize(t), gamerule.serialize(t1));
      return p_455076_;
   }

   @Override
   public <T> GameRulesService.GameRuleUpdate<T> getTypedRule(GameRule<T> p_454896_, T p_459015_) {
      return new GameRulesService.GameRuleUpdate<>(p_454896_, p_459015_);
   }

   @Override
   public Stream<GameRule<?>> getAvailableGameRules() {
      return this.gameRules.availableRules();
   }

   @Override
   public <T> T getRuleValue(GameRule<T> p_458011_) {
      return this.gameRules.get(p_458011_);
   }
}
