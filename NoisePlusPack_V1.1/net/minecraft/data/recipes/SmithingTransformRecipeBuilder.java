package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;

public class SmithingTransformRecipeBuilder {
   private final Ingredient template;
   private final Ingredient base;
   private final Ingredient addition;
   private final RecipeCategory category;
   private final Item result;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

   public SmithingTransformRecipeBuilder(Ingredient p_266973_, Ingredient p_267047_, Ingredient p_267009_, RecipeCategory p_266694_, Item p_267183_) {
      this.category = p_266694_;
      this.template = p_266973_;
      this.base = p_267047_;
      this.addition = p_267009_;
      this.result = p_267183_;
   }

   public static SmithingTransformRecipeBuilder smithing(
      Ingredient p_267071_, Ingredient p_266959_, Ingredient p_266803_, RecipeCategory p_266757_, Item p_267256_
   ) {
      return new SmithingTransformRecipeBuilder(p_267071_, p_266959_, p_266803_, p_266757_, p_267256_);
   }

   public SmithingTransformRecipeBuilder unlocks(String p_266919_, Criterion<?> p_297342_) {
      this.criteria.put(p_266919_, p_297342_);
      return this;
   }

   public void save(RecipeOutput p_300964_, String p_267035_) {
      this.save(p_300964_, ResourceKey.create(Registries.RECIPE, Identifier.parse(p_267035_)));
   }

   public void save(RecipeOutput p_301024_, ResourceKey<Recipe<?>> p_365608_) {
      this.ensureValid(p_365608_);
      Advancement.Builder advancement$builder = p_301024_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_365608_))
         .rewards(AdvancementRewards.Builder.recipe(p_365608_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      SmithingTransformRecipe smithingtransformrecipe = new SmithingTransformRecipe(
         Optional.of(this.template), this.base, Optional.of(this.addition), new TransmuteResult(this.result)
      );
      p_301024_.accept(
         p_365608_, smithingtransformrecipe, advancement$builder.build(p_365608_.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/"))
      );
   }

   private void ensureValid(ResourceKey<Recipe<?>> p_365190_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_365190_.identifier());
      }
   }
}
