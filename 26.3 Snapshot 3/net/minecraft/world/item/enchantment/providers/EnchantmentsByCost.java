package net.minecraft.world.item.enchantment.providers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record EnchantmentsByCost(HolderSet<Enchantment> enchantments, IntProvider cost) implements EnchantmentProvider {
   public static final MapCodec<EnchantmentsByCost> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).fieldOf("enchantments").forGetter(EnchantmentsByCost::enchantments),
            IntProviders.CODEC.fieldOf("cost").forGetter(EnchantmentsByCost::cost)
         )
         .apply(i, EnchantmentsByCost::new)
   );

   @Override
   public void enchant(final ItemStack item, final ItemEnchantments.Mutable itemEnchantments, final RandomSource random, final DifficultyInstance difficulty) {
      for (EnchantmentInstance instance : EnchantmentHelper.selectEnchantment(random, item, this.cost.sample(random), this.enchantments.stream())) {
         itemEnchantments.upgrade(instance.enchantment(), instance.level());
      }
   }

   @Override
   public MapCodec<EnchantmentsByCost> codec() {
      return CODEC;
   }
}
