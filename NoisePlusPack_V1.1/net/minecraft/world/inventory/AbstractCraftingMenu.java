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

   public AbstractCraftingMenu(MenuType<?> p_366631_, int p_364244_, int p_366819_, int p_362264_) {
      super(p_366631_, p_364244_);
      this.width = p_366819_;
      this.height = p_362264_;
      this.craftSlots = new TransientCraftingContainer(this, p_366819_, p_362264_);
   }

   protected Slot addResultSlot(Player p_368097_, int p_367668_, int p_363802_) {
      return this.addSlot(new ResultSlot(p_368097_, this.craftSlots, this.resultSlots, 0, p_367668_, p_363802_));
   }

   protected void addCraftingGridSlots(int p_361557_, int p_366601_) {
      for (int i = 0; i < this.width; i++) {
         for (int j = 0; j < this.height; j++) {
            this.addSlot(new Slot(this.craftSlots, j + i * this.width, p_361557_ + j * 18, p_366601_ + i * 18));
         }
      }
   }

   @Override
   public RecipeBookMenu.PostPlaceAction handlePlacement(
      boolean p_367003_, boolean p_360772_, RecipeHolder<?> p_361387_, ServerLevel p_365408_, Inventory p_368520_
   ) {
      RecipeHolder<CraftingRecipe> recipeholder = (RecipeHolder<CraftingRecipe>)p_361387_;
      this.beginPlacingRecipe();

      RecipeBookMenu.PostPlaceAction recipebookmenu$postplaceaction;
      try {
         List<Slot> list = this.getInputGridSlots();
         recipebookmenu$postplaceaction = ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<CraftingRecipe>() {
            @Override
            public void fillCraftSlotsStackedContents(StackedItemContents p_367296_) {
               AbstractCraftingMenu.this.fillCraftSlotsStackedContents(p_367296_);
            }

            @Override
            public void clearCraftingContent() {
               AbstractCraftingMenu.this.resultSlots.clearContent();
               AbstractCraftingMenu.this.craftSlots.clearContent();
            }

            @Override
            public boolean recipeMatches(RecipeHolder<CraftingRecipe> p_368304_) {
               return p_368304_.value().matches(AbstractCraftingMenu.this.craftSlots.asCraftInput(), AbstractCraftingMenu.this.owner().level());
            }
         }, this.width, this.height, list, list, p_368520_, recipeholder, p_367003_, p_360772_);
      } finally {
         this.finishPlacingRecipe(p_365408_, (RecipeHolder<CraftingRecipe>)p_361387_);
      }

      return recipebookmenu$postplaceaction;
   }

   protected void beginPlacingRecipe() {
   }

   protected void finishPlacingRecipe(ServerLevel p_365958_, RecipeHolder<CraftingRecipe> p_364067_) {
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
   public void fillCraftSlotsStackedContents(StackedItemContents p_365758_) {
      this.craftSlots.fillStackedContents(p_365758_);
   }
}
