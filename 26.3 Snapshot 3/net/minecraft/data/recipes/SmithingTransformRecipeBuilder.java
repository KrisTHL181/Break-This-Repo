package net.minecraft.data.recipes;

import java.util.Optional;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

public class SmithingTransformRecipeBuilder {
   private final Ingredient template;
   private final Ingredient base;
   private final Ingredient addition;
   private final RecipeCategory category;
   private final ItemStackTemplate result;
   private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();

   public SmithingTransformRecipeBuilder(
      final Ingredient template, final Ingredient base, final Ingredient addition, final RecipeCategory category, final ItemStackTemplate result
   ) {
      this.category = category;
      this.template = template;
      this.base = base;
      this.addition = addition;
      this.result = result;
   }

   public static SmithingTransformRecipeBuilder smithing(
      final Ingredient template, final Ingredient base, final Ingredient addition, final RecipeCategory category, final Item result
   ) {
      return new SmithingTransformRecipeBuilder(template, base, addition, category, new ItemStackTemplate(result));
   }

   public SmithingTransformRecipeBuilder unlocks(final String name, final Criterion<?> criterion) {
      this.advancementBuilder.unlockedBy(name, criterion);
      return this;
   }

   public void save(final RecipeOutput output, final String id) {
      this.save(output, ResourceKey.create(Registries.RECIPE, Identifier.parse(id)));
   }

   public void save(final RecipeOutput output, final ResourceKey<Recipe<?>> id) {
      SmithingTransformRecipe recipe = new SmithingTransformRecipe(
         new Recipe.CommonInfo(true), Optional.of(this.template), this.base, Optional.of(this.addition), this.result
      );
      output.accept(id, recipe, this.advancementBuilder.build(output, id, this.category));
   }
}
