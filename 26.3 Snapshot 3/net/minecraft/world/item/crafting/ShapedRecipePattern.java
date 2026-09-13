package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.chars.CharArraySet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

public final class ShapedRecipePattern {
   private static final int MAX_SIZE = 3;
   public static final char EMPTY_SLOT = ' ';
   public static final MapCodec<ShapedRecipePattern> MAP_CODEC = ShapedRecipePattern.Data.MAP_CODEC
      .flatXmap(
         ShapedRecipePattern::unpack,
         pattern -> pattern.data.<DataResult>map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      e -> e.width,
      ByteBufCodecs.VAR_INT,
      e -> e.height,
      Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
      e -> e.ingredients,
      ShapedRecipePattern::createFromNetwork
   );
   private final int width;
   private final int height;
   private final List<Optional<Ingredient>> ingredients;
   private final Optional<ShapedRecipePattern.Data> data;
   private final int ingredientCount;
   private final boolean symmetrical;

   public ShapedRecipePattern(final int width, final int height, final List<Optional<Ingredient>> ingredients, final Optional<ShapedRecipePattern.Data> data) {
      this.width = width;
      this.height = height;
      this.ingredients = ingredients;
      this.data = data;
      this.ingredientCount = (int)ingredients.stream().flatMap(Optional::stream).count();
      this.symmetrical = Util.isSymmetrical(width, height, ingredients);
   }

   private static ShapedRecipePattern createFromNetwork(final Integer width, final Integer height, final List<Optional<Ingredient>> ingredients) {
      return new ShapedRecipePattern(width, height, ingredients, Optional.empty());
   }

   public static ShapedRecipePattern of(final Map<Character, Ingredient> key, final String... pattern) {
      return of(key, List.of(pattern));
   }

   public static ShapedRecipePattern of(final Map<Character, Ingredient> key, final List<String> pattern) {
      ShapedRecipePattern.Data data = new ShapedRecipePattern.Data(key, pattern);
      return (ShapedRecipePattern)unpack(data).getOrThrow();
   }

   private static DataResult<ShapedRecipePattern> unpack(final ShapedRecipePattern.Data data) {
      String[] shrunkPattern = shrink(data.pattern);
      int width = shrunkPattern[0].length();
      int height = shrunkPattern.length;
      List<Optional<Ingredient>> ingredients = new ArrayList<>(width * height);
      CharSet unusedSymbols = new CharArraySet(data.key.keySet());

      for (String line : shrunkPattern) {
         for (int x = 0; x < line.length(); x++) {
            char symbol = line.charAt(x);
            Optional<Ingredient> ingredient;
            if (symbol == ' ') {
               ingredient = Optional.empty();
            } else {
               Ingredient ingredientForSymbol = data.key.get(symbol);
               if (ingredientForSymbol == null) {
                  return DataResult.error(() -> "Pattern references symbol '" + symbol + "' but it's not defined in the key");
               }

               ingredient = Optional.of(ingredientForSymbol);
            }

            unusedSymbols.remove(symbol);
            ingredients.add(ingredient);
         }
      }

      return !unusedSymbols.isEmpty()
         ? DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + unusedSymbols)
         : DataResult.success(new ShapedRecipePattern(width, height, ingredients, Optional.of(data)));
   }

   @VisibleForTesting
   static String[] shrink(final List<String> pattern) {
      int left = Integer.MAX_VALUE;
      int right = 0;
      int top = 0;
      int bottom = 0;

      for (int i = 0; i < pattern.size(); i++) {
         String line = pattern.get(i);
         left = Math.min(left, firstNonEmpty(line));
         int lastNonSpace = lastNonEmpty(line);
         right = Math.max(right, lastNonSpace);
         if (lastNonSpace < 0) {
            if (top == i) {
               top++;
            }

            bottom++;
         } else {
            bottom = 0;
         }
      }

      if (pattern.size() == bottom) {
         return new String[0];
      }

      String[] result = new String[pattern.size() - bottom - top];

      for (int line = 0; line < result.length; line++) {
         result[line] = pattern.get(line + top).substring(left, right + 1);
      }

      return result;
   }

   private static int firstNonEmpty(final String line) {
      int index = 0;

      while (index < line.length() && line.charAt(index) == ' ') {
         index++;
      }

      return index;
   }

   private static int lastNonEmpty(final String line) {
      int index = line.length() - 1;

      while (index >= 0 && line.charAt(index) == ' ') {
         index--;
      }

      return index;
   }

   public boolean matches(final CraftingInput input) {
      if (input.ingredientCount() != this.ingredientCount) {
         return false;
      }

      if (input.width() == this.width && input.height() == this.height) {
         if (!this.symmetrical && this.matches(input, true)) {
            return true;
         }

         if (this.matches(input, false)) {
            return true;
         }
      }

      return false;
   }

   private boolean matches(final CraftingInput input, final boolean xFlip) {
      for (int y = 0; y < this.height; y++) {
         for (int x = 0; x < this.width; x++) {
            Optional<Ingredient> expected;
            if (xFlip) {
               expected = this.ingredients.get(this.width - x - 1 + y * this.width);
            } else {
               expected = this.ingredients.get(x + y * this.width);
            }

            ItemStack actual = input.getItem(x, y);
            if (!Ingredient.testOptionalIngredient(expected, actual)) {
               return false;
            }
         }
      }

      return true;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public List<Optional<Ingredient>> ingredients() {
      return this.ingredients;
   }

   public record Data(Map<Character, Ingredient> key, List<String> pattern) {
      private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(strings -> {
         if (strings.size() > 3) {
            return DataResult.error(() -> "Invalid pattern: too many rows, 3 is maximum");
         }

         if (strings.isEmpty()) {
            return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
         }

         int firstLength = ((String)strings.getFirst()).length();

         for (String line : strings) {
            if (line.length() > 3) {
               return DataResult.error(() -> "Invalid pattern: too many columns, 3 is maximum");
            }

            if (firstLength != line.length()) {
               return DataResult.error(() -> "Invalid pattern: each row must be the same width");
            }
         }

         return DataResult.success(strings);
      }, Function.identity());
      private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(symbol -> {
         if (symbol.length() != 1) {
            return DataResult.error(() -> "Invalid key entry: '" + symbol + "' is an invalid symbol (must be 1 character only).");
         } else {
            return " ".equals(symbol) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(symbol.charAt(0));
         }
      }, String::valueOf);
      public static final MapCodec<ShapedRecipePattern.Data> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC).fieldOf("key").forGetter(d -> d.key),
               PATTERN_CODEC.fieldOf("pattern").forGetter(d -> d.pattern)
            )
            .apply(i, ShapedRecipePattern.Data::new)
      );
   }
}
