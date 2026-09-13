package net.minecraft.stats;

import net.minecraft.world.inventory.RecipeBookType;

public class RecipeBook {
   protected final RecipeBookSettings bookSettings = new RecipeBookSettings();

   public boolean isOpen(RecipeBookType p_12692_) {
      return this.bookSettings.isOpen(p_12692_);
   }

   public void setOpen(RecipeBookType p_12694_, boolean p_12695_) {
      this.bookSettings.setOpen(p_12694_, p_12695_);
   }

   public boolean isFiltering(RecipeBookType p_12705_) {
      return this.bookSettings.isFiltering(p_12705_);
   }

   public void setFiltering(RecipeBookType p_12707_, boolean p_12708_) {
      this.bookSettings.setFiltering(p_12707_, p_12708_);
   }

   public void setBookSettings(RecipeBookSettings p_12688_) {
      this.bookSettings.replaceFrom(p_12688_);
   }

   public RecipeBookSettings getBookSettings() {
      return this.bookSettings;
   }

   public void setBookSetting(RecipeBookType p_12697_, boolean p_12698_, boolean p_12699_) {
      this.bookSettings.setOpen(p_12697_, p_12698_);
      this.bookSettings.setFiltering(p_12697_, p_12699_);
   }
}
