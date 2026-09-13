package net.minecraft.data.recipes;

import java.util.Objects;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class SimpleCookingRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory craftingCategory;
   private final CookingBookCategory cookingCategory;
   private final ItemStackTemplate result;
   private final Ingredient ingredient;
   private final float experience;
   private final int cookingTime;
   private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
   private @Nullable String group;
   private final AbstractCookingRecipe.Factory<?> factory;

   private SimpleCookingRecipeBuilder(
      final RecipeCategory craftingCategory,
      final CookingBookCategory cookingCategory,
      final ItemStackTemplate result,
      final Ingredient ingredient,
      final float experience,
      final int cookingTime,
      final AbstractCookingRecipe.Factory<?> factory
   ) {
      this.craftingCategory = craftingCategory;
      this.cookingCategory = cookingCategory;
      this.result = result;
      this.ingredient = ingredient;
      this.experience = experience;
      this.cookingTime = cookingTime;
      this.factory = factory;
   }

   private SimpleCookingRecipeBuilder(
      final RecipeCategory craftingCategory,
      final CookingBookCategory cookingCategory,
      final ItemLike result,
      final Ingredient ingredient,
      final float experience,
      final int cookingTime,
      final AbstractCookingRecipe.Factory<?> factory
   ) {
      this(craftingCategory, cookingCategory, new ItemStackTemplate(result.asItem()), ingredient, experience, cookingTime, factory);
   }

   public static <T extends AbstractCookingRecipe> SimpleCookingRecipeBuilder generic(
      final Ingredient ingredient,
      final RecipeCategory craftingCategory,
      final CookingBookCategory cookingCategory,
      final ItemLike result,
      final float experience,
      final int cookingTime,
      final AbstractCookingRecipe.Factory<T> factory
   ) {
      return new SimpleCookingRecipeBuilder(craftingCategory, cookingCategory, result, ingredient, experience, cookingTime, factory);
   }

   public static SimpleCookingRecipeBuilder campfireCooking(
      final Ingredient ingredient, final RecipeCategory craftingCategory, final ItemLike result, final float experience, final int cookingTime
   ) {
      return new SimpleCookingRecipeBuilder(craftingCategory, CookingBookCategory.FOOD, result, ingredient, experience, cookingTime, CampfireCookingRecipe::new);
   }

   public static SimpleCookingRecipeBuilder blasting(
      final Ingredient ingredient,
      final RecipeCategory craftingCategory,
      final CookingBookCategory cookingCategory,
      final ItemLike result,
      final float experience,
      final int cookingTime
   ) {
      return new SimpleCookingRecipeBuilder(craftingCategory, cookingCategory, result, ingredient, experience, cookingTime, BlastingRecipe::new);
   }

   public static SimpleCookingRecipeBuilder smelting(
      final Ingredient ingredient,
      final RecipeCategory craftingCategory,
      final CookingBookCategory cookingCategory,
      final ItemLike result,
      final float experience,
      final int cookingTime
   ) {
      return new SimpleCookingRecipeBuilder(craftingCategory, cookingCategory, result, ingredient, experience, cookingTime, SmeltingRecipe::new);
   }

   public static SimpleCookingRecipeBuilder smoking(
      final Ingredient ingredient, final RecipeCategory craftingCategory, final ItemLike result, final float experience, final int cookingTime
   ) {
      return new SimpleCookingRecipeBuilder(craftingCategory, CookingBookCategory.FOOD, result, ingredient, experience, cookingTime, SmokingRecipe::new);
   }

   public SimpleCookingRecipeBuilder unlockedBy(final String name, final Criterion<?> criterion) {
      this.advancementBuilder.unlockedBy(name, criterion);
      return this;
   }

   public SimpleCookingRecipeBuilder group(final @Nullable String group) {
      this.group = group;
      return this;
   }

   @Override
   public ResourceKey<Recipe<?>> defaultId() {
      return RecipeBuilder.getDefaultRecipeId(this.result);
   }

   @Override
   public void save(final RecipeOutput output, final ResourceKey<Recipe<?>> id) {
      AbstractCookingRecipe recipe = this.factory
         .create(
            RecipeBuilder.createCraftingCommonInfo(true),
            new AbstractCookingRecipe.CookingBookInfo(this.cookingCategory, Objects.requireNonNullElse(this.group, "")),
            this.ingredient,
            this.result,
            this.experience,
            this.cookingTime
         );
      output.accept(id, recipe, this.advancementBuilder.build(output, id, this.craftingCategory));
   }
}
