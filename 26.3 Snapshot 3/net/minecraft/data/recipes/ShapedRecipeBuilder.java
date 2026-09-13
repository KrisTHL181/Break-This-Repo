package net.minecraft.data.recipes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class ShapedRecipeBuilder implements RecipeBuilder {
   private final HolderGetter<Item> items;
   private final RecipeCategory category;
   private final ItemStackTemplate result;
   private final List<String> rows = Lists.newArrayList();
   private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
   private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
   private @Nullable String group;
   private boolean showNotification = true;

   private ShapedRecipeBuilder(final HolderGetter<Item> items, final RecipeCategory category, final ItemStackTemplate result) {
      this.items = items;
      this.category = category;
      this.result = result;
   }

   private ShapedRecipeBuilder(final HolderGetter<Item> items, final RecipeCategory category, final ItemLike result, final int count) {
      this(items, category, new ItemStackTemplate(result.asItem(), count));
   }

   public static ShapedRecipeBuilder shaped(final HolderGetter<Item> items, final RecipeCategory category, final ItemLike item) {
      return shaped(items, category, item, 1);
   }

   public static ShapedRecipeBuilder shaped(final HolderGetter<Item> items, final RecipeCategory category, final ItemLike item, final int count) {
      return new ShapedRecipeBuilder(items, category, item, count);
   }

   public ShapedRecipeBuilder define(final Character symbol, final TagKey<Item> tag) {
      return this.define(symbol, Ingredient.of(this.items.getOrThrow(tag)));
   }

   public ShapedRecipeBuilder define(final Character symbol, final ItemLike item) {
      return this.define(symbol, Ingredient.of(item));
   }

   public ShapedRecipeBuilder define(final Character symbol, final Ingredient ingredient) {
      if (this.key.containsKey(symbol)) {
         throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
      }

      if (symbol == ' ') {
         throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
      }

      this.key.put(symbol, ingredient);
      return this;
   }

   public ShapedRecipeBuilder pattern(final String row) {
      if (!this.rows.isEmpty() && row.length() != this.rows.get(0).length()) {
         throw new IllegalArgumentException("Pattern must be the same width on every line!");
      }

      this.rows.add(row);
      return this;
   }

   public ShapedRecipeBuilder unlockedBy(final String name, final Criterion<?> criterion) {
      this.advancementBuilder.unlockedBy(name, criterion);
      return this;
   }

   public ShapedRecipeBuilder group(final @Nullable String group) {
      this.group = group;
      return this;
   }

   public ShapedRecipeBuilder showNotification(final boolean showNotification) {
      this.showNotification = showNotification;
      return this;
   }

   @Override
   public ResourceKey<Recipe<?>> defaultId() {
      return RecipeBuilder.getDefaultRecipeId(this.result);
   }

   @Override
   public void save(final RecipeOutput output, final ResourceKey<Recipe<?>> id) {
      ShapedRecipePattern pattern = ShapedRecipePattern.of(this.key, this.rows);
      ShapedRecipe recipe = new ShapedRecipe(
         RecipeBuilder.createCraftingCommonInfo(this.showNotification), RecipeBuilder.createCraftingBookInfo(this.category, this.group), pattern, this.result
      );
      output.accept(id, recipe, this.advancementBuilder.build(output, id, this.category));
   }
}
