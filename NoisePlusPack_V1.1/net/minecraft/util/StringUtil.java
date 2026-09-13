package net.minecraft.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class StringUtil {
   private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
   private static final Pattern LINE_PATTERN = Pattern.compile("\\r\\n|\\v");
   private static final Pattern LINE_END_PATTERN = Pattern.compile("(?:\\r\\n|\\v)$");

   public static String formatTickDuration(int p_14405_, float p_313197_) {
      int i = Mth.floor(p_14405_ / p_313197_);
      int j = i / 60;
      i %= 60;
      int k = j / 60;
      j %= 60;
      return k > 0 ? String.format(Locale.ROOT, "%02d:%02d:%02d", k, j, i) : String.format(Locale.ROOT, "%02d:%02d", j, i);
   }

   public static String stripColor(String p_14407_) {
      return STRIP_COLOR_PATTERN.matcher(p_14407_).replaceAll("");
   }

   public static boolean isNullOrEmpty(@Nullable String p_14409_) {
      return StringUtils.isEmpty(p_14409_);
   }

   public static String truncateStringIfNecessary(String p_144999_, int p_145000_, boolean p_145001_) {
      if (p_144999_.length() <= p_145000_) {
         return p_144999_;
      } else {
         return p_145001_ && p_145000_ > 3 ? p_144999_.substring(0, p_145000_ - 3) + "..." : p_144999_.substring(0, p_145000_);
      }
   }

   public static int lineCount(String p_145003_) {
      if (p_145003_.isEmpty()) {
         return 0;
      }

      Matcher matcher = LINE_PATTERN.matcher(p_145003_);
      int i = 1;

      while (matcher.find()) {
         i++;
      }

      return i;
   }

   public static boolean endsWithNewLine(String p_145005_) {
      return LINE_END_PATTERN.matcher(p_145005_).find();
   }

   public static String trimChatMessage(String p_216470_) {
      return truncateStringIfNecessary(p_216470_, 256, false);
   }

   public static boolean isAllowedChatCharacter(int p_422340_) {
      return p_422340_ != 167 && p_422340_ >= 32 && p_422340_ != 127;
   }

   public static boolean isValidPlayerName(String p_328576_) {
      return p_328576_.length() > 16 ? false : p_328576_.chars().filter(p_333267_ -> p_333267_ <= 32 || p_333267_ >= 127).findAny().isEmpty();
   }

   public static String filterText(String p_329405_) {
      return filterText(p_329405_, false);
   }

   public static String filterText(String p_335196_, boolean p_329791_) {
      StringBuilder stringbuilder = new StringBuilder();

      for (char c0 : p_335196_.toCharArray()) {
         if (isAllowedChatCharacter(c0)) {
            stringbuilder.append(c0);
         } else if (p_329791_ && c0 == '\n') {
            stringbuilder.append(c0);
         }
      }

      return stringbuilder.toString();
   }

   public static boolean isWhitespace(int p_332672_) {
      return Character.isWhitespace(p_332672_) || Character.isSpaceChar(p_332672_);
   }

   public static boolean isBlank(@Nullable String p_334499_) {
      return p_334499_ != null && !p_334499_.isEmpty() ? p_334499_.chars().allMatch(StringUtil::isWhitespace) : true;
   }
}
