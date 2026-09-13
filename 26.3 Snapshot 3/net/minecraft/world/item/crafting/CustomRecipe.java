package net.minecraft.world.item.crafting;

public abstract class CustomRecipe implements CraftingRecipe {
   @Override
   public boolean isSpecial() {
      return true;
   }

   @Override
   public boolean showNotification() {
      return false;
   }

   @Override
   public String group() {
      return "";
   }

   @Override
   public CraftingBookCategory category() {
      return CraftingBookCategory.MISC;
   }

   @Override
   public PlacementInfo placementInfo() {
      return PlacementInfo.NOT_PLACEABLE;
   }

   @Override
   public abstract RecipeSerializer<? extends CustomRecipe> getSerializer();
}
