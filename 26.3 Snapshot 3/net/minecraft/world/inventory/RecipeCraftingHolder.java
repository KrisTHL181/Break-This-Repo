package net.minecraft.world.inventory;

import java.util.Collections;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public interface RecipeCraftingHolder {
   void setRecipeUsed(final @Nullable RecipeHolder<?> recipeUsed);

   @Nullable RecipeHolder<?> getRecipeUsed();

   default void awardUsedRecipes(final Player player, final List<ItemStack> itemStacks) {
      RecipeHolder<?> recipeUsed = this.getRecipeUsed();
      if (recipeUsed != null) {
         player.triggerRecipeCrafted(recipeUsed, itemStacks);
         if (!recipeUsed.value().isSpecial()) {
            player.awardRecipes(Collections.singleton(recipeUsed));
            this.setRecipeUsed(null);
         }
      }
   }

   default boolean setRecipeUsed(final ServerPlayer player, final RecipeHolder<?> recipe) {
      if (!recipe.value().isSpecial() && player.level().getGameRules().get(GameRules.LIMITED_CRAFTING) && !player.getRecipeBook().contains(recipe.id())) {
         return false;
      }

      this.setRecipeUsed(recipe);
      return true;
   }
}
