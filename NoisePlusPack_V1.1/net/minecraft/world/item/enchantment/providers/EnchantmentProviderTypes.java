package net.minecraft.world.item.enchantment.providers;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public interface EnchantmentProviderTypes {
   static MapCodec<? extends EnchantmentProvider> bootstrap(Registry<MapCodec<? extends EnchantmentProvider>> p_345094_) {
      Registry.register(p_345094_, "by_cost", EnchantmentsByCost.CODEC);
      Registry.register(p_345094_, "by_cost_with_difficulty", EnchantmentsByCostWithDifficulty.CODEC);
      return Registry.register(p_345094_, "single", SingleEnchantment.CODEC);
   }
}
