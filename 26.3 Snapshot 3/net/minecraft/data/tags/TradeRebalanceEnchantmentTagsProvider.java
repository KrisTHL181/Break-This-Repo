package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class TradeRebalanceEnchantmentTagsProvider extends TagsProvider<Enchantment> {
   public TradeRebalanceEnchantmentTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, Registries.ENCHANTMENT, lookupProvider);
   }

   @Override
   protected void addTags(final HolderLookup.Provider registries) {
      this.tag(EnchantmentTags.TRADES_DESERT_COMMON).add(Enchantments.FIRE_PROTECTION, Enchantments.THORNS, Enchantments.INFINITY);
      this.tag(EnchantmentTags.TRADES_JUNGLE_COMMON).add(Enchantments.FEATHER_FALLING, Enchantments.PROJECTILE_PROTECTION, Enchantments.POWER);
      this.tag(EnchantmentTags.TRADES_PLAINS_COMMON).add(Enchantments.PUNCH, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS);
      this.tag(EnchantmentTags.TRADES_SAVANNA_COMMON).add(Enchantments.KNOCKBACK, Enchantments.BINDING_CURSE, Enchantments.SWEEPING_EDGE);
      this.tag(EnchantmentTags.TRADES_SNOW_COMMON).add(Enchantments.AQUA_AFFINITY, Enchantments.LOOTING, Enchantments.FROST_WALKER);
      this.tag(EnchantmentTags.TRADES_SWAMP_COMMON).add(Enchantments.DEPTH_STRIDER, Enchantments.RESPIRATION, Enchantments.VANISHING_CURSE);
      this.tag(EnchantmentTags.TRADES_TAIGA_COMMON).add(Enchantments.BLAST_PROTECTION, Enchantments.FIRE_ASPECT, Enchantments.FLAME);
   }
}
