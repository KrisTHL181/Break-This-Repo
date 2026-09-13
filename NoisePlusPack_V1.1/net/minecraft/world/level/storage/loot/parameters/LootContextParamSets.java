package net.minecraft.world.level.storage.loot.parameters;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;

public class LootContextParamSets {
   private static final BiMap<Identifier, ContextKeySet> REGISTRY = HashBiMap.create();
   public static final Codec<ContextKeySet> CODEC = Identifier.CODEC
      .comapFlatMap(
         p_457152_ -> Optional.ofNullable((ContextKeySet)REGISTRY.get(p_457152_))
            .<DataResult>map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "No parameter set exists with id: '" + p_457152_ + "'")),
         REGISTRY.inverse()::get
      );
   public static final ContextKeySet EMPTY = register("empty", p_367081_ -> {});
   public static final ContextKeySet CHEST = register(
      "chest", p_360697_ -> p_360697_.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY)
   );
   public static final ContextKeySet COMMAND = register(
      "command", p_360688_ -> p_360688_.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY)
   );
   public static final ContextKeySet SELECTOR = register(
      "selector", p_360687_ -> p_360687_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY)
   );
   public static final ContextKeySet FISHING = register(
      "fishing", p_360682_ -> p_360682_.required(LootContextParams.ORIGIN).required(LootContextParams.TOOL).optional(LootContextParams.THIS_ENTITY)
   );
   public static final ContextKeySet ENTITY = register(
      "entity",
      p_360695_ -> p_360695_.required(LootContextParams.THIS_ENTITY)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.DAMAGE_SOURCE)
         .optional(LootContextParams.ATTACKING_ENTITY)
         .optional(LootContextParams.DIRECT_ATTACKING_ENTITY)
         .optional(LootContextParams.LAST_DAMAGE_PLAYER)
   );
   public static final ContextKeySet EQUIPMENT = register(
      "equipment", p_360692_ -> p_360692_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY)
   );
   public static final ContextKeySet ARCHAEOLOGY = register(
      "archaeology", p_360683_ -> p_360683_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY).required(LootContextParams.TOOL)
   );
   public static final ContextKeySet GIFT = register("gift", p_360689_ -> p_360689_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY));
   public static final ContextKeySet PIGLIN_BARTER = register("barter", p_360698_ -> p_360698_.required(LootContextParams.THIS_ENTITY));
   public static final ContextKeySet VAULT = register(
      "vault", p_360686_ -> p_360686_.required(LootContextParams.ORIGIN).optional(LootContextParams.THIS_ENTITY).optional(LootContextParams.TOOL)
   );
   public static final ContextKeySet ADVANCEMENT_REWARD = register(
      "advancement_reward", p_360684_ -> p_360684_.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN)
   );
   public static final ContextKeySet ADVANCEMENT_ENTITY = register(
      "advancement_entity", p_360680_ -> p_360680_.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN)
   );
   public static final ContextKeySet ADVANCEMENT_LOCATION = register(
      "advancement_location",
      p_360699_ -> p_360699_.required(LootContextParams.THIS_ENTITY)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.TOOL)
         .required(LootContextParams.BLOCK_STATE)
   );
   public static final ContextKeySet BLOCK_USE = register(
      "block_use", p_360679_ -> p_360679_.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ORIGIN).required(LootContextParams.BLOCK_STATE)
   );
   public static final ContextKeySet ALL_PARAMS = register(
      "generic",
      p_360694_ -> p_360694_.required(LootContextParams.THIS_ENTITY)
         .required(LootContextParams.LAST_DAMAGE_PLAYER)
         .required(LootContextParams.DAMAGE_SOURCE)
         .required(LootContextParams.ATTACKING_ENTITY)
         .required(LootContextParams.DIRECT_ATTACKING_ENTITY)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.BLOCK_STATE)
         .required(LootContextParams.BLOCK_ENTITY)
         .required(LootContextParams.TOOL)
         .required(LootContextParams.EXPLOSION_RADIUS)
   );
   public static final ContextKeySet BLOCK = register(
      "block",
      p_360696_ -> p_360696_.required(LootContextParams.BLOCK_STATE)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.TOOL)
         .optional(LootContextParams.THIS_ENTITY)
         .optional(LootContextParams.BLOCK_ENTITY)
         .optional(LootContextParams.EXPLOSION_RADIUS)
   );
   public static final ContextKeySet SHEARING = register(
      "shearing", p_360691_ -> p_360691_.required(LootContextParams.ORIGIN).required(LootContextParams.THIS_ENTITY).required(LootContextParams.TOOL)
   );
   public static final ContextKeySet ENTITY_INTERACT = register(
      "entity_interact",
      p_422274_ -> p_422274_.required(LootContextParams.TARGET_ENTITY).optional(LootContextParams.INTERACTING_ENTITY).required(LootContextParams.TOOL)
   );
   public static final ContextKeySet BLOCK_INTERACT = register(
      "block_interact",
      p_422273_ -> p_422273_.required(LootContextParams.BLOCK_STATE)
         .optional(LootContextParams.BLOCK_ENTITY)
         .optional(LootContextParams.INTERACTING_ENTITY)
         .optional(LootContextParams.TOOL)
   );
   public static final ContextKeySet ENCHANTED_DAMAGE = register(
      "enchanted_damage",
      p_360677_ -> p_360677_.required(LootContextParams.THIS_ENTITY)
         .required(LootContextParams.ENCHANTMENT_LEVEL)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.DAMAGE_SOURCE)
         .optional(LootContextParams.DIRECT_ATTACKING_ENTITY)
         .optional(LootContextParams.ATTACKING_ENTITY)
   );
   public static final ContextKeySet ENCHANTED_ITEM = register(
      "enchanted_item", p_360678_ -> p_360678_.required(LootContextParams.TOOL).required(LootContextParams.ENCHANTMENT_LEVEL)
   );
   public static final ContextKeySet ENCHANTED_LOCATION = register(
      "enchanted_location",
      p_360693_ -> p_360693_.required(LootContextParams.THIS_ENTITY)
         .required(LootContextParams.ENCHANTMENT_LEVEL)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.ENCHANTMENT_ACTIVE)
   );
   public static final ContextKeySet ENCHANTED_ENTITY = register(
      "enchanted_entity",
      p_360690_ -> p_360690_.required(LootContextParams.THIS_ENTITY).required(LootContextParams.ENCHANTMENT_LEVEL).required(LootContextParams.ORIGIN)
   );
   public static final ContextKeySet HIT_BLOCK = register(
      "hit_block",
      p_360685_ -> p_360685_.required(LootContextParams.THIS_ENTITY)
         .required(LootContextParams.ENCHANTMENT_LEVEL)
         .required(LootContextParams.ORIGIN)
         .required(LootContextParams.BLOCK_STATE)
   );

   private static ContextKeySet register(String p_81429_, Consumer<ContextKeySet.Builder> p_81430_) {
      ContextKeySet.Builder contextkeyset$builder = new ContextKeySet.Builder();
      p_81430_.accept(contextkeyset$builder);
      ContextKeySet contextkeyset = contextkeyset$builder.build();
      Identifier identifier = Identifier.withDefaultNamespace(p_81429_);
      ContextKeySet contextkeyset1 = (ContextKeySet)REGISTRY.put(identifier, contextkeyset);
      if (contextkeyset1 != null) {
         throw new IllegalStateException("Loot table parameter set " + identifier + " is already registered");
      } else {
         return contextkeyset;
      }
   }
}
