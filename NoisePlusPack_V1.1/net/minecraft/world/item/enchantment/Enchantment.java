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
import java.util.function.Consumer;
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
      p_344995_ -> p_344995_.group(
            ComponentSerialization.CODEC.fieldOf("description").forGetter(Enchantment::description),
            Enchantment.EnchantmentDefinition.CODEC.forGetter(Enchantment::definition),
            RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("exclusive_set", HolderSet.direct()).forGetter(Enchantment::exclusiveSet),
            EnchantmentEffectComponents.CODEC.optionalFieldOf("effects", DataComponentMap.EMPTY).forGetter(Enchantment::effects)
         )
         .apply(p_344995_, Enchantment::new)
   );
   public static final Codec<Holder<Enchantment>> CODEC = RegistryFixedCodec.create(Registries.ENCHANTMENT);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Enchantment>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT);

   public static Enchantment.Cost constantCost(int p_334530_) {
      return new Enchantment.Cost(p_334530_, 0);
   }

   public static Enchantment.Cost dynamicCost(int p_334326_, int p_335507_) {
      return new Enchantment.Cost(p_334326_, p_335507_);
   }

   public static Enchantment.EnchantmentDefinition definition(
      HolderSet<Item> p_345362_,
      HolderSet<Item> p_343516_,
      int p_328611_,
      int p_336009_,
      Enchantment.Cost p_330605_,
      Enchantment.Cost p_333983_,
      int p_327771_,
      EquipmentSlotGroup... p_344843_
   ) {
      return new Enchantment.EnchantmentDefinition(p_345362_, Optional.of(p_343516_), p_328611_, p_336009_, p_330605_, p_333983_, p_327771_, List.of(p_344843_));
   }

   public static Enchantment.EnchantmentDefinition definition(
      HolderSet<Item> p_342934_,
      int p_329635_,
      int p_331888_,
      Enchantment.Cost p_328182_,
      Enchantment.Cost p_328787_,
      int p_333931_,
      EquipmentSlotGroup... p_342587_
   ) {
      return new Enchantment.EnchantmentDefinition(p_342934_, Optional.empty(), p_329635_, p_331888_, p_328182_, p_328787_, p_333931_, List.of(p_342587_));
   }

   public Map<EquipmentSlot, ItemStack> getSlotItems(LivingEntity p_44685_) {
      Map<EquipmentSlot, ItemStack> map = Maps.newEnumMap(EquipmentSlot.class);

      for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
         if (this.matchingSlot(equipmentslot)) {
            ItemStack itemstack = p_44685_.getItemBySlot(equipmentslot);
            if (!itemstack.isEmpty()) {
               map.put(equipmentslot, itemstack);
            }
         }
      }

      return map;
   }

   public HolderSet<Item> getSupportedItems() {
      return this.definition.supportedItems();
   }

   public boolean matchingSlot(EquipmentSlot p_344889_) {
      return this.definition.slots().stream().anyMatch(p_345380_ -> p_345380_.test(p_344889_));
   }

   public boolean isPrimaryItem(ItemStack p_334183_) {
      return this.isSupportedItem(p_334183_) && (this.definition.primaryItems.isEmpty() || p_334183_.is(this.definition.primaryItems.get()));
   }

   public boolean isSupportedItem(ItemStack p_343312_) {
      return p_343312_.is(this.definition.supportedItems);
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

   public int getMinCost(int p_44679_) {
      return this.definition.minCost().calculate(p_44679_);
   }

   public int getMaxCost(int p_44691_) {
      return this.definition.maxCost().calculate(p_44691_);
   }

   @Override
   public String toString() {
      return "Enchantment " + this.description.getString();
   }

   public static boolean areCompatible(Holder<Enchantment> p_345028_, Holder<Enchantment> p_342568_) {
      return !p_345028_.equals(p_342568_) && !p_345028_.value().exclusiveSet.contains(p_342568_) && !p_342568_.value().exclusiveSet.contains(p_345028_);
   }

   public static Component getFullname(Holder<Enchantment> p_342825_, int p_44701_) {
      MutableComponent mutablecomponent = p_342825_.value().description.copy();
      if (p_342825_.is(EnchantmentTags.CURSE)) {
         mutablecomponent = ComponentUtils.mergeStyles(mutablecomponent, Style.EMPTY.withColor(ChatFormatting.RED));
      } else {
         mutablecomponent = ComponentUtils.mergeStyles(mutablecomponent, Style.EMPTY.withColor(ChatFormatting.GRAY));
      }

      if (p_44701_ != 1 || p_342825_.value().getMaxLevel() != 1) {
         mutablecomponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + p_44701_));
      }

      return mutablecomponent;
   }

   public boolean canEnchant(ItemStack p_44689_) {
      return this.definition.supportedItems().contains(p_44689_.getItemHolder());
   }

   public <T> List<T> getEffects(DataComponentType<List<T>> p_344699_) {
      return this.effects.getOrDefault(p_344699_, List.of());
   }

   public boolean isImmuneToDamage(ServerLevel p_344493_, int p_345313_, Entity p_342613_, DamageSource p_345401_) {
      LootContext lootcontext = damageContext(p_344493_, p_345313_, p_342613_, p_345401_);

      for (ConditionalEffect<DamageImmunity> conditionaleffect : this.getEffects(EnchantmentEffectComponents.DAMAGE_IMMUNITY)) {
         if (conditionaleffect.matches(lootcontext)) {
            return true;
         }
      }

      return false;
   }

   public void modifyDamageProtection(
      ServerLevel p_344642_, int p_344297_, ItemStack p_345382_, Entity p_342229_, DamageSource p_342824_, MutableFloat p_345325_
   ) {
      LootContext lootcontext = damageContext(p_344642_, p_344297_, p_342229_, p_342824_);

      for (ConditionalEffect<EnchantmentValueEffect> conditionaleffect : this.getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION)) {
         if (conditionaleffect.matches(lootcontext)) {
            p_345325_.setValue(conditionaleffect.effect().process(p_344297_, p_342229_.getRandom(), p_345325_.floatValue()));
         }
      }
   }

   public void modifyDurabilityChange(ServerLevel p_342774_, int p_345090_, ItemStack p_343700_, MutableFloat p_343591_) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.ITEM_DAMAGE, p_342774_, p_345090_, p_343700_, p_343591_);
   }

   public void modifyAmmoCount(ServerLevel p_342942_, int p_342884_, ItemStack p_344742_, MutableFloat p_343607_) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.AMMO_USE, p_342942_, p_342884_, p_344742_, p_343607_);
   }

   public void modifyPiercingCount(ServerLevel p_342338_, int p_344838_, ItemStack p_343994_, MutableFloat p_342065_) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.PROJECTILE_PIERCING, p_342338_, p_344838_, p_343994_, p_342065_);
   }

   public void modifyBlockExperience(ServerLevel p_342072_, int p_343543_, ItemStack p_343319_, MutableFloat p_342766_) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.BLOCK_EXPERIENCE, p_342072_, p_343543_, p_343319_, p_342766_);
   }

   public void modifyMobExperience(ServerLevel p_343695_, int p_342521_, ItemStack p_344064_, Entity p_342215_, MutableFloat p_345517_) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.MOB_EXPERIENCE, p_343695_, p_342521_, p_344064_, p_342215_, p_345517_);
   }

   public void modifyDurabilityToRepairFromXp(ServerLevel p_343302_, int p_343123_, ItemStack p_343007_, MutableFloat p_342327_) {
      this.modifyItemFilteredCount(EnchantmentEffectComponents.REPAIR_WITH_XP, p_343302_, p_343123_, p_343007_, p_342327_);
   }

   public void modifyTridentReturnToOwnerAcceleration(ServerLevel p_344014_, int p_345042_, ItemStack p_343051_, Entity p_342961_, MutableFloat p_343498_) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.TRIDENT_RETURN_ACCELERATION, p_344014_, p_345042_, p_343051_, p_342961_, p_343498_);
   }

   public void modifyTridentSpinAttackStrength(RandomSource p_343013_, int p_342342_, MutableFloat p_342582_) {
      this.modifyUnfilteredValue(EnchantmentEffectComponents.TRIDENT_SPIN_ATTACK_STRENGTH, p_343013_, p_342342_, p_342582_);
   }

   public void modifyFishingTimeReduction(ServerLevel p_343004_, int p_344690_, ItemStack p_342235_, Entity p_344564_, MutableFloat p_345086_) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.FISHING_TIME_REDUCTION, p_343004_, p_344690_, p_342235_, p_344564_, p_345086_);
   }

   public void modifyFishingLuckBonus(ServerLevel p_342216_, int p_343343_, ItemStack p_344550_, Entity p_343347_, MutableFloat p_345240_) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.FISHING_LUCK_BONUS, p_342216_, p_343343_, p_344550_, p_343347_, p_345240_);
   }

   public void modifyDamage(ServerLevel p_343328_, int p_344751_, ItemStack p_342664_, Entity p_344239_, DamageSource p_345253_, MutableFloat p_344727_) {
      this.modifyDamageFilteredValue(EnchantmentEffectComponents.DAMAGE, p_343328_, p_344751_, p_342664_, p_344239_, p_345253_, p_344727_);
   }

   public void modifyFallBasedDamage(
      ServerLevel p_344377_, int p_342769_, ItemStack p_344741_, Entity p_344920_, DamageSource p_345151_, MutableFloat p_343049_
   ) {
      this.modifyDamageFilteredValue(
         EnchantmentEffectComponents.SMASH_DAMAGE_PER_FALLEN_BLOCK, p_344377_, p_342769_, p_344741_, p_344920_, p_345151_, p_343049_
      );
   }

   public void modifyKnockback(ServerLevel p_345022_, int p_343469_, ItemStack p_343441_, Entity p_345003_, DamageSource p_345200_, MutableFloat p_345434_) {
      this.modifyDamageFilteredValue(EnchantmentEffectComponents.KNOCKBACK, p_345022_, p_343469_, p_343441_, p_345003_, p_345200_, p_345434_);
   }

   public void modifyArmorEffectivness(
      ServerLevel p_345291_, int p_343247_, ItemStack p_343537_, Entity p_344244_, DamageSource p_344953_, MutableFloat p_345146_
   ) {
      this.modifyDamageFilteredValue(EnchantmentEffectComponents.ARMOR_EFFECTIVENESS, p_345291_, p_343247_, p_343537_, p_344244_, p_344953_, p_345146_);
   }

   public void doPostAttack(
      ServerLevel p_342826_, int p_343675_, EnchantedItemInUse p_343641_, EnchantmentTarget p_342372_, Entity p_344548_, DamageSource p_342692_
   ) {
      for (TargetedConditionalEffect<EnchantmentEntityEffect> targetedconditionaleffect : this.getEffects(EnchantmentEffectComponents.POST_ATTACK)) {
         if (p_342372_ == targetedconditionaleffect.enchanted()) {
            doPostAttack(targetedconditionaleffect, p_342826_, p_343675_, p_343641_, p_344548_, p_342692_);
         }
      }
   }

   public static void doPostAttack(
      TargetedConditionalEffect<EnchantmentEntityEffect> p_344913_,
      ServerLevel p_344428_,
      int p_44688_,
      EnchantedItemInUse p_344260_,
      Entity p_44687_,
      DamageSource p_344028_
   ) {
      if (p_344913_.matches(damageContext(p_344428_, p_44688_, p_44687_, p_344028_))) {
         Entity entity = switch (p_344913_.affected()) {
            case ATTACKER -> p_344028_.getEntity();
            case DAMAGING_ENTITY -> p_344028_.getDirectEntity();
            case VICTIM -> p_44687_;
         };
         if (entity != null) {
            p_344913_.effect().apply(p_344428_, p_44688_, p_344260_, entity, entity.position());
         }
      }
   }

   public void doLunge(ServerLevel p_451740_, int p_452223_, EnchantedItemInUse p_460564_, Entity p_460331_) {
      applyEffects(
         this.getEffects(EnchantmentEffectComponents.POST_PIERCING_ATTACK),
         entityContext(p_451740_, p_452223_, p_460331_, p_460331_.position()),
         p_344229_ -> p_344229_.apply(p_451740_, p_452223_, p_460564_, p_460331_, p_460331_.position())
      );
   }

   public void modifyProjectileCount(ServerLevel p_344710_, int p_344927_, ItemStack p_343332_, Entity p_344173_, MutableFloat p_345307_) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.PROJECTILE_COUNT, p_344710_, p_344927_, p_343332_, p_344173_, p_345307_);
   }

   public void modifyProjectileSpread(ServerLevel p_342398_, int p_342291_, ItemStack p_345308_, Entity p_343809_, MutableFloat p_343769_) {
      this.modifyEntityFilteredValue(EnchantmentEffectComponents.PROJECTILE_SPREAD, p_342398_, p_342291_, p_345308_, p_343809_, p_343769_);
   }

   public void modifyCrossbowChargeTime(RandomSource p_345467_, int p_343593_, MutableFloat p_343046_) {
      this.modifyUnfilteredValue(EnchantmentEffectComponents.CROSSBOW_CHARGE_TIME, p_345467_, p_343593_, p_343046_);
   }

   public void modifyUnfilteredValue(DataComponentType<EnchantmentValueEffect> p_342685_, RandomSource p_344622_, int p_343957_, MutableFloat p_342813_) {
      EnchantmentValueEffect enchantmentvalueeffect = this.effects.get(p_342685_);
      if (enchantmentvalueeffect != null) {
         p_342813_.setValue(enchantmentvalueeffect.process(p_343957_, p_344622_, p_342813_.floatValue()));
      }
   }

   public void tick(ServerLevel p_345036_, int p_344388_, EnchantedItemInUse p_344279_, Entity p_342497_) {
      applyEffects(
         this.getEffects(EnchantmentEffectComponents.TICK),
         entityContext(p_345036_, p_344388_, p_342497_, p_342497_.position()),
         p_449849_ -> p_449849_.apply(p_345036_, p_344388_, p_344279_, p_342497_, p_342497_.position())
      );
   }

   public void onProjectileSpawned(ServerLevel p_344400_, int p_344273_, EnchantedItemInUse p_342998_, Entity p_344540_) {
      applyEffects(
         this.getEffects(EnchantmentEffectComponents.PROJECTILE_SPAWNED),
         entityContext(p_344400_, p_344273_, p_344540_, p_344540_.position()),
         p_342906_ -> p_342906_.apply(p_344400_, p_344273_, p_342998_, p_344540_, p_344540_.position())
      );
   }

   public void onHitBlock(ServerLevel p_343964_, int p_343622_, EnchantedItemInUse p_344872_, Entity p_342891_, Vec3 p_344312_, BlockState p_345016_) {
      applyEffects(
         this.getEffects(EnchantmentEffectComponents.HIT_BLOCK),
         blockHitContext(p_343964_, p_343622_, p_342891_, p_344312_, p_345016_),
         p_343722_ -> p_343722_.apply(p_343964_, p_343622_, p_344872_, p_342891_, p_344312_)
      );
   }

   private void modifyItemFilteredCount(
      DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> p_342071_,
      ServerLevel p_342667_,
      int p_344856_,
      ItemStack p_343442_,
      MutableFloat p_342223_
   ) {
      applyEffects(
         this.getEffects(p_342071_),
         itemContext(p_342667_, p_344856_, p_343442_),
         p_449840_ -> p_342223_.setValue(p_449840_.process(p_344856_, p_342667_.getRandom(), p_342223_.floatValue()))
      );
   }

   private void modifyEntityFilteredValue(
      DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> p_343552_,
      ServerLevel p_344107_,
      int p_342319_,
      ItemStack p_342576_,
      Entity p_343285_,
      MutableFloat p_342059_
   ) {
      applyEffects(
         this.getEffects(p_343552_),
         entityContext(p_344107_, p_342319_, p_343285_, p_343285_.position()),
         p_344133_ -> p_342059_.setValue(p_344133_.process(p_342319_, p_343285_.getRandom(), p_342059_.floatValue()))
      );
   }

   private void modifyDamageFilteredValue(
      DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> p_342460_,
      ServerLevel p_344318_,
      int p_343892_,
      ItemStack p_345407_,
      Entity p_343559_,
      DamageSource p_344560_,
      MutableFloat p_344658_
   ) {
      applyEffects(
         this.getEffects(p_342460_),
         damageContext(p_344318_, p_343892_, p_343559_, p_344560_),
         p_344340_ -> p_344658_.setValue(p_344340_.process(p_343892_, p_343559_.getRandom(), p_344658_.floatValue()))
      );
   }

   public static LootContext damageContext(ServerLevel p_342651_, int p_344201_, Entity p_345425_, DamageSource p_343766_) {
      LootParams lootparams = new LootParams.Builder(p_342651_)
         .withParameter(LootContextParams.THIS_ENTITY, p_345425_)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, p_344201_)
         .withParameter(LootContextParams.ORIGIN, p_345425_.position())
         .withParameter(LootContextParams.DAMAGE_SOURCE, p_343766_)
         .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, p_343766_.getEntity())
         .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, p_343766_.getDirectEntity())
         .create(LootContextParamSets.ENCHANTED_DAMAGE);
      return new LootContext.Builder(lootparams).create(Optional.empty());
   }

   private static LootContext itemContext(ServerLevel p_344430_, int p_344526_, ItemStack p_343134_) {
      LootParams lootparams = new LootParams.Builder(p_344430_)
         .withParameter(LootContextParams.TOOL, p_343134_)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, p_344526_)
         .create(LootContextParamSets.ENCHANTED_ITEM);
      return new LootContext.Builder(lootparams).create(Optional.empty());
   }

   private static LootContext locationContext(ServerLevel p_342658_, int p_342243_, Entity p_345215_, boolean p_342535_) {
      LootParams lootparams = new LootParams.Builder(p_342658_)
         .withParameter(LootContextParams.THIS_ENTITY, p_345215_)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, p_342243_)
         .withParameter(LootContextParams.ORIGIN, p_345215_.position())
         .withParameter(LootContextParams.ENCHANTMENT_ACTIVE, p_342535_)
         .create(LootContextParamSets.ENCHANTED_LOCATION);
      return new LootContext.Builder(lootparams).create(Optional.empty());
   }

   private static LootContext entityContext(ServerLevel p_342654_, int p_343984_, Entity p_342853_, Vec3 p_343585_) {
      LootParams lootparams = new LootParams.Builder(p_342654_)
         .withParameter(LootContextParams.THIS_ENTITY, p_342853_)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, p_343984_)
         .withParameter(LootContextParams.ORIGIN, p_343585_)
         .create(LootContextParamSets.ENCHANTED_ENTITY);
      return new LootContext.Builder(lootparams).create(Optional.empty());
   }

   private static LootContext blockHitContext(ServerLevel p_342041_, int p_344013_, Entity p_345496_, Vec3 p_343741_, BlockState p_342321_) {
      LootParams lootparams = new LootParams.Builder(p_342041_)
         .withParameter(LootContextParams.THIS_ENTITY, p_345496_)
         .withParameter(LootContextParams.ENCHANTMENT_LEVEL, p_344013_)
         .withParameter(LootContextParams.ORIGIN, p_343741_)
         .withParameter(LootContextParams.BLOCK_STATE, p_342321_)
         .create(LootContextParamSets.HIT_BLOCK);
      return new LootContext.Builder(lootparams).create(Optional.empty());
   }

   private static <T> void applyEffects(List<ConditionalEffect<T>> p_345356_, LootContext p_343574_, Consumer<T> p_343387_) {
      for (ConditionalEffect<T> conditionaleffect : p_345356_) {
         if (conditionaleffect.matches(p_343574_)) {
            p_343387_.accept(conditionaleffect.effect());
         }
      }
   }

   public void runLocationChangedEffects(ServerLevel p_343889_, int p_344660_, EnchantedItemInUse p_344903_, LivingEntity p_342969_) {
      EquipmentSlot equipmentslot = p_344903_.inSlot();
      if (equipmentslot != null) {
         Map<Enchantment, Set<EnchantmentLocationBasedEffect>> map = p_342969_.activeLocationDependentEnchantments(equipmentslot);
         if (!this.matchingSlot(equipmentslot)) {
            Set<EnchantmentLocationBasedEffect> set1 = map.remove(this);
            if (set1 != null) {
               set1.forEach(p_449844_ -> p_449844_.onDeactivated(p_344903_, p_342969_, p_342969_.position(), p_344660_));
            }
         } else {
            Set<EnchantmentLocationBasedEffect> set = map.get(this);

            for (ConditionalEffect<EnchantmentLocationBasedEffect> conditionaleffect : this.getEffects(EnchantmentEffectComponents.LOCATION_CHANGED)) {
               EnchantmentLocationBasedEffect enchantmentlocationbasedeffect = conditionaleffect.effect();
               boolean flag = set != null && set.contains(enchantmentlocationbasedeffect);
               if (conditionaleffect.matches(locationContext(p_343889_, p_344660_, p_342969_, flag))) {
                  if (!flag) {
                     if (set == null) {
                        set = new ObjectArraySet();
                        map.put(this, set);
                     }

                     set.add(enchantmentlocationbasedeffect);
                  }

                  enchantmentlocationbasedeffect.onChangedBlock(p_343889_, p_344660_, p_344903_, p_342969_, p_342969_.position(), !flag);
               } else if (set != null && set.remove(enchantmentlocationbasedeffect)) {
                  enchantmentlocationbasedeffect.onDeactivated(p_344903_, p_342969_, p_342969_.position(), p_344660_);
               }
            }

            if (set != null && set.isEmpty()) {
               map.remove(this);
            }
         }
      }
   }

   public void stopLocationBasedEffects(int p_342505_, EnchantedItemInUse p_342723_, LivingEntity p_345268_) {
      EquipmentSlot equipmentslot = p_342723_.inSlot();
      if (equipmentslot != null) {
         Set<EnchantmentLocationBasedEffect> set = p_345268_.activeLocationDependentEnchantments(equipmentslot).remove(this);
         if (set != null) {
            for (EnchantmentLocationBasedEffect enchantmentlocationbasedeffect : set) {
               enchantmentlocationbasedeffect.onDeactivated(p_342723_, p_345268_, p_345268_.position(), p_342505_);
            }
         }
      }
   }

   public static Enchantment.Builder enchantment(Enchantment.EnchantmentDefinition p_342298_) {
      return new Enchantment.Builder(p_342298_);
   }

   public static class Builder {
      private final Enchantment.EnchantmentDefinition definition;
      private HolderSet<Enchantment> exclusiveSet = HolderSet.direct();
      private final Map<DataComponentType<?>, List<?>> effectLists = new HashMap<>();
      private final DataComponentMap.Builder effectMapBuilder = DataComponentMap.builder();

      public Builder(Enchantment.EnchantmentDefinition p_343724_) {
         this.definition = p_343724_;
      }

      public Enchantment.Builder exclusiveWith(HolderSet<Enchantment> p_342789_) {
         this.exclusiveSet = p_342789_;
         return this;
      }

      public <E> Enchantment.Builder withEffect(DataComponentType<List<ConditionalEffect<E>>> p_345040_, E p_342593_, LootItemCondition.Builder p_344651_) {
         this.getEffectsList(p_345040_).add(new ConditionalEffect<>(p_342593_, Optional.of(p_344651_.build())));
         return this;
      }

      public <E> Enchantment.Builder withEffect(DataComponentType<List<ConditionalEffect<E>>> p_344612_, E p_343461_) {
         this.getEffectsList(p_344612_).add(new ConditionalEffect<>(p_343461_, Optional.empty()));
         return this;
      }

      public <E> Enchantment.Builder withEffect(
         DataComponentType<List<TargetedConditionalEffect<E>>> p_343061_,
         EnchantmentTarget p_342247_,
         EnchantmentTarget p_344619_,
         E p_344716_,
         LootItemCondition.Builder p_344245_
      ) {
         this.getEffectsList(p_343061_).add(new TargetedConditionalEffect<>(p_342247_, p_344619_, p_344716_, Optional.of(p_344245_.build())));
         return this;
      }

      public <E> Enchantment.Builder withEffect(
         DataComponentType<List<TargetedConditionalEffect<E>>> p_342938_, EnchantmentTarget p_342145_, EnchantmentTarget p_345458_, E p_344837_
      ) {
         this.getEffectsList(p_342938_).add(new TargetedConditionalEffect<>(p_342145_, p_345458_, p_344837_, Optional.empty()));
         return this;
      }

      public Enchantment.Builder withEffect(DataComponentType<List<EnchantmentAttributeEffect>> p_342540_, EnchantmentAttributeEffect p_344032_) {
         this.getEffectsList(p_342540_).add(p_344032_);
         return this;
      }

      public <E> Enchantment.Builder withSpecialEffect(DataComponentType<E> p_342163_, E p_344148_) {
         this.effectMapBuilder.set(p_342163_, p_344148_);
         return this;
      }

      public Enchantment.Builder withEffect(DataComponentType<Unit> p_344219_) {
         this.effectMapBuilder.set(p_344219_, Unit.INSTANCE);
         return this;
      }

      private <E> List<E> getEffectsList(DataComponentType<List<E>> p_343556_) {
         return (List<E>)this.effectLists.computeIfAbsent(p_343556_, p_344394_ -> {
            ArrayList<E> arraylist = new ArrayList<>();
            this.effectMapBuilder.set(p_343556_, arraylist);
            return arraylist;
         });
      }

      public Enchantment build(Identifier p_458101_) {
         return new Enchantment(
            Component.translatable(Util.makeDescriptionId("enchantment", p_458101_)), this.definition, this.exclusiveSet, this.effectMapBuilder.build()
         );
      }
   }

   public record Cost(int base, int perLevelAboveFirst) {
      public static final Codec<Enchantment.Cost> CODEC = RecordCodecBuilder.create(
         p_345482_ -> p_345482_.group(
               Codec.INT.fieldOf("base").forGetter(Enchantment.Cost::base),
               Codec.INT.fieldOf("per_level_above_first").forGetter(Enchantment.Cost::perLevelAboveFirst)
            )
            .apply(p_345482_, Enchantment.Cost::new)
      );

      public int calculate(int p_333351_) {
         return this.base + this.perLevelAboveFirst * (p_333351_ - 1);
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
         p_344743_ -> p_344743_.group(
               RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("supported_items").forGetter(Enchantment.EnchantmentDefinition::supportedItems),
               RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("primary_items").forGetter(Enchantment.EnchantmentDefinition::primaryItems),
               ExtraCodecs.intRange(1, 1024).fieldOf("weight").forGetter(Enchantment.EnchantmentDefinition::weight),
               ExtraCodecs.intRange(1, 255).fieldOf("max_level").forGetter(Enchantment.EnchantmentDefinition::maxLevel),
               Enchantment.Cost.CODEC.fieldOf("min_cost").forGetter(Enchantment.EnchantmentDefinition::minCost),
               Enchantment.Cost.CODEC.fieldOf("max_cost").forGetter(Enchantment.EnchantmentDefinition::maxCost),
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("anvil_cost").forGetter(Enchantment.EnchantmentDefinition::anvilCost),
               EquipmentSlotGroup.CODEC.listOf().fieldOf("slots").forGetter(Enchantment.EnchantmentDefinition::slots)
            )
            .apply(p_344743_, Enchantment.EnchantmentDefinition::new)
      );
   }
}
