package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
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
   public static final MapCodec<LootItemRandomChanceWithEnchantedBonusCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("unenchanted_chance").forGetter(LootItemRandomChanceWithEnchantedBonusCondition::unenchantedChance),
            LevelBasedValue.CODEC.fieldOf("enchanted_chance").forGetter(LootItemRandomChanceWithEnchantedBonusCondition::enchantedChance),
            Enchantment.CODEC.fieldOf("enchantment").forGetter(LootItemRandomChanceWithEnchantedBonusCondition::enchantment)
         )
         .apply(i, LootItemRandomChanceWithEnchantedBonusCondition::new)
   );

   @Override
   public MapCodec<LootItemRandomChanceWithEnchantedBonusCondition> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ATTACKING_ENTITY);
   }

   public boolean test(final LootContext context) {
      Entity killerEntity = context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);
      int enchantmentLevel = killerEntity instanceof LivingEntity livingKiller ? EnchantmentHelper.getEnchantmentLevel(this.enchantment, livingKiller) : 0;
      float chance = enchantmentLevel > 0 ? this.enchantedChance.calculate(enchantmentLevel) : this.unenchantedChance;
      return context.getRandom().nextFloat() < chance;
   }

   public static LootItemCondition.Builder randomChanceAndLootingBoost(
      final HolderGetter<Enchantment> enchantments, final float chance, final float perEnchantmentLevel
   ) {
      return () -> new LootItemRandomChanceWithEnchantedBonusCondition(
         chance, new LevelBasedValue.Linear(chance + perEnchantmentLevel, perEnchantmentLevel), enchantments.getOrThrow(Enchantments.LOOTING)
      );
   }
}
