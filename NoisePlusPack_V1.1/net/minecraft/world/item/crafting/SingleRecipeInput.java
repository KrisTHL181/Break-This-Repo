package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;

public record SingleRecipeInput(ItemStack item) implements RecipeInput {
   @Override
   public ItemStack getItem(int p_343340_) {
      if (p_343340_ != 0) {
         throw new IllegalArgumentException("No item for index " + p_343340_);
      } else {
         return this.item;
      }
   }

   @Override
   public int size() {
      return 1;
   }
}
