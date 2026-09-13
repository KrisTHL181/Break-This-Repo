package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class TippedArrowRecipe extends CustomRecipe {
   public TippedArrowRecipe(CraftingBookCategory p_252163_) {
      super(p_252163_);
   }

   public boolean matches(CraftingInput p_342921_, Level p_44516_) {
      if (p_342921_.width() == 3 && p_342921_.height() == 3 && p_342921_.ingredientCount() == 9) {
         for (int i = 0; i < p_342921_.height(); i++) {
            for (int j = 0; j < p_342921_.width(); j++) {
               ItemStack itemstack = p_342921_.getItem(j, i);
               if (itemstack.isEmpty()) {
                  return false;
               }

               if (j == 1 && i == 1) {
                  if (!itemstack.is(Items.LINGERING_POTION)) {
                     return false;
                  }
               } else if (!itemstack.is(Items.ARROW)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public ItemStack assemble(CraftingInput p_343937_, HolderLookup.Provider p_335423_) {
      ItemStack itemstack = p_343937_.getItem(1, 1);
      if (!itemstack.is(Items.LINGERING_POTION)) {
         return ItemStack.EMPTY;
      }

      ItemStack itemstack1 = new ItemStack(Items.TIPPED_ARROW, 8);
      itemstack1.set(DataComponents.POTION_CONTENTS, itemstack.get(DataComponents.POTION_CONTENTS));
      return itemstack1;
   }

   @Override
   public RecipeSerializer<TippedArrowRecipe> getSerializer() {
      return RecipeSerializer.TIPPED_ARROW;
   }
}
