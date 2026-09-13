package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Function;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public abstract class EnchantmentsPredicate implements SingleComponentItemPredicate<ItemEnchantments> {
   private final List<EnchantmentPredicate> enchantments;

   protected EnchantmentsPredicate(List<EnchantmentPredicate> p_391472_) {
      this.enchantments = p_391472_;
   }

   public static <T extends EnchantmentsPredicate> Codec<T> codec(Function<List<EnchantmentPredicate>, T> p_393092_) {
      return EnchantmentPredicate.CODEC.listOf().xmap(p_393092_, EnchantmentsPredicate::enchantments);
   }

   protected List<EnchantmentPredicate> enchantments() {
      return this.enchantments;
   }

   public boolean matches(ItemEnchantments p_395653_) {
      for (EnchantmentPredicate enchantmentpredicate : this.enchantments) {
         if (!enchantmentpredicate.containedIn(p_395653_)) {
            return false;
         }
      }

      return true;
   }

   public static EnchantmentsPredicate.Enchantments enchantments(List<EnchantmentPredicate> p_396443_) {
      return new EnchantmentsPredicate.Enchantments(p_396443_);
   }

   public static EnchantmentsPredicate.StoredEnchantments storedEnchantments(List<EnchantmentPredicate> p_392960_) {
      return new EnchantmentsPredicate.StoredEnchantments(p_392960_);
   }

   public static class Enchantments extends EnchantmentsPredicate {
      public static final Codec<EnchantmentsPredicate.Enchantments> CODEC = codec(EnchantmentsPredicate.Enchantments::new);

      protected Enchantments(List<EnchantmentPredicate> p_391341_) {
         super(p_391341_);
      }

      @Override
      public DataComponentType<ItemEnchantments> componentType() {
         return DataComponents.ENCHANTMENTS;
      }
   }

   public static class StoredEnchantments extends EnchantmentsPredicate {
      public static final Codec<EnchantmentsPredicate.StoredEnchantments> CODEC = codec(EnchantmentsPredicate.StoredEnchantments::new);

      protected StoredEnchantments(List<EnchantmentPredicate> p_393148_) {
         super(p_393148_);
      }

      @Override
      public DataComponentType<ItemEnchantments> componentType() {
         return DataComponents.STORED_ENCHANTMENTS;
      }
   }
}
