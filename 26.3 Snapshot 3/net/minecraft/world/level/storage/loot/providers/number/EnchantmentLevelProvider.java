package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record EnchantmentLevelProvider(LevelBasedValue amount) implements NumberProvider {
   public static final MapCodec<EnchantmentLevelProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(LevelBasedValue.CODEC.fieldOf("amount").forGetter(EnchantmentLevelProvider::amount)).apply(i, EnchantmentLevelProvider::new)
   );

   @Override
   public MapCodec<EnchantmentLevelProvider> codec() {
      return MAP_CODEC;
   }

   @Override
   public float getFloat(final LootContext context) {
      int level = context.getParameter(LootContextParams.ENCHANTMENT_LEVEL);
      return this.amount.calculate(level);
   }

   public static EnchantmentLevelProvider forEnchantmentLevel(final LevelBasedValue amount) {
      return new EnchantmentLevelProvider(amount);
   }
}
