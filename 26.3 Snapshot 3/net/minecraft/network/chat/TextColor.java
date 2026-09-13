package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

public final class TextColor {
   private static final String CUSTOM_COLOR_PREFIX = "#";
   public static final Codec<TextColor> CODEC = Codec.STRING.comapFlatMap(TextColor::parseColor, TextColor::serialize);
   private static final Map<String, TextColor> NAMED_COLORS = new HashMap<>();
   public static final TextColor BLACK = named("black", 0);
   public static final TextColor DARK_BLUE = named("dark_blue", 170);
   public static final TextColor DARK_GREEN = named("dark_green", 43520);
   public static final TextColor DARK_AQUA = named("dark_aqua", 43690);
   public static final TextColor DARK_RED = named("dark_red", 11141120);
   public static final TextColor DARK_PURPLE = named("dark_purple", 11141290);
   public static final TextColor GOLD = named("gold", 16755200);
   public static final TextColor GRAY = named("gray", 11184810);
   public static final TextColor DARK_GRAY = named("dark_gray", 5592405);
   public static final TextColor BLUE = named("blue", 5592575);
   public static final TextColor GREEN = named("green", 5635925);
   public static final TextColor AQUA = named("aqua", 5636095);
   public static final TextColor RED = named("red", 16733525);
   public static final TextColor LIGHT_PURPLE = named("light_purple", 16733695);
   public static final TextColor YELLOW = named("yellow", 16777045);
   public static final TextColor WHITE = named("white", 16777215);
   private final int value;
   private final @Nullable String name;

   private TextColor(final int value, final String name) {
      this.value = value & 16777215;
      this.name = name;
   }

   private TextColor(final int value) {
      this.value = value & 16777215;
      this.name = null;
   }

   private static TextColor named(final String name, final int rgb) {
      TextColor result = new TextColor(rgb, name);
      NAMED_COLORS.put(name, result);
      return result;
   }

   public int getValue() {
      return this.value;
   }

   public String serialize() {
      return this.name != null ? this.name : this.formatValue();
   }

   private String formatValue() {
      return String.format(Locale.ROOT, "#%06X", this.value);
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         TextColor other = (TextColor)o;
         return this.value == other.value;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.value, this.name);
   }

   @Override
   public String toString() {
      return this.serialize();
   }

   public static @Nullable TextColor fromLegacyFormat(final ChatFormatting format) {
      return switch (format) {
         case BLACK -> BLACK;
         case DARK_BLUE -> DARK_BLUE;
         case DARK_GREEN -> DARK_GREEN;
         case DARK_AQUA -> DARK_AQUA;
         case DARK_RED -> DARK_RED;
         case DARK_PURPLE -> DARK_PURPLE;
         case GOLD -> GOLD;
         case GRAY -> GRAY;
         case DARK_GRAY -> DARK_GRAY;
         case BLUE -> BLUE;
         case GREEN -> GREEN;
         case AQUA -> AQUA;
         case RED -> RED;
         case LIGHT_PURPLE -> LIGHT_PURPLE;
         case YELLOW -> YELLOW;
         case WHITE -> WHITE;
         default -> null;
      };
   }

   public static TextColor fromRgb(final int rgb) {
      return new TextColor(rgb);
   }

   public static DataResult<TextColor> parseColor(final String color) {
      if (color.startsWith("#")) {
         try {
            int value = Integer.parseInt(color.substring(1), 16);
            return value >= 0 && value <= 16777215
               ? DataResult.success(fromRgb(value), Lifecycle.stable())
               : DataResult.error(() -> "Color value out of range: " + color);
         } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid color value: " + color);
         }
      } else {
         TextColor predefinedColor = NAMED_COLORS.get(color);
         return predefinedColor == null ? DataResult.error(() -> "Invalid color name: " + color) : DataResult.success(predefinedColor, Lifecycle.stable());
      }
   }
}
