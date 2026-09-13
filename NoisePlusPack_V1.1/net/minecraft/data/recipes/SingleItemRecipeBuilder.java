package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class SingleItemRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory category;
   private final Item result;
   private final Ingredient ingredient;
   private final int count;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   private @Nullable String group;
   private final SingleItemRecipe.Factory<?> factory;

   public SingleItemRecipeBuilder(RecipeCategory p_251425_, SingleItemRecipe.Factory<?> p_311287_, Ingredient p_251221_, ItemLike p_251302_, int p_250964_) {
      this.category = p_251425_;
      this.factory = p_311287_;
      this.result = p_251302_.asItem();
      this.ingredient = p_251221_;
      this.count = p_250964_;
   }

   public static SingleItemRecipeBuilder stonecutting(Ingredient p_248596_, RecipeCategory p_250503_, ItemLike p_250269_) {
      return new SingleItemRecipeBuilder(p_250503_, StonecutterRecipe::new, p_248596_, p_250269_, 1);
   }

   public static SingleItemRecipeBuilder stonecutting(Ingredient p_251375_, RecipeCategory p_248984_, ItemLike p_250105_, int p_249506_) {
      return new SingleItemRecipeBuilder(p_248984_, StonecutterRecipe::new, p_251375_, p_250105_, p_249506_);
   }

   public SingleItemRecipeBuilder unlockedBy(String p_176810_, Criterion<?> p_298188_) {
      this.criteria.put(p_176810_, p_298188_);
      return this;
   }

   public SingleItemRecipeBuilder group(@Nullable String p_176808_) {
      this.group = p_176808_;
      return this;
   }

   @Override
   public Item getResult() {
      return this.result;
   }

   @Override
   public void save(RecipeOutput p_298439_, ResourceKey<Recipe<?>> p_362425_) {
      this.ensureValid(p_362425_);
      Advancement.Builder advancement$builder = p_298439_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_362425_))
         .rewards(AdvancementRewards.Builder.recipe(p_362425_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      SingleItemRecipe singleitemrecipe = this.factory
         .create(Objects.requireNonNullElse(this.group, ""), this.ingredient, new ItemStack(this.result, this.count));
      p_298439_.accept(
         p_362425_, singleitemrecipe, advancement$builder.build(p_362425_.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/"))
      );
   }

   private void ensureValid(ResourceKey<Recipe<?>> p_364816_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_364816_.identifier());
      }
   }
}
