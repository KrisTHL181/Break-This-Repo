package net.minecraft.advancements.criterion;

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
      p_460436_ -> p_460436_.group(
            RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("enchantments").forGetter(EnchantmentPredicate::enchantments),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("levels", MinMaxBounds.Ints.ANY).forGetter(EnchantmentPredicate::level)
         )
         .apply(p_460436_, EnchantmentPredicate::new)
   );

   public EnchantmentPredicate(Holder<Enchantment> p_459026_, MinMaxBounds.Ints p_456849_) {
      this(Optional.of(HolderSet.direct(p_459026_)), p_456849_);
   }

   public EnchantmentPredicate(HolderSet<Enchantment> p_457329_, MinMaxBounds.Ints p_453010_) {
      this(Optional.of(p_457329_), p_453010_);
   }

   public boolean containedIn(ItemEnchantments p_453231_) {
      if (this.enchantments.isPresent()) {
         for (Holder<Enchantment> holder : this.enchantments.get()) {
            if (this.matchesEnchantment(p_453231_, holder)) {
               return true;
            }
         }

         return false;
      } else if (this.level != MinMaxBounds.Ints.ANY) {
         for (Entry<Holder<Enchantment>> entry : p_453231_.entrySet()) {
            if (this.level.matches(entry.getIntValue())) {
               return true;
            }
         }

         return false;
      } else {
         return !p_453231_.isEmpty();
      }
   }

   private boolean matchesEnchantment(ItemEnchantments p_452422_, Holder<Enchantment> p_455764_) {
      int i = p_452422_.getLevel(p_455764_);
      if (i == 0) {
         return false;
      } else {
         return this.level == MinMaxBounds.Ints.ANY ? true : this.level.matches(i);
      }
   }
}
