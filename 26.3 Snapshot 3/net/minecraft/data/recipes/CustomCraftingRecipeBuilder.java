package net.minecraft.data.recipes;

import java.util.function.BiFunction;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

public class CustomCraftingRecipeBuilder {
   private final RecipeCategory category;
   private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
   private @Nullable String group;
   private final CustomCraftingRecipeBuilder.Factory factory;

   public CustomCraftingRecipeBuilder(final RecipeCategory category, final CustomCraftingRecipeBuilder.Factory factory) {
      this.category = category;
      this.factory = factory;
   }

   public static CustomCraftingRecipeBuilder customCrafting(final RecipeCategory category, final CustomCraftingRecipeBuilder.Factory factory) {
      return new CustomCraftingRecipeBuilder(category, factory);
   }

   public CustomCraftingRecipeBuilder unlockedBy(final String name, final Criterion<?> criterion) {
      this.advancementBuilder.unlockedBy(name, criterion);
      return this;
   }

   public CustomCraftingRecipeBuilder group(final @Nullable String group) {
      this.group = group;
      return this;
   }

   public void save(final RecipeOutput output, final String name) {
      this.save(output, ResourceKey.create(Registries.RECIPE, Identifier.parse(name)));
   }

   public void save(final RecipeOutput output, final ResourceKey<Recipe<?>> id) {
      Recipe.CommonInfo commonInfo = RecipeBuilder.createCraftingCommonInfo(true);
      CraftingRecipe.CraftingBookInfo bookInfo = RecipeBuilder.createCraftingBookInfo(this.category, this.group);
      Recipe<?> recipe = this.factory.apply(commonInfo, bookInfo);
      output.accept(id, recipe, this.advancementBuilder.build(output, id, this.category));
   }

   @FunctionalInterface
   public interface Factory extends BiFunction<Recipe.CommonInfo, CraftingRecipe.CraftingBookInfo, Recipe<?>> {
   }
}
