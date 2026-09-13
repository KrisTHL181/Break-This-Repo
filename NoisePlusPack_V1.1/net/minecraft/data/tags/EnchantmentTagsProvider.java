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

public abstract class EnchantmentTagsProvider extends KeyTagProvider<Enchantment> {
   public EnchantmentTagsProvider(PackOutput p_332794_, CompletableFuture<HolderLookup.Provider> p_331070_) {
      super(p_332794_, Registries.ENCHANTMENT, p_331070_);
   }

   protected void tooltipOrder(HolderLookup.Provider p_335292_, ResourceKey<Enchantment>... p_343612_) {
      this.tag(EnchantmentTags.TOOLTIP_ORDER).add(p_343612_);
      Set<ResourceKey<Enchantment>> set = Set.of(p_343612_);
      List<String> list = p_335292_.lookupOrThrow(Registries.ENCHANTMENT)
         .listElements()
         .filter(p_341081_ -> !set.contains(p_341081_.unwrapKey().get()))
         .map(Holder::getRegisteredName)
         .collect(Collectors.toList());
      if (!list.isEmpty()) {
         throw new IllegalStateException("Not all enchantments were registered for tooltip ordering. Missing: " + String.join(", ", list));
      }
   }
}
