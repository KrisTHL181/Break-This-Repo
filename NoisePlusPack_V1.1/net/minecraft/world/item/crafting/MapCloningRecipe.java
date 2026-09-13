package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MapCloningRecipe extends CustomRecipe {
   public MapCloningRecipe(CraftingBookCategory p_251985_) {
      super(p_251985_);
   }

   public boolean matches(CraftingInput p_342926_, Level p_43981_) {
      if (p_342926_.ingredientCount() < 2) {
         return false;
      }

      boolean flag = false;
      boolean flag1 = false;

      for (int i = 0; i < p_342926_.size(); i++) {
         ItemStack itemstack = p_342926_.getItem(i);
         if (!itemstack.isEmpty()) {
            if (itemstack.has(DataComponents.MAP_ID)) {
               if (flag1) {
                  return false;
               }

               flag1 = true;
            } else {
               if (!itemstack.is(Items.MAP)) {
                  return false;
               }

               flag = true;
            }
         }
      }

      return flag1 && flag;
   }

   public ItemStack assemble(CraftingInput p_344433_, HolderLookup.Provider p_334317_) {
      int i = 0;
      ItemStack itemstack = ItemStack.EMPTY;

      for (int j = 0; j < p_344433_.size(); j++) {
         ItemStack itemstack1 = p_344433_.getItem(j);
         if (!itemstack1.isEmpty()) {
            if (itemstack1.has(DataComponents.MAP_ID)) {
               if (!itemstack.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               itemstack = itemstack1;
            } else {
               if (!itemstack1.is(Items.MAP)) {
                  return ItemStack.EMPTY;
               }

               i++;
            }
         }
      }

      return !itemstack.isEmpty() && i >= 1 ? itemstack.copyWithCount(i + 1) : ItemStack.EMPTY;
   }

   @Override
   public RecipeSerializer<MapCloningRecipe> getSerializer() {
      return RecipeSerializer.MAP_CLONING;
   }
}
