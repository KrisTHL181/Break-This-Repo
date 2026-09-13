package net.minecraft.data.tags;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

public abstract class EnchantmentTagsProvider extends TagsProvider<Enchantment> {
   public EnchantmentTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, Registries.ENCHANTMENT, lookupProvider);
   }

   protected void tooltipOrder(final HolderLookup.Provider registries, final ResourceKey<Enchantment>... order) {
      this.tag(EnchantmentTags.TOOLTIP_ORDER).add(order);
      Set<ResourceKey<Enchantment>> set = Set.of(order);
      List<String> unlisted = registries.lookupOrThrow(Registries.ENCHANTMENT)
         .listElements()
         .filter(e -> !set.contains(e.unwrapKey().get()))
         .map(Holder::getRegisteredName)
         .collect(Collectors.toList());
      if (!unlisted.isEmpty()) {
         throw new IllegalStateException("Not all enchantments were registered for tooltip ordering. Missing: " + String.join(", ", unlisted));
      }
   }
}
