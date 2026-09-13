package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;

public class GameRulesService {
   public static List<GameRulesService.GameRuleUpdate<?>> get(MinecraftApi p_426026_) {
      List<GameRulesService.GameRuleUpdate<?>> list = new ArrayList<>();
      p_426026_.gameRuleService().getAvailableGameRules().forEach(p_460432_ -> addGameRule(p_426026_, (GameRule<?>)p_460432_, list));
      return list;
   }

   private static <T> void addGameRule(MinecraftApi p_460649_, GameRule<T> p_454403_, List<GameRulesService.GameRuleUpdate<?>> p_452140_) {
      T t = p_460649_.gameRuleService().getRuleValue(p_454403_);
      p_452140_.add(getTypedRule(p_460649_, p_454403_, Objects.requireNonNull(t)));
   }

   public static <T> GameRulesService.GameRuleUpdate<T> getTypedRule(MinecraftApi p_423755_, GameRule<T> p_460918_, T p_452193_) {
      return p_423755_.gameRuleService().getTypedRule(p_460918_, p_452193_);
   }

   public static <T> GameRulesService.GameRuleUpdate<T> update(MinecraftApi p_426148_, GameRulesService.GameRuleUpdate<T> p_456062_, ClientInfo p_430040_) {
      return p_426148_.gameRuleService().updateGameRule(p_456062_, p_430040_);
   }

   public record GameRuleUpdate<T>(GameRule<T> gameRule, T value) {
      public static final Codec<GameRulesService.GameRuleUpdate<?>> TYPED_CODEC = BuiltInRegistries.GAME_RULE
         .byNameCodec()
         .dispatch("key", GameRulesService.GameRuleUpdate::gameRule, GameRulesService.GameRuleUpdate::getValueAndTypeCodec);
      public static final Codec<GameRulesService.GameRuleUpdate<?>> CODEC = BuiltInRegistries.GAME_RULE
         .byNameCodec()
         .dispatch("key", GameRulesService.GameRuleUpdate::gameRule, GameRulesService.GameRuleUpdate::getValueCodec);

      private static <T> MapCodec<? extends GameRulesService.GameRuleUpdate<T>> getValueCodec(GameRule<T> p_450451_) {
         return p_450451_.valueCodec()
            .fieldOf("value")
            .xmap(p_452476_ -> new GameRulesService.GameRuleUpdate<>(p_450451_, (T)p_452476_), GameRulesService.GameRuleUpdate::value);
      }

      private static <T> MapCodec<? extends GameRulesService.GameRuleUpdate<T>> getValueAndTypeCodec(GameRule<T> p_458167_) {
         return RecordCodecBuilder.mapCodec(
            p_453181_ -> p_453181_.group(
                  StringRepresentable.fromEnum(GameRuleType::values).fieldOf("type").forGetter(p_455312_ -> p_455312_.gameRule.gameRuleType()),
                  p_458167_.valueCodec().fieldOf("value").forGetter(GameRulesService.GameRuleUpdate::value)
               )
               .apply(p_453181_, (p_452449_, p_453928_) -> getUntypedRule(p_458167_, p_452449_, (T)p_453928_))
         );
      }

      private static <T> GameRulesService.GameRuleUpdate<T> getUntypedRule(GameRule<T> p_456801_, GameRuleType p_457354_, T p_455577_) {
         if (p_456801_.gameRuleType() != p_457354_) {
            throw new InvalidParameterJsonRpcException(
               "Stated type \"" + p_457354_ + "\" mismatches with actual type \"" + p_456801_.gameRuleType() + "\" of gamerule \"" + p_456801_.id() + "\""
            );
         } else {
            return new GameRulesService.GameRuleUpdate<>(p_456801_, p_455577_);
         }
      }
   }
}
