package net.minecraft.world.item.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;

public class ArmorDyeRecipe extends CustomRecipe {
   public ArmorDyeRecipe(CraftingBookCategory p_251949_) {
      super(p_251949_);
   }

   public boolean matches(CraftingInput p_342712_, Level p_43770_) {
      if (p_342712_.ingredientCount() < 2) {
         return false;
      }

      boolean flag = false;
      boolean flag1 = false;

      for (int i = 0; i < p_342712_.size(); i++) {
         ItemStack itemstack = p_342712_.getItem(i);
         if (!itemstack.isEmpty()) {
            if (itemstack.is(ItemTags.DYEABLE)) {
               if (flag) {
                  return false;
               }

               flag = true;
            } else {
               if (!(itemstack.getItem() instanceof DyeItem)) {
                  return false;
               }

               flag1 = true;
            }
         }
      }

      return flag1 && flag;
   }

   public ItemStack assemble(CraftingInput p_344169_, HolderLookup.Provider p_329480_) {
      List<DyeItem> list = new ArrayList<>();
      ItemStack itemstack = ItemStack.EMPTY;

      for (int i = 0; i < p_344169_.size(); i++) {
         ItemStack itemstack1 = p_344169_.getItem(i);
         if (!itemstack1.isEmpty()) {
            if (itemstack1.is(ItemTags.DYEABLE)) {
               if (!itemstack.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               itemstack = itemstack1.copy();
            } else {
               if (!(itemstack1.getItem() instanceof DyeItem dyeitem)) {
                  return ItemStack.EMPTY;
               }

               list.add(dyeitem);
            }
         }
      }

      return !itemstack.isEmpty() && !list.isEmpty() ? DyedItemColor.applyDyes(itemstack, list) : ItemStack.EMPTY;
   }

   @Override
   public RecipeSerializer<ArmorDyeRecipe> getSerializer() {
      return RecipeSerializer.ARMOR_DYE;
   }
}
