package net.minecraft.data.recipes;

import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public interface RecipeBuilder {
   Identifier ROOT_RECIPE_ADVANCEMENT = Identifier.withDefaultNamespace("recipes/root");

   RecipeBuilder unlockedBy(String var1, Criterion<?> var2);

   RecipeBuilder group(@Nullable String var1);

   Item getResult();

   void save(RecipeOutput var1, ResourceKey<Recipe<?>> var2);

   default void save(RecipeOutput p_298540_) {
      this.save(p_298540_, ResourceKey.create(Registries.RECIPE, getDefaultRecipeId(this.getResult())));
   }

   default void save(RecipeOutput p_300884_, String p_176502_) {
      Identifier identifier = getDefaultRecipeId(this.getResult());
      Identifier identifier1 = Identifier.parse(p_176502_);
      if (identifier1.equals(identifier)) {
         throw new IllegalStateException("Recipe " + p_176502_ + " should remove its 'save' argument as it is equal to default one");
      }

      this.save(p_300884_, ResourceKey.create(Registries.RECIPE, identifier1));
   }

   static Identifier getDefaultRecipeId(ItemLike p_176494_) {
      return BuiltInRegistries.ITEM.getKey(p_176494_.asItem());
   }

   static CraftingBookCategory determineBookCategory(RecipeCategory p_313042_) {
      return switch (p_313042_) {
         case BUILDING_BLOCKS -> CraftingBookCategory.BUILDING;
         case TOOLS, COMBAT -> CraftingBookCategory.EQUIPMENT;
         case REDSTONE -> CraftingBookCategory.REDSTONE;
         default -> CraftingBookCategory.MISC;
      };
   }
}
