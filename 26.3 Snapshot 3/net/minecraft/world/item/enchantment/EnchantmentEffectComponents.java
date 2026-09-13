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
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.enchantment.effects.DamageImmunity;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public interface EnchantmentEffectComponents {
   Codec<DataComponentType<?>> COMPONENT_CODEC = Codec.lazyInitialized(() -> BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.byNameCodec());
   Codec<DataComponentMap> CODEC = DataComponentMap.makeCodec(COMPONENT_CODEC);
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> DAMAGE_PROTECTION = register(
      "damage_protection", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
   );
   DataComponentType<List<ConditionalEffect<DamageImmunity>>> DAMAGE_IMMUNITY = register(
      "damage_immunity", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(DamageImmunity.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> DAMAGE = register(
      "damage", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> SMASH_DAMAGE_PER_FALLEN_BLOCK = register(
      "smash_damage_per_fallen_block",
      b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> KNOCKBACK = register(
      "knockback", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ARMOR_EFFECTIVENESS = register(
      "armor_effectiveness",
      b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
   );
   DataComponentType<List<TargetedConditionalEffect<EnchantmentEntityEffect>>> POST_ATTACK = register(
      "post_attack",
      b -> b.persistent(validatedListCodec(TargetedConditionalEffect.codec(EnchantmentEntityEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> POST_PIERCING_ATTACK = register(
      "post_piercing_attack",
      b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> HIT_BLOCK = register(
      "hit_block", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC), LootContextParamSets.HIT_BLOCK))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ITEM_DAMAGE = register(
      "item_damage", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ITEM))
   );
   DataComponentType<List<TargetedConditionalEffect<EnchantmentValueEffect>>> EQUIPMENT_DROPS = register(
      "equipment_drops",
      b -> b.persistent(validatedListCodec(TargetedConditionalEffect.equipmentDropsCodec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentLocationBasedEffect>>> LOCATION_CHANGED = register(
      "location_changed",
      b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentLocationBasedEffect.CODEC), LootContextParamSets.ENCHANTED_LOCATION))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> TICK = register(
      "tick", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC), LootContextParamSets.ENCHANTED_ENTITY))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> AMMO_USE = register(
      "ammo_use", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ITEM))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_PIERCING = register(
      "projectile_piercing", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ITEM))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> PROJECTILE_SPAWNED = register(
      "projectile_spawned",
      b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC), LootContextParamSets.ENCHANTED_ENTITY))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_SPREAD = register(
      "projectile_spread", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ENTITY))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_COUNT = register(
      "projectile_count", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ENTITY))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> TRIDENT_RETURN_ACCELERATION = register(
      "trident_return_acceleration",
      b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ENTITY))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> FISHING_TIME_REDUCTION = register(
      "fishing_time_reduction",
      b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ENTITY))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> FISHING_LUCK_BONUS = register(
      "fishing_luck_bonus", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ENTITY))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> BLOCK_EXPERIENCE = register(
      "block_experience", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ITEM))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> MOB_EXPERIENCE = register(
      "mob_experience", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ENTITY))
   );
   DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> REPAIR_WITH_XP = register(
      "repair_with_xp", b -> b.persistent(validatedListCodec(ConditionalEffect.codec(EnchantmentValueEffect.CODEC), LootContextParamSets.ENCHANTED_ITEM))
   );
   DataComponentType<List<EnchantmentAttributeEffect>> ATTRIBUTES = register(
      "attributes", b -> b.persistent(EnchantmentAttributeEffect.MAP_CODEC.codec().listOf())
   );
   DataComponentType<EnchantmentValueEffect> CROSSBOW_CHARGE_TIME = register("crossbow_charge_time", b -> b.persistent(EnchantmentValueEffect.CODEC));
   DataComponentType<List<CrossbowItem.ChargingSounds>> CROSSBOW_CHARGING_SOUNDS = register(
      "crossbow_charging_sounds", b -> b.persistent(CrossbowItem.ChargingSounds.CODEC.listOf())
   );
   DataComponentType<List<Holder<SoundEvent>>> TRIDENT_SOUND = register("trident_sound", b -> b.persistent(SoundEvent.CODEC.listOf()));
   DataComponentType<Unit> PREVENT_EQUIPMENT_DROP = register("prevent_equipment_drop", b -> b.persistent(Unit.CODEC));
   DataComponentType<Unit> PREVENT_ARMOR_CHANGE = register("prevent_armor_change", b -> b.persistent(Unit.CODEC));
   DataComponentType<EnchantmentValueEffect> TRIDENT_SPIN_ATTACK_STRENGTH = register(
      "trident_spin_attack_strength", b -> b.persistent(EnchantmentValueEffect.CODEC)
   );

   private static <T extends Validatable> Codec<List<T>> validatedListCodec(final Codec<T> elementCodec, final ContextKeySet paramSet) {
      return elementCodec.listOf().validate(Validatable.listValidatorForContext(paramSet));
   }

   static DataComponentType<?> bootstrap(final Registry<DataComponentType<?>> registry) {
      return DAMAGE_PROTECTION;
   }

   private static <T> DataComponentType<T> register(final String id, final UnaryOperator<DataComponentType.Builder<T>> builder) {
      return Registry.register(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, id, builder.apply(DataComponentType.builder()).build());
   }
}
