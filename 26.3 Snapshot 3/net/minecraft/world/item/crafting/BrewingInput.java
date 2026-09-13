package net.minecraft.world.item.crafting;

import net.minecraft.world.item.ItemStack;

public record BrewingInput(ItemStack input, ItemStack reagent) implements RecipeInput {
   @Override
   public ItemStack getItem(final int index) {
      return switch (index) {
         case 0 -> this.input;
         case 1 -> this.reagent;
         default -> throw new IllegalArgumentException("No item for index " + index);
      };
   }

   @Override
   public int size() {
      return 2;
   }
}
