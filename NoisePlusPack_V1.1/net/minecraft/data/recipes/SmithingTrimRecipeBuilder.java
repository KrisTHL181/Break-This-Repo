package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
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
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

   public SmithingTrimRecipeBuilder(RecipeCategory p_267007_, Ingredient p_266712_, Ingredient p_267018_, Ingredient p_267264_, Holder<TrimPattern> p_397328_) {
      this.category = p_267007_;
      this.template = p_266712_;
      this.base = p_267018_;
      this.addition = p_267264_;
      this.pattern = p_397328_;
   }

   public static SmithingTrimRecipeBuilder smithingTrim(
      Ingredient p_266812_, Ingredient p_266843_, Ingredient p_267309_, Holder<TrimPattern> p_396610_, RecipeCategory p_267269_
   ) {
      return new SmithingTrimRecipeBuilder(p_267269_, p_266812_, p_266843_, p_267309_, p_396610_);
   }

   public SmithingTrimRecipeBuilder unlocks(String p_266882_, Criterion<?> p_297910_) {
      this.criteria.put(p_266882_, p_297910_);
      return this;
   }

   public void save(RecipeOutput p_301392_, ResourceKey<Recipe<?>> p_363621_) {
      this.ensureValid(p_363621_);
      Advancement.Builder advancement$builder = p_301392_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_363621_))
         .rewards(AdvancementRewards.Builder.recipe(p_363621_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      SmithingTrimRecipe smithingtrimrecipe = new SmithingTrimRecipe(this.template, this.base, this.addition, this.pattern);
      p_301392_.accept(
         p_363621_, smithingtrimrecipe, advancement$builder.build(p_363621_.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/"))
      );
   }

   private void ensureValid(ResourceKey<Recipe<?>> p_369707_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_369707_.identifier());
      }
   }
}
