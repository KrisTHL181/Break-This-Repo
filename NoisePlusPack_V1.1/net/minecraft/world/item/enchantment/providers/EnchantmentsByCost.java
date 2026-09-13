package net.minecraft.world.item.enchantment.providers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record EnchantmentsByCost(HolderSet<Enchantment> enchantments, IntProvider cost) implements EnchantmentProvider {
   public static final MapCodec<EnchantmentsByCost> CODEC = RecordCodecBuilder.mapCodec(
      p_345021_ -> p_345021_.group(
            RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).fieldOf("enchantments").forGetter(EnchantmentsByCost::enchantments),
            IntProvider.CODEC.fieldOf("cost").forGetter(EnchantmentsByCost::cost)
         )
         .apply(p_345021_, EnchantmentsByCost::new)
   );

   @Override
   public void enchant(ItemStack p_344059_, ItemEnchantments.Mutable p_344702_, RandomSource p_344509_, DifficultyInstance p_345381_) {
      for (EnchantmentInstance enchantmentinstance : EnchantmentHelper.selectEnchantment(
         p_344509_, p_344059_, this.cost.sample(p_344509_), this.enchantments.stream()
      )) {
         p_344702_.upgrade(enchantmentinstance.enchantment(), enchantmentinstance.level());
      }
   }

   @Override
   public MapCodec<EnchantmentsByCost> codec() {
      return CODEC;
   }
}
