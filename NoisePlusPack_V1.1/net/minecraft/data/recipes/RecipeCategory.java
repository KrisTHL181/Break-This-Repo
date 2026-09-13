package net.minecraft.data.recipes;

public enum RecipeCategory {
   BUILDING_BLOCKS("building_blocks"),
   DECORATIONS("decorations"),
   REDSTONE("redstone"),
   TRANSPORTATION("transportation"),
   TOOLS("tools"),
   COMBAT("combat"),
   FOOD("food"),
   BREWING("brewing"),
   MISC("misc");

   private final String recipeFolderName;

   RecipeCategory(final String p_251010_) {
      this.recipeFolderName = p_251010_;
   }

   public String getFolderName() {
      return this.recipeFolderName;
   }
}
