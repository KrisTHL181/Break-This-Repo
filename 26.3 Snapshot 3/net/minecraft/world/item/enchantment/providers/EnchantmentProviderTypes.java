package net.minecraft.world.item.enchantment.providers;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public interface EnchantmentProviderTypes {
   static MapCodec<? extends EnchantmentProvider> bootstrap(final Registry<MapCodec<? extends EnchantmentProvider>> registry) {
      Registry.register(registry, "by_cost", EnchantmentsByCost.CODEC);
      Registry.register(registry, "by_cost_with_difficulty", EnchantmentsByCostWithDifficulty.CODEC);
      return Registry.register(registry, "single", SingleEnchantment.CODEC);
   }
}
