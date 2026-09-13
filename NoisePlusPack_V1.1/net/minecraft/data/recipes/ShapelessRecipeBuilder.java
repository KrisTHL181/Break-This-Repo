package net.minecraft.data.recipes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class ShapelessRecipeBuilder implements RecipeBuilder {
   private final HolderGetter<Item> items;
   private final RecipeCategory category;
   private final ItemStack result;
   private final List<Ingredient> ingredients = new ArrayList<>();
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   private @Nullable String group;

   private ShapelessRecipeBuilder(HolderGetter<Item> p_362903_, RecipeCategory p_250837_, ItemStack p_362773_) {
      this.items = p_362903_;
      this.category = p_250837_;
      this.result = p_362773_;
   }

   public static ShapelessRecipeBuilder shapeless(HolderGetter<Item> p_365884_, RecipeCategory p_250714_, ItemStack p_369160_) {
      return new ShapelessRecipeBuilder(p_365884_, p_250714_, p_369160_);
   }

   public static ShapelessRecipeBuilder shapeless(HolderGetter<Item> p_362305_, RecipeCategory p_368322_, ItemLike p_366832_) {
      return shapeless(p_362305_, p_368322_, p_366832_, 1);
   }

   public static ShapelessRecipeBuilder shapeless(HolderGetter<Item> p_361011_, RecipeCategory p_252339_, ItemLike p_250836_, int p_249928_) {
      return new ShapelessRecipeBuilder(p_361011_, p_252339_, p_250836_.asItem().getDefaultInstance().copyWithCount(p_249928_));
   }

   public ShapelessRecipeBuilder requires(TagKey<Item> p_206420_) {
      return this.requires(Ingredient.of(this.items.getOrThrow(p_206420_)));
   }

   public ShapelessRecipeBuilder requires(ItemLike p_126210_) {
      return this.requires(p_126210_, 1);
   }

   public ShapelessRecipeBuilder requires(ItemLike p_126212_, int p_126213_) {
      for (int i = 0; i < p_126213_; i++) {
         this.requires(Ingredient.of(p_126212_));
      }

      return this;
   }

   public ShapelessRecipeBuilder requires(Ingredient p_126185_) {
      return this.requires(p_126185_, 1);
   }

   public ShapelessRecipeBuilder requires(Ingredient p_126187_, int p_126188_) {
      for (int i = 0; i < p_126188_; i++) {
         this.ingredients.add(p_126187_);
      }

      return this;
   }

   public ShapelessRecipeBuilder unlockedBy(String p_176781_, Criterion<?> p_300919_) {
      this.criteria.put(p_176781_, p_300919_);
      return this;
   }

   public ShapelessRecipeBuilder group(@Nullable String p_126195_) {
      this.group = p_126195_;
      return this;
   }

   @Override
   public Item getResult() {
      return this.result.getItem();
   }

   @Override
   public void save(RecipeOutput p_300117_, ResourceKey<Recipe<?>> p_364714_) {
      this.ensureValid(p_364714_);
      Advancement.Builder advancement$builder = p_300117_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_364714_))
         .rewards(AdvancementRewards.Builder.recipe(p_364714_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      ShapelessRecipe shapelessrecipe = new ShapelessRecipe(
         Objects.requireNonNullElse(this.group, ""), RecipeBuilder.determineBookCategory(this.category), this.result, this.ingredients
      );
      p_300117_.accept(
         p_364714_, shapelessrecipe, advancement$builder.build(p_364714_.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/"))
      );
   }

   private void ensureValid(ResourceKey<Recipe<?>> p_368339_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_368339_.identifier());
      }
   }
}
