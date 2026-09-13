package net.minecraft.world.item.crafting;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

public class StonecutterRecipe extends SingleItemRecipe {
   public StonecutterRecipe(String p_44479_, Ingredient p_44480_, ItemStack p_301701_) {
      super(p_44479_, p_44480_, p_301701_);
   }

   @Override
   public RecipeType<StonecutterRecipe> getType() {
      return RecipeType.STONECUTTING;
   }

   @Override
   public RecipeSerializer<StonecutterRecipe> getSerializer() {
      return RecipeSerializer.STONECUTTER;
   }

   @Override
   public List<RecipeDisplay> display() {
      return List.of(new StonecutterRecipeDisplay(this.input().display(), this.resultDisplay(), new SlotDisplay.ItemSlotDisplay(Items.STONECUTTER)));
   }

   public SlotDisplay resultDisplay() {
      return new SlotDisplay.ItemStackSlotDisplay(this.result());
   }

   @Override
   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.STONECUTTER;
   }
}
