package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Level;

public class FireworkStarFadeRecipe extends CustomRecipe {
   private static final Ingredient STAR_INGREDIENT = Ingredient.of(Items.FIREWORK_STAR);

   public FireworkStarFadeRecipe(CraftingBookCategory p_251846_) {
      super(p_251846_);
   }

   public boolean matches(CraftingInput p_342221_, Level p_43874_) {
      if (p_342221_.ingredientCount() < 2) {
         return false;
      }

      boolean flag = false;
      boolean flag1 = false;

      for (int i = 0; i < p_342221_.size(); i++) {
         ItemStack itemstack = p_342221_.getItem(i);
         if (!itemstack.isEmpty()) {
            if (itemstack.getItem() instanceof DyeItem) {
               flag = true;
            } else {
               if (!STAR_INGREDIENT.test(itemstack)) {
                  return false;
               }

               if (flag1) {
                  return false;
               }

               flag1 = true;
            }
         }
      }

      return flag1 && flag;
   }

   public ItemStack assemble(CraftingInput p_345501_, HolderLookup.Provider p_333582_) {
      IntList intlist = new IntArrayList();
      ItemStack itemstack = null;

      for (int i = 0; i < p_345501_.size(); i++) {
         ItemStack itemstack1 = p_345501_.getItem(i);
         if (itemstack1.getItem() instanceof DyeItem dyeitem) {
            intlist.add(dyeitem.getDyeColor().getFireworkColor());
         } else if (STAR_INGREDIENT.test(itemstack1)) {
            itemstack = itemstack1.copyWithCount(1);
         }
      }

      if (itemstack != null && !intlist.isEmpty()) {
         itemstack.update(DataComponents.FIREWORK_EXPLOSION, FireworkExplosion.DEFAULT, intlist, FireworkExplosion::withFadeColors);
         return itemstack;
      } else {
         return ItemStack.EMPTY;
      }
   }

   @Override
   public RecipeSerializer<FireworkStarFadeRecipe> getSerializer() {
      return RecipeSerializer.FIREWORK_STAR_FADE;
   }
}
