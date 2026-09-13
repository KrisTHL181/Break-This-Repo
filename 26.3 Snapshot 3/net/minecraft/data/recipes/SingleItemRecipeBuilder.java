package net.minecraft.data.recipes;

import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class SingleItemRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory category;
   private final ItemStackTemplate result;
   private final Ingredient ingredient;
   private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
   private final SingleItemRecipe.Factory<?> factory;

   private SingleItemRecipeBuilder(
      final RecipeCategory category, final SingleItemRecipe.Factory<?> factory, final Ingredient ingredient, final ItemStackTemplate result
   ) {
      this.category = category;
      this.result = result;
      this.ingredient = ingredient;
      this.factory = factory;
   }

   public SingleItemRecipeBuilder(
      final RecipeCategory category, final SingleItemRecipe.Factory<?> factory, final Ingredient ingredient, final ItemLike result, final int count
   ) {
      this(category, factory, ingredient, new ItemStackTemplate(result.asItem(), count));
   }

   public static SingleItemRecipeBuilder stonecutting(final Ingredient ingredient, final RecipeCategory category, final ItemLike result, final int count) {
      return new SingleItemRecipeBuilder(category, StonecutterRecipe::new, ingredient, result, count);
   }

   public SingleItemRecipeBuilder unlockedBy(final String name, final Criterion<?> criterion) {
      this.advancementBuilder.unlockedBy(name, criterion);
      return this;
   }

   public SingleItemRecipeBuilder group(final @Nullable String group) {
      return this;
   }

   @Override
   public ResourceKey<Recipe<?>> defaultId() {
      return RecipeBuilder.getDefaultRecipeId(this.result);
   }

   @Override
   public void save(final RecipeOutput output, final ResourceKey<Recipe<?>> id) {
      SingleItemRecipe recipe = this.factory.create(new Recipe.CommonInfo(true), this.ingredient, this.result);
      output.accept(id, recipe, this.advancementBuilder.build(output, id, this.category));
   }
}
