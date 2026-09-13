package net.minecraft.data.recipes;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class ShapelessRecipeBuilder implements RecipeBuilder {
   private final HolderGetter<Item> items;
   private final RecipeCategory category;
   private final ItemStackTemplate result;
   private final List<Ingredient> ingredients = new ArrayList<>();
   private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
   private @Nullable String group;

   private ShapelessRecipeBuilder(final HolderGetter<Item> items, final RecipeCategory category, final ItemStackTemplate result) {
      this.items = items;
      this.category = category;
      this.result = result;
   }

   public static ShapelessRecipeBuilder shapeless(final HolderGetter<Item> items, final RecipeCategory category, final ItemStackTemplate result) {
      return new ShapelessRecipeBuilder(items, category, result);
   }

   public static ShapelessRecipeBuilder shapeless(final HolderGetter<Item> items, final RecipeCategory category, final ItemLike item) {
      return shapeless(items, category, item, 1);
   }

   public static ShapelessRecipeBuilder shapeless(final HolderGetter<Item> items, final RecipeCategory category, final ItemLike item, final int count) {
      return new ShapelessRecipeBuilder(items, category, new ItemStackTemplate(item.asItem(), count));
   }

   public ShapelessRecipeBuilder requires(final TagKey<Item> tag) {
      return this.requires(Ingredient.of(this.items.getOrThrow(tag)));
   }

   public ShapelessRecipeBuilder requires(final ItemLike item) {
      return this.requires(item, 1);
   }

   public ShapelessRecipeBuilder requires(final ItemLike item, final int count) {
      for (int i = 0; i < count; i++) {
         this.requires(Ingredient.of(item));
      }

      return this;
   }

   public ShapelessRecipeBuilder requires(final Ingredient ingredient) {
      return this.requires(ingredient, 1);
   }

   public ShapelessRecipeBuilder requires(final Ingredient ingredient, final int count) {
      for (int i = 0; i < count; i++) {
         this.ingredients.add(ingredient);
      }

      return this;
   }

   public ShapelessRecipeBuilder unlockedBy(final String name, final Criterion<?> criterion) {
      this.advancementBuilder.unlockedBy(name, criterion);
      return this;
   }

   public ShapelessRecipeBuilder group(final @Nullable String group) {
      this.group = group;
      return this;
   }

   @Override
   public ResourceKey<Recipe<?>> defaultId() {
      return RecipeBuilder.getDefaultRecipeId(this.result);
   }

   @Override
   public void save(final RecipeOutput output, final ResourceKey<Recipe<?>> id) {
      ShapelessRecipe recipe = new ShapelessRecipe(
         RecipeBuilder.createCraftingCommonInfo(true), RecipeBuilder.createCraftingBookInfo(this.category, this.group), this.result, this.ingredients
      );
      output.accept(id, recipe, this.advancementBuilder.build(output, id, this.category));
   }
}
