package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;
import org.jspecify.annotations.Nullable;

public class TransmuteRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory category;
   private final Holder<Item> result;
   private final Ingredient input;
   private final Ingredient material;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   private @Nullable String group;

   private TransmuteRecipeBuilder(RecipeCategory p_365540_, Holder<Item> p_368366_, Ingredient p_365902_, Ingredient p_366998_) {
      this.category = p_365540_;
      this.result = p_368366_;
      this.input = p_365902_;
      this.material = p_366998_;
   }

   public static TransmuteRecipeBuilder transmute(RecipeCategory p_367390_, Ingredient p_365855_, Ingredient p_361211_, Item p_366896_) {
      return new TransmuteRecipeBuilder(p_367390_, p_366896_.builtInRegistryHolder(), p_365855_, p_361211_);
   }

   public TransmuteRecipeBuilder unlockedBy(String p_361429_, Criterion<?> p_362231_) {
      this.criteria.put(p_361429_, p_362231_);
      return this;
   }

   public TransmuteRecipeBuilder group(@Nullable String p_364491_) {
      this.group = p_364491_;
      return this;
   }

   @Override
   public Item getResult() {
      return this.result.value();
   }

   @Override
   public void save(RecipeOutput p_369743_, ResourceKey<Recipe<?>> p_369659_) {
      this.ensureValid(p_369659_);
      Advancement.Builder advancement$builder = p_369743_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_369659_))
         .rewards(AdvancementRewards.Builder.recipe(p_369659_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      TransmuteRecipe transmuterecipe = new TransmuteRecipe(
         Objects.requireNonNullElse(this.group, ""),
         RecipeBuilder.determineBookCategory(this.category),
         this.input,
         this.material,
         new TransmuteResult(this.result.value())
      );
      p_369743_.accept(
         p_369659_, transmuterecipe, advancement$builder.build(p_369659_.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/"))
      );
   }

   private void ensureValid(ResourceKey<Recipe<?>> p_368853_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_368853_.identifier());
      }
   }
}
