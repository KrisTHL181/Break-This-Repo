package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;

public class DecoratedPotRecipe extends CustomRecipe {
   public DecoratedPotRecipe(CraftingBookCategory p_273056_) {
      super(p_273056_);
   }

   private static ItemStack back(CraftingInput p_364856_) {
      return p_364856_.getItem(1, 0);
   }

   private static ItemStack left(CraftingInput p_367598_) {
      return p_367598_.getItem(0, 1);
   }

   private static ItemStack right(CraftingInput p_365582_) {
      return p_365582_.getItem(2, 1);
   }

   private static ItemStack front(CraftingInput p_369403_) {
      return p_369403_.getItem(1, 2);
   }

   public boolean matches(CraftingInput p_342524_, Level p_272812_) {
      return p_342524_.width() == 3 && p_342524_.height() == 3 && p_342524_.ingredientCount() == 4
         ? back(p_342524_).is(ItemTags.DECORATED_POT_INGREDIENTS)
            && left(p_342524_).is(ItemTags.DECORATED_POT_INGREDIENTS)
            && right(p_342524_).is(ItemTags.DECORATED_POT_INGREDIENTS)
            && front(p_342524_).is(ItemTags.DECORATED_POT_INGREDIENTS)
         : false;
   }

   public ItemStack assemble(CraftingInput p_344747_, HolderLookup.Provider p_328495_) {
      PotDecorations potdecorations = new PotDecorations(
         back(p_344747_).getItem(), left(p_344747_).getItem(), right(p_344747_).getItem(), front(p_344747_).getItem()
      );
      return DecoratedPotBlockEntity.createDecoratedPotItem(potdecorations);
   }

   @Override
   public RecipeSerializer<DecoratedPotRecipe> getSerializer() {
      return RecipeSerializer.DECORATED_POT_RECIPE;
   }
}
