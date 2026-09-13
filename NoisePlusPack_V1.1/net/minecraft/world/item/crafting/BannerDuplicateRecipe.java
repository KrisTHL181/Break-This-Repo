package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class BannerDuplicateRecipe extends CustomRecipe {
   public BannerDuplicateRecipe(CraftingBookCategory p_250373_) {
      super(p_250373_);
   }

   public boolean matches(CraftingInput p_344586_, Level p_43786_) {
      if (p_344586_.ingredientCount() != 2) {
         return false;
      }

      DyeColor dyecolor = null;
      boolean flag = false;
      boolean flag1 = false;

      for (int i = 0; i < p_344586_.size(); i++) {
         ItemStack itemstack = p_344586_.getItem(i);
         if (!itemstack.isEmpty()) {
            if (!(itemstack.getItem() instanceof BannerItem banneritem)) {
               return false;
            }

            if (dyecolor == null) {
               dyecolor = banneritem.getColor();
            } else if (dyecolor != banneritem.getColor()) {
               return false;
            }

            int j = itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size();
            if (j > 6) {
               return false;
            }

            if (j > 0) {
               if (flag1) {
                  return false;
               }

               flag1 = true;
            } else {
               if (flag) {
                  return false;
               }

               flag = true;
            }
         }
      }

      return flag1 && flag;
   }

   public ItemStack assemble(CraftingInput p_344878_, HolderLookup.Provider p_333234_) {
      for (int i = 0; i < p_344878_.size(); i++) {
         ItemStack itemstack = p_344878_.getItem(i);
         if (!itemstack.isEmpty()) {
            int j = itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size();
            if (j > 0 && j <= 6) {
               return itemstack.copyWithCount(1);
            }
         }
      }

      return ItemStack.EMPTY;
   }

   @Override
   public NonNullList<ItemStack> getRemainingItems(CraftingInput p_342084_) {
      NonNullList<ItemStack> nonnulllist = NonNullList.withSize(p_342084_.size(), ItemStack.EMPTY);

      for (int i = 0; i < nonnulllist.size(); i++) {
         ItemStack itemstack = p_342084_.getItem(i);
         if (!itemstack.isEmpty()) {
            ItemStack itemstack1 = itemstack.getItem().getCraftingRemainder();
            if (!itemstack1.isEmpty()) {
               nonnulllist.set(i, itemstack1);
            } else if (!itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().isEmpty()) {
               nonnulllist.set(i, itemstack.copyWithCount(1));
            }
         }
      }

      return nonnulllist;
   }

   @Override
   public RecipeSerializer<BannerDuplicateRecipe> getSerializer() {
      return RecipeSerializer.BANNER_DUPLICATE;
   }
}
