package net.minecraft.advancements.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record EnchantmentPredicate(Optional<HolderSet<Enchantment>> enchantments, MinMaxBounds.Ints level) {
   public static final Codec<EnchantmentPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("enchantments").forGetter(EnchantmentPredicate::enchantments),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("levels", MinMaxBounds.Ints.ANY).forGetter(EnchantmentPredicate::level)
         )
         .apply(i, EnchantmentPredicate::new)
   );

   public EnchantmentPredicate(final Holder<Enchantment> enchantment, final MinMaxBounds.Ints level) {
      this(Optional.of(HolderSet.direct(enchantment)), level);
   }

   public EnchantmentPredicate(final HolderSet<Enchantment> enchantments, final MinMaxBounds.Ints level) {
      this(Optional.of(enchantments), level);
   }

   public boolean containedIn(final ItemEnchantments itemEnchantments) {
      if (this.enchantments.isPresent()) {
         for (Holder<Enchantment> enchantment : this.enchantments.get()) {
            if (this.matchesEnchantment(itemEnchantments, enchantment)) {
               return true;
            }
         }

         return false;
      } else if (this.level != MinMaxBounds.Ints.ANY) {
         for (Entry<Holder<Enchantment>> entry : itemEnchantments.entrySet()) {
            if (this.level.matches(entry.getIntValue())) {
               return true;
            }
         }

         return false;
      } else {
         return !itemEnchantments.isEmpty();
      }
   }

   private boolean matchesEnchantment(final ItemEnchantments itemEnchantments, final Holder<Enchantment> enchantment) {
      int level = itemEnchantments.getLevel(enchantment);
      if (level == 0) {
         return false;
      } else {
         return this.level == MinMaxBounds.Ints.ANY ? true : this.level.matches(level);
      }
   }
}
