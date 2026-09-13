package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class ShieldDecorationRecipe extends CustomRecipe {
   public ShieldDecorationRecipe(CraftingBookCategory p_251065_) {
      super(p_251065_);
   }

   public boolean matches(CraftingInput p_342277_, Level p_44309_) {
      if (p_342277_.ingredientCount() != 2) {
         return false;
      }

      boolean flag = false;
      boolean flag1 = false;

      for (int i = 0; i < p_342277_.size(); i++) {
         ItemStack itemstack = p_342277_.getItem(i);
         if (!itemstack.isEmpty()) {
            if (itemstack.getItem() instanceof BannerItem) {
               if (flag1) {
                  return false;
               }

               flag1 = true;
            } else {
               if (!itemstack.is(Items.SHIELD)) {
                  return false;
               }

               if (flag) {
                  return false;
               }

               BannerPatternLayers bannerpatternlayers = itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
               if (!bannerpatternlayers.layers().isEmpty()) {
                  return false;
               }

               flag = true;
            }
         }
      }

      return flag && flag1;
   }

   public ItemStack assemble(CraftingInput p_342063_, HolderLookup.Provider p_330479_) {
      ItemStack itemstack = ItemStack.EMPTY;
      ItemStack itemstack1 = ItemStack.EMPTY;

      for (int i = 0; i < p_342063_.size(); i++) {
         ItemStack itemstack2 = p_342063_.getItem(i);
         if (!itemstack2.isEmpty()) {
            if (itemstack2.getItem() instanceof BannerItem) {
               itemstack = itemstack2;
            } else if (itemstack2.is(Items.SHIELD)) {
               itemstack1 = itemstack2.copy();
            }
         }
      }

      if (itemstack1.isEmpty()) {
         return itemstack1;
      }

      itemstack1.set(DataComponents.BANNER_PATTERNS, itemstack.get(DataComponents.BANNER_PATTERNS));
      itemstack1.set(DataComponents.BASE_COLOR, ((BannerItem)itemstack.getItem()).getColor());
      return itemstack1;
   }

   @Override
   public RecipeSerializer<ShieldDecorationRecipe> getSerializer() {
      return RecipeSerializer.SHIELD_DECORATION;
   }
}
