package net.minecraft.world.item.crafting;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class RepairItemRecipe extends CustomRecipe {
   public RepairItemRecipe(CraftingBookCategory p_248679_) {
      super(p_248679_);
   }

   private static @Nullable Pair<ItemStack, ItemStack> getItemsToCombine(CraftingInput p_344890_) {
      if (p_344890_.ingredientCount() != 2) {
         return null;
      }

      ItemStack itemstack = null;

      for (int i = 0; i < p_344890_.size(); i++) {
         ItemStack itemstack1 = p_344890_.getItem(i);
         if (!itemstack1.isEmpty()) {
            if (itemstack != null) {
               return canCombine(itemstack, itemstack1) ? Pair.of(itemstack, itemstack1) : null;
            }

            itemstack = itemstack1;
         }
      }

      return null;
   }

   private static boolean canCombine(ItemStack p_335534_, ItemStack p_329259_) {
      return p_329259_.is(p_335534_.getItem())
         && p_335534_.getCount() == 1
         && p_329259_.getCount() == 1
         && p_335534_.has(DataComponents.MAX_DAMAGE)
         && p_329259_.has(DataComponents.MAX_DAMAGE)
         && p_335534_.has(DataComponents.DAMAGE)
         && p_329259_.has(DataComponents.DAMAGE);
   }

   public boolean matches(CraftingInput p_344438_, Level p_44139_) {
      return getItemsToCombine(p_344438_) != null;
   }

   public ItemStack assemble(CraftingInput p_342804_, HolderLookup.Provider p_331714_) {
      Pair<ItemStack, ItemStack> pair = getItemsToCombine(p_342804_);
      if (pair == null) {
         return ItemStack.EMPTY;
      }

      ItemStack itemstack = (ItemStack)pair.getFirst();
      ItemStack itemstack1 = (ItemStack)pair.getSecond();
      int i = Math.max(itemstack.getMaxDamage(), itemstack1.getMaxDamage());
      int j = itemstack.getMaxDamage() - itemstack.getDamageValue();
      int k = itemstack1.getMaxDamage() - itemstack1.getDamageValue();
      int l = j + k + i * 5 / 100;
      ItemStack itemstack2 = new ItemStack(itemstack.getItem());
      itemstack2.set(DataComponents.MAX_DAMAGE, i);
      itemstack2.setDamageValue(Math.max(i - l, 0));
      ItemEnchantments itemenchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemstack);
      ItemEnchantments itemenchantments1 = EnchantmentHelper.getEnchantmentsForCrafting(itemstack1);
      EnchantmentHelper.updateEnchantments(
         itemstack2,
         p_359850_ -> p_331714_.lookupOrThrow(Registries.ENCHANTMENT)
            .listElements()
            .filter(p_341586_ -> p_341586_.is(EnchantmentTags.CURSE))
            .forEach(p_341590_ -> {
               int i1 = Math.max(itemenchantments.getLevel(p_341590_), itemenchantments1.getLevel(p_341590_));
               if (i1 > 0) {
                  p_359850_.upgrade(p_341590_, i1);
               }
            })
      );
      return itemstack2;
   }

   @Override
   public RecipeSerializer<RepairItemRecipe> getSerializer() {
      return RecipeSerializer.REPAIR_ITEM;
   }
}
