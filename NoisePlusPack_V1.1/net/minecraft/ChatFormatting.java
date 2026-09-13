package net.minecraft;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public enum ChatFormatting implements StringRepresentable {
   BLACK("BLACK", '0', 0, 0),
   DARK_BLUE("DARK_BLUE", '1', 1, 170),
   DARK_GREEN("DARK_GREEN", '2', 2, 43520),
   DARK_AQUA("DARK_AQUA", '3', 3, 43690),
   DARK_RED("DARK_RED", '4', 4, 11141120),
   DARK_PURPLE("DARK_PURPLE", '5', 5, 11141290),
   GOLD("GOLD", '6', 6, 16755200),
   GRAY("GRAY", '7', 7, 11184810),
   DARK_GRAY("DARK_GRAY", '8', 8, 5592405),
   BLUE("BLUE", '9', 9, 5592575),
   GREEN("GREEN", 'a', 10, 5635925),
   AQUA("AQUA", 'b', 11, 5636095),
   RED("RED", 'c', 12, 16733525),
   LIGHT_PURPLE("LIGHT_PURPLE", 'd', 13, 16733695),
   YELLOW("YELLOW", 'e', 14, 16777045),
   WHITE("WHITE", 'f', 15, 16777215),
   OBFUSCATED("OBFUSCATED", 'k', true),
   BOLD("BOLD", 'l', true),
   STRIKETHROUGH("STRIKETHROUGH", 'm', true),
   UNDERLINE("UNDERLINE", 'n', true),
   ITALIC("ITALIC", 'o', true),
   RESET("RESET", 'r', -1, null);

   public static final Codec<ChatFormatting> CODEC = StringRepresentable.fromEnum(ChatFormatting::values);
   public static final Codec<ChatFormatting> COLOR_CODEC = CODEC.validate(
      p_389112_ -> p_389112_.isFormat() ? DataResult.error(() -> "Formatting was not a valid color: " + p_389112_) : DataResult.success(p_389112_)
   );
   public static final char PREFIX_CODE = '§';
   private static final Map<String, ChatFormatting> FORMATTING_BY_NAME = Arrays.stream(values())
      .collect(Collectors.toMap(p_126660_ -> cleanName(p_126660_.name), p_126652_ -> (ChatFormatting)p_126652_));
   private static final Pattern STRIP_FORMATTING_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");
   private final String name;
   private final char code;
   private final boolean isFormat;
   private final String toString;
   private final int id;
   private final @Nullable Integer color;

   private static String cleanName(String p_126663_) {
      return p_126663_.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
   }

   ChatFormatting(final String p_126627_, final @Nullable char p_126628_, final int p_126629_, final Integer p_126630_) {
      this(p_126627_, p_126628_, false, p_126629_, p_126630_);
   }

   ChatFormatting(final String p_126634_, final char p_126635_, final boolean p_126636_) {
      this(p_126634_, p_126635_, p_126636_, -1, null);
   }

   ChatFormatting(final String p_126640_, final char p_126641_, final boolean p_126642_, final int p_126643_, final Integer p_126644_) {
      this.name = p_126640_;
      this.code = p_126641_;
      this.isFormat = p_126642_;
      this.id = p_126643_;
      this.color = p_126644_;
      this.toString = "§" + p_126641_;
   }

   public char getChar() {
      return this.code;
   }

   public int getId() {
      return this.id;
   }

   public boolean isFormat() {
      return this.isFormat;
   }

   public boolean isColor() {
      return !this.isFormat && this != RESET;
   }

   public @Nullable Integer getColor() {
      return this.color;
   }

   public String getName() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   @Override
   public String toString() {
      return this.toString;
   }

   @Contract("!null->!null;_->_")
   public static @Nullable String stripFormatting(@Nullable String p_126650_) {
      return p_126650_ == null ? null : STRIP_FORMATTING_PATTERN.matcher(p_126650_).replaceAll("");
   }

   public static @Nullable ChatFormatting getByName(@Nullable String p_126658_) {
      return p_126658_ == null ? null : FORMATTING_BY_NAME.get(cleanName(p_126658_));
   }

   public static @Nullable ChatFormatting getById(int p_126648_) {
      if (p_126648_ < 0) {
         return RESET;
      }

      for (ChatFormatting chatformatting : values()) {
         if (chatformatting.getId() == p_126648_) {
            return chatformatting;
         }
      }

      return null;
   }

   public static @Nullable ChatFormatting getByCode(char p_126646_) {
      char c0 = Character.toLowerCase(p_126646_);

      for (ChatFormatting chatformatting : values()) {
         if (chatformatting.code == c0) {
            return chatformatting;
         }
      }

      return null;
   }

   public static Collection<String> getNames(boolean p_126654_, boolean p_126655_) {
      List<String> list = Lists.newArrayList();

      for (ChatFormatting chatformatting : values()) {
         if ((!chatformatting.isColor() || p_126654_) && (!chatformatting.isFormat() || p_126655_)) {
            list.add(chatformatting.getName());
         }
      }

      return list;
   }

   @Override
   public String getSerializedName() {
      return this.getName();
   }
}
