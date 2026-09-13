package net.minecraft.data.recipes;

import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class SmithingTrimRecipeBuilder {
   private final RecipeCategory category;
   private final Ingredient template;
   private final Ingredient base;
   private final Ingredient addition;
   private final Holder<TrimPattern> pattern;
   private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();

   public SmithingTrimRecipeBuilder(
      final RecipeCategory category, final Ingredient template, final Ingredient base, final Ingredient addition, final Holder<TrimPattern> pattern
   ) {
      this.category = category;
      this.template = template;
      this.base = base;
      this.addition = addition;
      this.pattern = pattern;
   }

   public static SmithingTrimRecipeBuilder smithingTrim(
      final Ingredient template, final Ingredient base, final Ingredient addition, final Holder<TrimPattern> pattern, final RecipeCategory category
   ) {
      return new SmithingTrimRecipeBuilder(category, template, base, addition, pattern);
   }

   public SmithingTrimRecipeBuilder unlocks(final String name, final Criterion<?> criterion) {
      this.advancementBuilder.unlockedBy(name, criterion);
      return this;
   }

   public void save(final RecipeOutput output, final ResourceKey<Recipe<?>> id) {
      SmithingTrimRecipe recipe = new SmithingTrimRecipe(new Recipe.CommonInfo(true), this.template, this.base, this.addition, this.pattern);
      output.accept(id, recipe, this.advancementBuilder.build(output, id, this.category));
   }
}
