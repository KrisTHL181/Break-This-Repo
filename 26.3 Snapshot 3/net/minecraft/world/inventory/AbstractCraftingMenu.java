package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public abstract class AbstractCraftingMenu extends RecipeBookMenu {
   private final int width;
   private final int height;
   protected final CraftingContainer craftSlots;
   protected final ResultContainer resultSlots = new ResultContainer();

   public AbstractCraftingMenu(final MenuType<?> menuType, final int containerId, final int width, final int height) {
      super(menuType, containerId);
      this.width = width;
      this.height = height;
      this.craftSlots = new TransientCraftingContainer(this, width, height);
   }

   protected Slot addResultSlot(final Player player, final int x, final int y) {
      return this.addSlot(new ResultSlot(player, this.craftSlots, this.resultSlots, 0, x, y));
   }

   protected void addCraftingGridSlots(final int left, final int top) {
      for (int y = 0; y < this.width; y++) {
         for (int x = 0; x < this.height; x++) {
            this.addSlot(new Slot(this.craftSlots, x + y * this.width, left + x * 18, top + y * 18));
         }
      }
   }

   @Override
   public RecipeBookMenu.PostPlaceAction handlePlacement(
      final boolean useMaxItems, final boolean allowDroppingItemsToClear, final RecipeHolder<?> recipe, final ServerLevel level, final Inventory inventory
   ) {
      RecipeHolder<CraftingRecipe> typedRecipe = (RecipeHolder<CraftingRecipe>)recipe;
      this.beginPlacingRecipe();

      try {
         List<Slot> inputSlots = this.getInputGridSlots();
         return ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<CraftingRecipe>() {
            @Override
            public void fillCraftSlotsStackedContents(final StackedItemContents stackedContents) {
               AbstractCraftingMenu.this.fillCraftSlotsStackedContents(stackedContents);
            }

            @Override
            public void clearCraftingContent() {
               AbstractCraftingMenu.this.resultSlots.clearContent();
               AbstractCraftingMenu.this.craftSlots.clearContent();
            }

            @Override
            public boolean recipeMatches(final RecipeHolder<CraftingRecipe> recipe) {
               return recipe.value().matches(AbstractCraftingMenu.this.craftSlots.asCraftInput(), AbstractCraftingMenu.this.owner().level());
            }
         }, this.width, this.height, inputSlots, inputSlots, inventory, typedRecipe, useMaxItems, allowDroppingItemsToClear);
      } finally {
         this.finishPlacingRecipe(level, typedRecipe);
      }
   }

   protected void beginPlacingRecipe() {
   }

   protected void finishPlacingRecipe(final ServerLevel level, final RecipeHolder<CraftingRecipe> recipe) {
   }

   public abstract Slot getResultSlot();

   public abstract List<Slot> getInputGridSlots();

   public int getGridWidth() {
      return this.width;
   }

   public int getGridHeight() {
      return this.height;
   }

   protected abstract Player owner();

   @Override
   public void fillCraftSlotsStackedContents(final StackedItemContents stackedContents) {
      this.craftSlots.fillStackedContents(stackedContents);
   }
}
