package net.minecraft.stats;

import net.minecraft.world.inventory.RecipeBookType;

public class RecipeBook {
   protected final RecipeBookSettings bookSettings = new RecipeBookSettings();

   public boolean isOpen(final RecipeBookType recipeBookType) {
      return this.bookSettings.isOpen(recipeBookType);
   }

   public void setOpen(final RecipeBookType recipeBookType, final boolean open) {
      this.bookSettings.setOpen(recipeBookType, open);
   }

   public boolean isFiltering(final RecipeBookType type) {
      return this.bookSettings.isFiltering(type);
   }

   public void setFiltering(final RecipeBookType type, final boolean filtering) {
      this.bookSettings.setFiltering(type, filtering);
   }

   public void setBookSettings(final RecipeBookSettings settings) {
      this.bookSettings.replaceFrom(settings);
   }

   public RecipeBookSettings getBookSettings() {
      return this.bookSettings;
   }

   public void setBookSetting(final RecipeBookType bookType, final boolean open, final boolean filtering) {
      this.bookSettings.setOpen(bookType, open);
      this.bookSettings.setFiltering(bookType, filtering);
   }
}
