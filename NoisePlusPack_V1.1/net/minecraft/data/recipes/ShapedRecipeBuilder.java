package net.minecraft.data.recipes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class ShapedRecipeBuilder implements RecipeBuilder {
   private final HolderGetter<Item> items;
   private final RecipeCategory category;
   private final Item result;
   private final int count;
   private final List<String> rows = Lists.newArrayList();
   private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   private @Nullable String group;
   private boolean showNotification = true;

   private ShapedRecipeBuilder(HolderGetter<Item> p_364858_, RecipeCategory p_249996_, ItemLike p_251475_, int p_248948_) {
      this.items = p_364858_;
      this.category = p_249996_;
      this.result = p_251475_.asItem();
      this.count = p_248948_;
   }

   public static ShapedRecipeBuilder shaped(HolderGetter<Item> p_364206_, RecipeCategory p_251325_, ItemLike p_250636_) {
      return shaped(p_364206_, p_251325_, p_250636_, 1);
   }

   public static ShapedRecipeBuilder shaped(HolderGetter<Item> p_364196_, RecipeCategory p_250853_, ItemLike p_249747_, int p_366751_) {
      return new ShapedRecipeBuilder(p_364196_, p_250853_, p_249747_, p_366751_);
   }

   public ShapedRecipeBuilder define(Character p_206417_, TagKey<Item> p_206418_) {
      return this.define(p_206417_, Ingredient.of(this.items.getOrThrow(p_206418_)));
   }

   public ShapedRecipeBuilder define(Character p_126128_, ItemLike p_126129_) {
      return this.define(p_126128_, Ingredient.of(p_126129_));
   }

   public ShapedRecipeBuilder define(Character p_126125_, Ingredient p_126126_) {
      if (this.key.containsKey(p_126125_)) {
         throw new IllegalArgumentException("Symbol '" + p_126125_ + "' is already defined!");
      }

      if (p_126125_ == ' ') {
         throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
      }

      this.key.put(p_126125_, p_126126_);
      return this;
   }

   public ShapedRecipeBuilder pattern(String p_126131_) {
      if (!this.rows.isEmpty() && p_126131_.length() != this.rows.get(0).length()) {
         throw new IllegalArgumentException("Pattern must be the same width on every line!");
      }

      this.rows.add(p_126131_);
      return this;
   }

   public ShapedRecipeBuilder unlockedBy(String p_176751_, Criterion<?> p_300780_) {
      this.criteria.put(p_176751_, p_300780_);
      return this;
   }

   public ShapedRecipeBuilder group(@Nullable String p_126146_) {
      this.group = p_126146_;
      return this;
   }

   public ShapedRecipeBuilder showNotification(boolean p_273326_) {
      this.showNotification = p_273326_;
      return this;
   }

   @Override
   public Item getResult() {
      return this.result;
   }

   @Override
   public void save(RecipeOutput p_298334_, ResourceKey<Recipe<?>> p_366310_) {
      ShapedRecipePattern shapedrecipepattern = this.ensureValid(p_366310_);
      Advancement.Builder advancement$builder = p_298334_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_366310_))
         .rewards(AdvancementRewards.Builder.recipe(p_366310_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      ShapedRecipe shapedrecipe = new ShapedRecipe(
         Objects.requireNonNullElse(this.group, ""),
         RecipeBuilder.determineBookCategory(this.category),
         shapedrecipepattern,
         new ItemStack(this.result, this.count),
         this.showNotification
      );
      p_298334_.accept(p_366310_, shapedrecipe, advancement$builder.build(p_366310_.identifier().withPrefix("recipes/" + this.category.getFolderName() + "/")));
   }

   private ShapedRecipePattern ensureValid(ResourceKey<Recipe<?>> p_362567_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_362567_.identifier());
      } else {
         return ShapedRecipePattern.of(this.key, this.rows);
      }
   }
}
