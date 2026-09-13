package net.minecraft.world.item.crafting;

import org.jspecify.annotations.Nullable;

public abstract class SimpleSmithingRecipe implements SmithingRecipe {
   protected final Recipe.CommonInfo commonInfo;
   private @Nullable PlacementInfo placementInfo;

   protected SimpleSmithingRecipe(final Recipe.CommonInfo commonInfo) {
      this.commonInfo = commonInfo;
   }

   @Override
   public abstract RecipeSerializer<? extends SimpleSmithingRecipe> getSerializer();

   @Override
   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = this.createPlacementInfo();
      }

      return this.placementInfo;
   }

   protected abstract PlacementInfo createPlacementInfo();

   @Override
   public String group() {
      return "";
   }

   @Override
   public final boolean showNotification() {
      return this.commonInfo.showNotification();
   }
}
