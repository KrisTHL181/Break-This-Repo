package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.enchantment.effects.DamageImmunity;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public interface EnchantmentEffectComponents {
   Codec<DataComponentType<?>> COMPONENT_CODEC = Codec.lazyInitialized(() -> BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.byNameCodec());
   Codec<DataComponentMap> CODEC = DataComponentMap.makeCodec(COMPONENT_CODEC);
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> DAMAGE_PROTECTION = register(
      "damage_protection",
      p_343083_ -> p_343083_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
   );
   DataComponentType<List<ConditionalEffect<DamageImmunity>>> DAMAGE_IMMUNITY = register(
      "damage_immunity", p_342369_ -> p_342369_.persistent(ConditionalEffect.codec(DamageImmunity.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> DAMAGE = register(
      "damage", p_343665_ -> p_343665_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> SMASH_DAMAGE_PER_FALLEN_BLOCK = register(
      "smash_damage_per_fallen_block",
      p_342204_ -> p_342204_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> KNOCKBACK = register(
      "knockback", p_342778_ -> p_342778_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ARMOR_EFFECTIVENESS = register(
      "armor_effectiveness",
      p_342687_ -> p_342687_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
   );
   DataComponentType<List<TargetedConditionalEffect<EnchantmentEntityEffect>>> POST_ATTACK = register(
      "post_attack",
      p_344691_ -> p_344691_.persistent(TargetedConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> POST_PIERCING_ATTACK = register(
      "post_piercing_attack",
      p_449850_ -> p_449850_.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> HIT_BLOCK = register(
      "hit_block", p_343726_ -> p_343726_.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.HIT_BLOCK).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ITEM_DAMAGE = register(
      "item_damage", p_343745_ -> p_343745_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
   );
   DataComponentType<List<EnchantmentAttributeEffect>> ATTRIBUTES = register(
      "attributes", p_342151_ -> p_342151_.persistent(EnchantmentAttributeEffect.CODEC.codec().listOf())
   );
   DataComponentType<List<TargetedConditionalEffect<EnchantmentValueEffect>>> EQUIPMENT_DROPS = register(
      "equipment_drops",
      p_342441_ -> p_342441_.persistent(
         TargetedConditionalEffect.equipmentDropsCodec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()
      )
   );
   DataComponentType<List<ConditionalEffect<EnchantmentLocationBasedEffect>>> LOCATION_CHANGED = register(
      "location_changed",
      p_344782_ -> p_344782_.persistent(ConditionalEffect.codec(EnchantmentLocationBasedEffect.CODEC, LootContextParamSets.ENCHANTED_LOCATION).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> TICK = register(
      "tick", p_345201_ -> p_345201_.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> AMMO_USE = register(
      "ammo_use", p_344537_ -> p_344537_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_PIERCING = register(
      "projectile_piercing",
      p_344214_ -> p_344214_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> PROJECTILE_SPAWNED = register(
      "projectile_spawned",
      p_345464_ -> p_345464_.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_SPREAD = register(
      "projectile_spread",
      p_342569_ -> p_342569_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_COUNT = register(
      "projectile_count",
      p_344670_ -> p_344670_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> TRIDENT_RETURN_ACCELERATION = register(
      "trident_return_acceleration",
      p_342620_ -> p_342620_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> FISHING_TIME_REDUCTION = register(
      "fishing_time_reduction",
      p_342994_ -> p_342994_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> FISHING_LUCK_BONUS = register(
      "fishing_luck_bonus",
      p_345348_ -> p_345348_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> BLOCK_EXPERIENCE = register(
      "block_experience",
      p_344261_ -> p_344261_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> MOB_EXPERIENCE = register(
      "mob_experience",
      p_344594_ -> p_344594_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> REPAIR_WITH_XP = register(
      "repair_with_xp", p_344724_ -> p_344724_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
   );
   DataComponentType<EnchantmentValueEffect> CROSSBOW_CHARGE_TIME = register(
      "crossbow_charge_time", p_344938_ -> p_344938_.persistent(EnchantmentValueEffect.CODEC)
   );
   DataComponentType<List<CrossbowItem.ChargingSounds>> CROSSBOW_CHARGING_SOUNDS = register(
      "crossbow_charging_sounds", p_344355_ -> p_344355_.persistent(CrossbowItem.ChargingSounds.CODEC.listOf())
   );
   DataComponentType<List<Holder<SoundEvent>>> TRIDENT_SOUND = register("trident_sound", p_345273_ -> p_345273_.persistent(SoundEvent.CODEC.listOf()));
   DataComponentType<Unit> PREVENT_EQUIPMENT_DROP = register("prevent_equipment_drop", p_345068_ -> p_345068_.persistent(Unit.CODEC));
   DataComponentType<Unit> PREVENT_ARMOR_CHANGE = register("prevent_armor_change", p_344955_ -> p_344955_.persistent(Unit.CODEC));
   DataComponentType<EnchantmentValueEffect> TRIDENT_SPIN_ATTACK_STRENGTH = register(
      "trident_spin_attack_strength", p_343362_ -> p_343362_.persistent(EnchantmentValueEffect.CODEC)
   );

   static DataComponentType<?> bootstrap(Registry<DataComponentType<?>> p_342462_) {
      return DAMAGE_PROTECTION;
   }

   private static <T> DataComponentType<T> register(String p_342959_, UnaryOperator<DataComponentType.Builder<T>> p_345175_) {
      return Registry.register(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, p_342959_, p_345175_.apply(DataComponentType.builder()).build());
   }
}
