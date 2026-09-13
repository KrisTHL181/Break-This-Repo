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
         p_341595_ -> p_341595_.data.<DataResult>map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Cannot encode unpacked recipe"))
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipePattern> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      p_359853_ -> p_359853_.width,
      ByteBufCodecs.VAR_INT,
      p_359854_ -> p_359854_.height,
      Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
      p_359852_ -> p_359852_.ingredients,
      ShapedRecipePattern::createFromNetwork
   );
   private final int width;
   private final int height;
   private final List<Optional<Ingredient>> ingredients;
   private final Optional<ShapedRecipePattern.Data> data;
   private final int ingredientCount;
   private final boolean symmetrical;

   public ShapedRecipePattern(int p_309692_, int p_311724_, List<Optional<Ingredient>> p_361049_, Optional<ShapedRecipePattern.Data> p_310645_) {
      this.width = p_309692_;
      this.height = p_311724_;
      this.ingredients = p_361049_;
      this.data = p_310645_;
      this.ingredientCount = (int)p_361049_.stream().flatMap(Optional::stream).count();
      this.symmetrical = Util.isSymmetrical(p_309692_, p_311724_, p_361049_);
   }

   private static ShapedRecipePattern createFromNetwork(Integer p_365396_, Integer p_361921_, List<Optional<Ingredient>> p_363051_) {
      return new ShapedRecipePattern(p_365396_, p_361921_, p_363051_, Optional.empty());
   }

   public static ShapedRecipePattern of(Map<Character, Ingredient> p_310983_, String... p_310430_) {
      return of(p_310983_, List.of(p_310430_));
   }

   public static ShapedRecipePattern of(Map<Character, Ingredient> p_313226_, List<String> p_310089_) {
      ShapedRecipePattern.Data shapedrecipepattern$data = new ShapedRecipePattern.Data(p_313226_, p_310089_);
      return (ShapedRecipePattern)unpack(shapedrecipepattern$data).getOrThrow();
   }

   private static DataResult<ShapedRecipePattern> unpack(ShapedRecipePattern.Data p_312333_) {
      String[] astring = shrink(p_312333_.pattern);
      int i = astring[0].length();
      int j = astring.length;
      List<Optional<Ingredient>> list = new ArrayList<>(i * j);
      CharSet charset = new CharArraySet(p_312333_.key.keySet());

      for (String s : astring) {
         for (int k = 0; k < s.length(); k++) {
            char c0 = s.charAt(k);
            Optional<Ingredient> optional;
            if (c0 == ' ') {
               optional = Optional.empty();
            } else {
               Ingredient ingredient = p_312333_.key.get(c0);
               if (ingredient == null) {
                  return DataResult.error(() -> "Pattern references symbol '" + c0 + "' but it's not defined in the key");
               }

               optional = Optional.of(ingredient);
            }

            charset.remove(c0);
            list.add(optional);
         }
      }

      return !charset.isEmpty()
         ? DataResult.error(() -> "Key defines symbols that aren't used in pattern: " + charset)
         : DataResult.success(new ShapedRecipePattern(i, j, list, Optional.of(p_312333_)));
   }

   @VisibleForTesting
   static String[] shrink(List<String> p_311492_) {
      int i = Integer.MAX_VALUE;
      int j = 0;
      int k = 0;
      int l = 0;

      for (int i1 = 0; i1 < p_311492_.size(); i1++) {
         String s = p_311492_.get(i1);
         i = Math.min(i, firstNonEmpty(s));
         int j1 = lastNonEmpty(s);
         j = Math.max(j, j1);
         if (j1 < 0) {
            if (k == i1) {
               k++;
            }

            l++;
         } else {
            l = 0;
         }
      }

      if (p_311492_.size() == l) {
         return new String[0];
      }

      String[] astring = new String[p_311492_.size() - l - k];

      for (int k1 = 0; k1 < astring.length; k1++) {
         astring[k1] = p_311492_.get(k1 + k).substring(i, j + 1);
      }

      return astring;
   }

   private static int firstNonEmpty(String p_309836_) {
      int i = 0;

      while (i < p_309836_.length() && p_309836_.charAt(i) == ' ') {
         i++;
      }

      return i;
   }

   private static int lastNonEmpty(String p_312853_) {
      int i = p_312853_.length() - 1;

      while (i >= 0 && p_312853_.charAt(i) == ' ') {
         i--;
      }

      return i;
   }

   public boolean matches(CraftingInput p_343130_) {
      if (p_343130_.ingredientCount() != this.ingredientCount) {
         return false;
      }

      if (p_343130_.width() == this.width && p_343130_.height() == this.height) {
         if (!this.symmetrical && this.matches(p_343130_, true)) {
            return true;
         }

         if (this.matches(p_343130_, false)) {
            return true;
         }
      }

      return false;
   }

   private boolean matches(CraftingInput p_345096_, boolean p_342488_) {
      for (int i = 0; i < this.height; i++) {
         for (int j = 0; j < this.width; j++) {
            Optional<Ingredient> optional;
            if (p_342488_) {
               optional = this.ingredients.get(this.width - j - 1 + i * this.width);
            } else {
               optional = this.ingredients.get(j + i * this.width);
            }

            ItemStack itemstack = p_345096_.getItem(j, i);
            if (!Ingredient.testOptionalIngredient(optional, itemstack)) {
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
      private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().comapFlatMap(p_311191_ -> {
         if (p_311191_.size() > 3) {
            return DataResult.error(() -> "Invalid pattern: too many rows, 3 is maximum");
         }

         if (p_311191_.isEmpty()) {
            return DataResult.error(() -> "Invalid pattern: empty pattern not allowed");
         }

         int i = ((String)p_311191_.getFirst()).length();

         for (String s : p_311191_) {
            if (s.length() > 3) {
               return DataResult.error(() -> "Invalid pattern: too many columns, 3 is maximum");
            }

            if (i != s.length()) {
               return DataResult.error(() -> "Invalid pattern: each row must be the same width");
            }
         }

         return DataResult.success(p_311191_);
      }, Function.identity());
      private static final Codec<Character> SYMBOL_CODEC = Codec.STRING.comapFlatMap(p_313217_ -> {
         if (p_313217_.length() != 1) {
            return DataResult.error(() -> "Invalid key entry: '" + p_313217_ + "' is an invalid symbol (must be 1 character only).");
         } else {
            return " ".equals(p_313217_) ? DataResult.error(() -> "Invalid key entry: ' ' is a reserved symbol.") : DataResult.success(p_313217_.charAt(0));
         }
      }, String::valueOf);
      public static final MapCodec<ShapedRecipePattern.Data> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_359855_ -> p_359855_.group(
               ExtraCodecs.strictUnboundedMap(SYMBOL_CODEC, Ingredient.CODEC).fieldOf("key").forGetter(p_311797_ -> p_311797_.key),
               PATTERN_CODEC.fieldOf("pattern").forGetter(p_309770_ -> p_309770_.pattern)
            )
            .apply(p_359855_, ShapedRecipePattern.Data::new)
      );
   }
}
