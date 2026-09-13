package net.minecraft.world.item.enchantment;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.effects.DamageImmunity;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableFloat;

public record Enchantment(Component description, Enchantment.EnchantmentDefinition definition, HolderSet<Enchantment> exclusiveSet, DataComponentMap effects) {
   public static final int MAX_LEVEL = 255;
   public static final Codec<Enchantment> DIRECT_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ComponentSerialization.CODEC.fieldOf("description").forGetter(Enchantment::description),
            Enchantment.EnchantmentDefinition.CODEC.forGetter(Enchantment::definition),
            RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("exclusive_set", HolderSet.empty()).forGetter(Enchantment::exclusiveSet),
            EnchantmentEffectComponents.CODEC.optionalFieldOf("effects", DataComponentMap.EMPTY).forGetter(Enchantment::effects)
         )
         .apply(i, Enchantment::new)
   );
   public static final Codec<Holder<Enchantment>> CODEC = RegistryFixedCodec.create(Registries.ENCHANTMENT);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Enchantment>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT);

   public static Enchantment.Cost constantCost(final int base) {
      return new Enchantment.Cost(base, 0);
   }

   public static Enchantment.Cost dynamicCost(final int base, final int perLevel) {
      return new Enchantment.Cost(base, perLevel);
   }

   public static Enchantment.EnchantmentDefinition definition(
      final HolderSet<Item> supportedItems,
      final HolderSet<Item> primaryItems,
      final int weight,
      final int maxLevel,
      final Enchantment.Cost minCost,
      final Enchantment.Cost maxCost,
      final int anvilCost,
      final EquipmentSlotGroup... slots
   ) {
      return new Enchantment.EnchantmentDefinition(supportedItems, Optional.of(primaryItems), weight, maxLevel, minCost, maxCost, anvilCost, List.of(slots));
   }

   public static Enchantment.EnchantmentDefinition definition(
      final HolderSet<Item> supportedItems,
      final int weight,
      final int maxLevel,
      final Enchantment.Cost minCost,
      final Enchantment.Cost maxCost,
      final int anvilCost,
      final EquipmentSlotGroup... slots
   ) {
      return new Enchantment.EnchantmentDefinition(supportedItems, Optional.empty(), weight, maxLevel, minCost, maxCost, anvilCost, List.of(slots));
   }

   public Map<EquipmentSlot, ItemStack> getSlotItems(final LivingEntity entity) {
      Map<EquipmentSlot, ItemStack> itemStacks = Maps.newEnumMap(EquipmentSlot.class);

      for (EquipmentSlot slot : EquipmentSlot.VALUES) {
         if (this.matchingSlot(slot)) {
            ItemStack itemStack = entity.getItemBySlot(slot);
            if (!itemStack.isEmpty()) {
               itemStacks.put(slot, itemStack);
            }
         }
      }

      return itemStacks;
   }

   public HolderSet<Item> getSupportedItems() {
      return this.definition.supportedItems();
   }

   public boolean matchingSlot(final EquipmentSlot slot) {
      return this.definition.slots().stream().anyMatch(group -> group.test(slot));
   }

   public boolean isPrimaryItem(final ItemStack item) {
      return this.isSupportedItem(item) && (this.definition.primaryItems.isEmpty() || item.is(this.definition.primaryItems.get()));
   }

   public boolean isSupportedItem(final ItemStack item) {
      return item.is(this.definition.supportedItems);
   }

   public int getWeight() {
      return this.definition.weight();
   }

   public int getAnvilCost() {
      return this.definition.anvilCost();
   }

   public int getMinLevel() {
      return 1;
   }

   public int getMaxLevel() {
      return this.definition.maxLevel();
   }

   public int getMinCost(final int level) {
      return this.definition.minCost().calculate(level);
   }

   public int getMaxCost(final int level) {
      return this.definition.maxCost().calculate(level);
   }

   @Override
   public String toString() {
      return "Enchantment " + this.description.getString();
   }

   public static boolean areCompatible(final Holder<Enchantment> enchantment, final Holder<Enchantment> other) {
      return !enchantment.equals(other) && !enchantment.value().exclusiveSet.contains(other) && !other.value().exclusiveSet.contains(enchantment);
   }

   public static Component getFullname(final Holder<Enchantment> enchantment, final int level) {
      MutableComponent result = enchantment.value().description.copy();
      if (enchantment.is(EnchantmentTags.CURSE)) {
         result = ComponentUtils.mergeStyles(result, Style.EMPTY.withColor(ChatFormatting.RED));
      } else {
         result = ComponentUtils.mergeStyles(result, Style.EMPTY.withColor(ChatFormatting.GRAY));
      }

      if (level != 1 || enchantment.value().getMaxLevel() != 1) {
         result.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level));
      }

      return result;
   }

   public boolean canEnchant(final ItemStack itemStack) {
      return this.definition.supportedItems().contains(itemStack.typeHolder());
   }

   public <T> List<T> getEffects(final DataComponentType<List<T>> type) {
      return this.effects.getOrDefault(type, List.of());
   }

   public boolean isImmuneToDamage(final ServerLevel serverLevel, final int enchantmentLevel, final Entity victim, final DamageSource source) {
      LootContext context = damageContext(serverLevel, enchantmentLevel, victim, source);

      for (ConditionalEffect<DamageImmunity> filteredEffect : this.getEffects(EnchantmentEffectComponents.DAMAGE_IMMUNITY)) {
         if (filteredEffect.matches(context)) {
            return true;
         }
      }

      return false;
   }

   public void modifyDamageProtection(
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final ItemStack item,
      final Entity victim,
      final DamageSource source,
      final MutableFloat protection
   ) {
      applyEffects(
         this.getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION),
         damageContext(serverLevel, enchantmentLevel, victim, source),
         protection,
         (e, v) -> e.process(enchantmentLevel, victim.getRandom(), v)
      );
   }

   public void modifyDurabilityChange(final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack itemStack, final MutableFloat change) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.ITEM_DAMAGE, serverLevel, enchantmentLevel, itemStack, change);
   }

   public void modifyAmmoCount(final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack itemStack, final MutableFloat change) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.AMMO_USE, serverLevel, enchantmentLevel, itemStack, change);
   }

   public void modifyPiercingCount(final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack itemStack, final MutableFloat count) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.PROJECTILE_PIERCING, serverLevel, enchantmentLevel, itemStack, count);
   }

   public void modifyBlockExperience(final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack itemStack, final MutableFloat count) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.BLOCK_EXPERIENCE, serverLevel, enchantmentLevel, itemStack, count);
   }

   public void modifyMobExperience(
      final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack itemStack, final Entity killer, final MutableFloat experience
   ) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.MOB_EXPERIENCE, serverLevel, enchantmentLevel, itemStack, killer, experience);
   }

   public void modifyDurabilityToRepairFromXp(final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack itemStack, final MutableFloat change) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.REPAIR_WITH_XP, serverLevel, enchantmentLevel, itemStack, change);
   }

   public void modifyTridentReturnToOwnerAcceleration(
      final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack itemStack, final Entity trident, final MutableFloat count
   ) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.TRIDENT_RETURN_ACCELERATION, serverLevel, enchantmentLevel, itemStack, trident, count);
   }

   public void modifyTridentSpinAttackStrength(final RandomSource random, final int enchantmentLevel, final MutableFloat strength) {
      this.modifyUnfilteredValue(EnchantmentEffectComponents.TRIDENT_SPIN_ATTACK_STRENGTH, random, enchantmentLevel, strength);
   }

   public void modifyFishingTimeReduction(
      final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack itemStack, final Entity fisher, final MutableFloat timeReduction
   ) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.FISHING_TIME_REDUCTION, serverLevel, enchantmentLevel, itemStack, fisher, timeReduction);
   }

   public void modifyFishingLuckBonus(
      final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack itemStack, final Entity fisher, final MutableFloat luck
   ) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.FISHING_LUCK_BONUS, serverLevel, enchantmentLevel, itemStack, fisher, luck);
   }

   public void modifyDamage(
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final ItemStack itemStack,
      final Entity victim,
      final DamageSource damageSource,
      final MutableFloat amount
   ) {
      this.modifyDamageFilteredValue(EnchantmentEffectComponents.DAMAGE, serverLevel, enchantmentLevel, itemStack, victim, damageSource, amount);
   }

   public void modifyFallBasedDamage(
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final ItemStack itemStack,
      final Entity victim,
      final DamageSource damageSource,
      final MutableFloat amount
   ) {
      this.modifyDamageFilteredValue(
         EnchantmentEffectComponents.SMASH_DAMAGE_PER_FALLEN_BLOCK, serverLevel, enchantmentLevel, itemStack, victim, damageSource, amount
      );
   }

   public void modifyKnockback(
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final ItemStack itemStack,
      final Entity victim,
      final DamageSource damageSource,
      final MutableFloat amount
   ) {
      this.modifyDamageFilteredValue(EnchantmentEffectComponents.KNOCKBACK, serverLevel, enchantmentLevel, itemStack, victim, damageSource, amount);
   }

   public void modifyArmorEffectivness(
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final ItemStack itemStack,
      final Entity victim,
      final DamageSource damageSource,
      final MutableFloat amount
   ) {
      this.modifyDamageFilteredValue(EnchantmentEffectComponents.ARMOR_EFFECTIVENESS, serverLevel, enchantmentLevel, itemStack, victim, damageSource, amount);
   }

   public void doPostAttack(
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final EnchantedItemInUse item,
      final EnchantmentTarget forTarget,
      final Entity victim,
      final DamageSource damageSource
   ) {
      for (TargetedConditionalEffect<EnchantmentEntityEffect> effect : this.getEffects(EnchantmentEffectComponents.POST_ATTACK)) {
         if (forTarget == effect.enchanted()) {
            doPostAttack(effect, serverLevel, enchantmentLevel, item, victim, damageSource);
         }
      }
   }

   public static void doPostAttack(
      final TargetedConditionalEffect<EnchantmentEntityEffect> effect,
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final EnchantedItemInUse item,
      final Entity victim,
      final DamageSource damageSource
   ) {
      if (effect.matches(damageContext(serverLevel, enchantmentLevel, victim, damageSource))) {
         Entity target = switch (effect.affected()) {
            case ATTACKER -> damageSource.getEntity();
            case DAMAGING_ENTITY -> damageSource.getDirectEntity();
            case VICTIM -> victim;
         };
         if (target != null) {
            effect.effect().apply(serverLevel, enchantmentLevel, item, target, target.position());
         }
      }
   }

   public void doPostPiercingAttack(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity user) {
      applyEffects(
         this.getEffects(EnchantmentEffectComponents.POST_PIERCING_ATTACK),
         entityContext(serverLevel, enchantmentLevel, user, user.position()),
         e -> e.apply(serverLevel, enchantmentLevel, item, user, user.position())
      );
   }

   public void modifyProjectileCount(
      final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack weapon, final Entity shooter, final MutableFloat count
   ) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.PROJECTILE_COUNT, serverLevel, enchantmentLevel, weapon, shooter, count);
   }

   public void modifyProjectileSpread(
      final ServerLevel serverLevel, final int enchantmentLevel, final ItemStack weapon, final Entity shooter, final MutableFloat angle
   ) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.PROJECTILE_SPREAD, serverLevel, enchantmentLevel, weapon, shooter, angle);
   }

   public void modifyCrossbowChargeTime(final RandomSource random, final int enchantmentLevel, final MutableFloat time) {
      this.modifyUnfilteredValue(EnchantmentEffectComponents.CROSSBOW_CHARGE_TIME, random, enchantmentLevel, time);
   }

   public void modifyUnfilteredValue(
      final DataComponentType<EnchantmentValueEffect> component, final RandomSource random, final int enchantmentLevel, final MutableFloat value
   ) {
      EnchantmentValueEffect effect = this.effects.get(component);
      if (effect != null) {
         value.setValue(effect.process(enchantmentLevel, random, value.floatValue()));
      }
   }

   public void tick(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity) {
      applyEffects(
         this.getEffects(EnchantmentEffectComponents.TICK),
         entityContext(serverLevel, enchantmentLevel, entity, entity.position()),
         e -> e.apply(serverLevel, enchantmentLevel, item, entity, entity.position())
      );
   }

   public void onProjectileSpawned(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse weapon, final Entity projectile) {
      applyEffects(
         this.getEffects(EnchantmentEffectComponents.PROJECTILE_SPAWNED),
         entityContext(serverLevel, enchantmentLevel, projectile, projectile.position()),
         e -> e.apply(serverLevel, enchantmentLevel, weapon, projectile, projectile.position())
      );
   }

   public void onHitBlock(
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final EnchantedItemInUse weapon,
      final Entity projectile,
      final Vec3 position,
      final BlockState hitBlock
   ) {
      applyEffects(
         this.getEffects(EnchantmentEffectComponents.HIT_BLOCK),
         blockHitContext(serverLevel, enchantmentLevel, projectile, position, hitBlock),
         e -> e.apply(serverLevel, enchantmentLevel, weapon, projectile, position)
      );
   }

   private void modifyItemFilteredCount(
      final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> effectType,
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final ItemInstance itemStack,
      final MutableFloat value
   ) {
      applyEffects(
         this.getEffects(effectType),
         itemContext(serverLevel, enchantmentLevel, itemStack),
         value,
         (e, v) -> e.process(enchantmentLevel, serverLevel.getRandom(), v)
      );
   }

   private void modifyEntityFilteredValue(
      final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> effectType,
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final ItemStack itemStack,
      final Entity entity,
      final MutableFloat value
   ) {
      applyEffects(
         this.getEffects(effectType),
         entityContext(serverLevel, enchantmentLevel, entity, entity.position()),
         value,
         (e, v) -> e.process(enchantmentLevel, entity.getRandom(), v)
      );
   }

   private void modifyDamageFilteredValue(
      final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> effectType,
      final ServerLevel serverLevel,
      final int enchantmentLevel,
      final ItemStack itemStack,
      final Entity victim,
      final DamageSource damageSource,
      final MutableFloat value
   ) {
      applyEffects(
         this.getEffects(effectType),
         damageContext(serverLevel, enchantmentLevel, victim, damageSource),
         value,
         (e, v) -> e.process(enchantmentLevel, victim.getRandom(), v)
      );
   }

   public static LootContext damageContext(final ServerLevel serverLevel, final int enchantmentLevel, final Entity victim, final DamageSource source) {
      LootParams params = new LootParams.Builder(serverLevel)
         .withParameter(LootContextParams.THIS_ENTITY, victim)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, enchantmentLevel)
         .withParameter(LootContextParams.ORIGIN, victim.position())
         .withParameter(LootContextParams.DAMAGE_SOURCE, source)
         .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
         .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
         .create(LootContextParamSets.ENCHANTED_DAMAGE);
      return new LootContext.Builder(params).create(Optional.empty());
   }

   private static LootContext itemContext(final ServerLevel serverLevel, final int enchantmentLevel, final ItemInstance itemStack) {
      LootParams params = new LootParams.Builder(serverLevel)
         .withParameter(LootContextParams.TOOL, itemStack)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, enchantmentLevel)
         .create(LootContextParamSets.ENCHANTED_ITEM);
      return new LootContext.Builder(params).create(Optional.empty());
   }

   private static LootContext locationContext(final ServerLevel serverLevel, final int enchantmentLevel, final Entity entity, final boolean active) {
      LootParams params = new LootParams.Builder(serverLevel)
         .withParameter(LootContextParams.THIS_ENTITY, entity)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, enchantmentLevel)
         .withParameter(LootContextParams.ORIGIN, entity.position())
         .withParameter(LootContextParams.ENCHANTMENT_ACTIVE, active)
         .create(LootContextParamSets.ENCHANTED_LOCATION);
      return new LootContext.Builder(params).create(Optional.empty());
   }

   private static LootContext entityContext(final ServerLevel serverLevel, final int enchantmentLevel, final Entity entity, final Vec3 position) {
      LootParams params = new LootParams.Builder(serverLevel)
         .withParameter(LootContextParams.THIS_ENTITY, entity)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, enchantmentLevel)
         .withParameter(LootContextParams.ORIGIN, position)
         .create(LootContextParamSets.ENCHANTED_ENTITY);
      return new LootContext.Builder(params).create(Optional.empty());
   }

   private static LootContext blockHitContext(
      final ServerLevel serverLevel, final int enchantmentLevel, final Entity entity, final Vec3 position, final BlockState hitBlock
   ) {
      LootParams params = new LootParams.Builder(serverLevel)
         .withParameter(LootContextParams.THIS_ENTITY, entity)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, enchantmentLevel)
         .withParameter(LootContextParams.ORIGIN, position)
         .withParameter(LootContextParams.BLOCK_STATE, hitBlock)
         .create(LootContextParamSets.HIT_BLOCK);
      return new LootContext.Builder(params).create(Optional.empty());
   }

   private static <T> void applyEffects(final List<ConditionalEffect<T>> effects, final LootContext filterData, final Enchantment.GenericAction<T> action) {
      for (ConditionalEffect<T> conditionalEffect : effects) {
         if (conditionalEffect.matches(filterData)) {
            action.apply(conditionalEffect.effect());
         }
      }
   }

   private static <T> void applyEffects(
      final List<ConditionalEffect<T>> effects, final LootContext filterData, final MutableFloat value, final Enchantment.FloatAction<T> action
   ) {
      applyEffects(effects, filterData, action.asGeneric(value));
   }

   public void runLocationChangedEffects(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final LivingEntity entity) {
      EquipmentSlot slot = item.inSlot();
      if (slot != null) {
         Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEffects = entity.activeLocationDependentEnchantments(slot);
         if (!this.matchingSlot(slot)) {
            Set<EnchantmentLocationBasedEffect> activeEffects = activeLocationDependentEffects.remove(this);
            if (activeEffects != null) {
               activeEffects.forEach(effectx -> effectx.onDeactivated(item, entity, entity.position(), enchantmentLevel));
            }
         } else {
            Set<EnchantmentLocationBasedEffect> activeEffects = activeLocationDependentEffects.get(this);

            for (ConditionalEffect<EnchantmentLocationBasedEffect> filteredEffect : this.getEffects(EnchantmentEffectComponents.LOCATION_CHANGED)) {
               EnchantmentLocationBasedEffect effect = filteredEffect.effect();
               boolean wasActive = activeEffects != null && activeEffects.contains(effect);
               if (filteredEffect.matches(locationContext(serverLevel, enchantmentLevel, entity, wasActive))) {
                  if (!wasActive) {
                     if (activeEffects == null) {
                        activeEffects = new ObjectArraySet();
                        activeLocationDependentEffects.put(this, activeEffects);
                     }

                     activeEffects.add(effect);
                  }

                  effect.onChangedBlock(serverLevel, enchantmentLevel, item, entity, entity.position(), !wasActive);
               } else if (activeEffects != null && activeEffects.remove(effect)) {
                  effect.onDeactivated(item, entity, entity.position(), enchantmentLevel);
               }
            }

            if (activeEffects != null && activeEffects.isEmpty()) {
               activeLocationDependentEffects.remove(this);
            }
         }
      }
   }

   public void stopLocationBasedEffects(final int enchantmentLevel, final EnchantedItemInUse item, final LivingEntity entity) {
      EquipmentSlot slot = item.inSlot();
      if (slot != null) {
         Set<EnchantmentLocationBasedEffect> activeEffects = entity.activeLocationDependentEnchantments(slot).remove(this);
         if (activeEffects != null) {
            for (EnchantmentLocationBasedEffect effect : activeEffects) {
               effect.onDeactivated(item, entity, entity.position(), enchantmentLevel);
            }
         }
      }
   }

   public static Enchantment.Builder enchantment(final Enchantment.EnchantmentDefinition definition) {
      return new Enchantment.Builder(definition);
   }

   public static class Builder {
      private final Enchantment.EnchantmentDefinition definition;
      private HolderSet<Enchantment> exclusiveSet = HolderSet.empty();
      private final Map<DataComponentType<?>, List<?>> effectLists = new HashMap<>();
      private final DataComponentMap.Builder effectMapBuilder = DataComponentMap.builder();

      public Builder(final Enchantment.EnchantmentDefinition definition) {
         this.definition = definition;
      }

      public Enchantment.Builder exclusiveWith(final HolderSet<Enchantment> set) {
         this.exclusiveSet = set;
         return this;
      }

      public <E> Enchantment.Builder withEffect(
         final DataComponentType<List<ConditionalEffect<E>>> type, final E effect, final LootItemCondition.Builder condition
      ) {
         this.getEffectsList(type).add(new ConditionalEffect<>(effect, Optional.of(condition.build())));
         return this;
      }

      public <E> Enchantment.Builder withEffect(final DataComponentType<List<ConditionalEffect<E>>> type, final E effect) {
         this.getEffectsList(type).add(new ConditionalEffect<>(effect, Optional.empty()));
         return this;
      }

      public <E> Enchantment.Builder withEffect(
         final DataComponentType<List<TargetedConditionalEffect<E>>> type,
         final EnchantmentTarget enchanted,
         final EnchantmentTarget affected,
         final E effect,
         final LootItemCondition.Builder condition
      ) {
         this.getEffectsList(type).add(new TargetedConditionalEffect<>(enchanted, affected, effect, Optional.of(condition.build())));
         return this;
      }

      public <E> Enchantment.Builder withEffect(
         final DataComponentType<List<TargetedConditionalEffect<E>>> type, final EnchantmentTarget enchanted, final EnchantmentTarget affected, final E effect
      ) {
         this.getEffectsList(type).add(new TargetedConditionalEffect<>(enchanted, affected, effect, Optional.empty()));
         return this;
      }

      public Enchantment.Builder withEffect(final DataComponentType<List<EnchantmentAttributeEffect>> type, final EnchantmentAttributeEffect effect) {
         this.getEffectsList(type).add(effect);
         return this;
      }

      public <E> Enchantment.Builder withSpecialEffect(final DataComponentType<E> type, final E effect) {
         this.effectMapBuilder.set(type, effect);
         return this;
      }

      public Enchantment.Builder withEffect(final DataComponentType<Unit> type) {
         this.effectMapBuilder.set(type, Unit.INSTANCE);
         return this;
      }

      private <E> List<E> getEffectsList(final DataComponentType<List<E>> type) {
         return (List<E>)this.effectLists.computeIfAbsent(type, k -> {
            ArrayList<E> newList = new ArrayList<>();
            this.effectMapBuilder.set(type, newList);
            return newList;
         });
      }

      public Enchantment build(final Identifier descriptionKey) {
         return new Enchantment(
            Component.translatable(Util.makeDescriptionId("enchantment", descriptionKey)), this.definition, this.exclusiveSet, this.effectMapBuilder.build()
         );
      }
   }

   public record Cost(int base, int perLevelAboveFirst) {
      public static final Codec<Enchantment.Cost> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.INT.fieldOf("base").forGetter(Enchantment.Cost::base),
               Codec.INT.fieldOf("per_level_above_first").forGetter(Enchantment.Cost::perLevelAboveFirst)
            )
            .apply(i, Enchantment.Cost::new)
      );

      public int calculate(final int level) {
         return this.base + this.perLevelAboveFirst * (level - 1);
      }
   }

   public record EnchantmentDefinition(
      HolderSet<Item> supportedItems,
      Optional<HolderSet<Item>> primaryItems,
      int weight,
      int maxLevel,
      Enchantment.Cost minCost,
      Enchantment.Cost maxCost,
      int anvilCost,
      List<EquipmentSlotGroup> slots
   ) {
      public static final MapCodec<Enchantment.EnchantmentDefinition> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("supported_items").forGetter(Enchantment.EnchantmentDefinition::supportedItems),
               RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("primary_items").forGetter(Enchantment.EnchantmentDefinition::primaryItems),
               ExtraCodecs.intRange(1, 1024).fieldOf("weight").forGetter(Enchantment.EnchantmentDefinition::weight),
               ExtraCodecs.intRange(1, 255).fieldOf("max_level").forGetter(Enchantment.EnchantmentDefinition::maxLevel),
               Enchantment.Cost.CODEC.fieldOf("min_cost").forGetter(Enchantment.EnchantmentDefinition::minCost),
               Enchantment.Cost.CODEC.fieldOf("max_cost").forGetter(Enchantment.EnchantmentDefinition::maxCost),
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("anvil_cost").forGetter(Enchantment.EnchantmentDefinition::anvilCost),
               EquipmentSlotGroup.CODEC.listOf().fieldOf("slots").forGetter(Enchantment.EnchantmentDefinition::slots)
            )
            .apply(i, Enchantment.EnchantmentDefinition::new)
      );
   }

   @FunctionalInterface
   private interface FloatAction<T> {
      float apply(T effect, float value);

      default Enchantment.GenericAction<T> asGeneric(final MutableFloat v) {
         return effect -> v.setValue(this.apply(effect, v.floatValue()));
      }
   }

   @FunctionalInterface
   private interface GenericAction<T> {
      void apply(T effect);
   }
}
