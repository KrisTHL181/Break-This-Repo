package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record LootItemRandomChanceWithEnchantedBonusCondition(float unenchantedChance, LevelBasedValue enchantedChance, Holder<Enchantment> enchantment)
   implements LootItemCondition {
   public static final MapCodec<LootItemRandomChanceWithEnchantedBonusCondition> CODEC = RecordCodecBuilder.mapCodec(
      p_345513_ -> p_345513_.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("unenchanted_chance").forGetter(LootItemRandomChanceWithEnchantedBonusCondition::unenchantedChance),
            LevelBasedValue.CODEC.fieldOf("enchanted_chance").forGetter(LootItemRandomChanceWithEnchantedBonusCondition::enchantedChance),
            Enchantment.CODEC.fieldOf("enchantment").forGetter(LootItemRandomChanceWithEnchantedBonusCondition::enchantment)
         )
         .apply(p_345513_, LootItemRandomChanceWithEnchantedBonusCondition::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.RANDOM_CHANCE_WITH_ENCHANTED_BONUS;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ATTACKING_ENTITY);
   }

   public boolean test(LootContext p_343845_) {
      Entity entity = p_343845_.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);
      int i = entity instanceof LivingEntity livingentity ? EnchantmentHelper.getEnchantmentLevel(this.enchantment, livingentity) : 0;
      float f = i > 0 ? this.enchantedChance.calculate(i) : this.unenchantedChance;
      return p_343845_.getRandom().nextFloat() < f;
   }

   public static LootItemCondition.Builder randomChanceAndLootingBoost(HolderLookup.Provider p_343257_, float p_343637_, float p_342446_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = p_343257_.lookupOrThrow(Registries.ENCHANTMENT);
      return () -> new LootItemRandomChanceWithEnchantedBonusCondition(
         p_343637_, new LevelBasedValue.Linear(p_343637_ + p_342446_, p_342446_), registrylookup.getOrThrow(Enchantments.LOOTING)
      );
   }
}
