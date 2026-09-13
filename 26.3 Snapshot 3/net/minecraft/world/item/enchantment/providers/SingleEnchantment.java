package net.minecraft.world.item.enchantment.providers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record SingleEnchantment(Holder<Enchantment> enchantment, IntProvider level) implements EnchantmentProvider {
   public static final MapCodec<SingleEnchantment> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Enchantment.CODEC.fieldOf("enchantment").forGetter(SingleEnchantment::enchantment),
            IntProviders.CODEC.fieldOf("level").forGetter(SingleEnchantment::level)
         )
         .apply(i, SingleEnchantment::new)
   );

   @Override
   public void enchant(final ItemStack item, final ItemEnchantments.Mutable itemEnchantments, final RandomSource random, final DifficultyInstance difficulty) {
      itemEnchantments.upgrade(
         this.enchantment, Mth.clamp(this.level.sample(random), this.enchantment.value().getMinLevel(), this.enchantment.value().getMaxLevel())
      );
   }

   @Override
   public MapCodec<SingleEnchantment> codec() {
      return CODEC;
   }
}
