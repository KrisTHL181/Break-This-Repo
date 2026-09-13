package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class LootItemConditionTypes {
   public static MapCodec<? extends LootItemCondition> bootstrap(final Registry<MapCodec<? extends LootItemCondition>> registry) {
      Registry.register(registry, "inverted", InvertedLootItemCondition.MAP_CODEC);
      Registry.register(registry, "any_of", AnyOfCondition.MAP_CODEC);
      Registry.register(registry, "all_of", AllOfCondition.MAP_CODEC);
      Registry.register(registry, "random_chance", LootItemRandomChanceCondition.MAP_CODEC);
      Registry.register(registry, "random_chance_with_enchanted_bonus", LootItemRandomChanceWithEnchantedBonusCondition.MAP_CODEC);
      Registry.register(registry, "entity_properties", LootItemEntityPropertyCondition.MAP_CODEC);
      Registry.register(registry, "killed_by_player", LootItemKilledByPlayerCondition.MAP_CODEC);
      Registry.register(registry, "entity_scores", EntityHasScoreCondition.MAP_CODEC);
      Registry.register(registry, "block_state_property", LootItemBlockStatePropertyCondition.MAP_CODEC);
      Registry.register(registry, "match_tool", MatchTool.MAP_CODEC);
      Registry.register(registry, "table_bonus", BonusLevelTableCondition.MAP_CODEC);
      Registry.register(registry, "survives_explosion", ExplosionCondition.MAP_CODEC);
      Registry.register(registry, "damage_source_properties", DamageSourceCondition.MAP_CODEC);
      Registry.register(registry, "location_check", LocationCheck.MAP_CODEC);
      Registry.register(registry, "weather_check", WeatherCheck.MAP_CODEC);
      Registry.register(registry, "reference", ConditionReference.MAP_CODEC);
      Registry.register(registry, "time_check", TimeCheck.MAP_CODEC);
      Registry.register(registry, "value_check", ValueCheckCondition.MAP_CODEC);
      Registry.register(registry, "enchantment_active_check", EnchantmentActiveCheck.MAP_CODEC);
      return Registry.register(registry, "environment_attribute_check", EnvironmentAttributeCheck.MAP_CODEC);
   }
}
