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

   public static String formatTickDuration(final int ticks, final float tickrate) {
      int seconds = Mth.floor(ticks / tickrate);
      int minutes = seconds / 60;
      seconds %= 60;
      int hours = minutes / 60;
      minutes %= 60;
      return hours > 0 ? String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds) : String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
   }

   public static String stripColor(final String input) {
      return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
   }

   public static boolean isNullOrEmpty(final @Nullable String s) {
      return StringUtils.isEmpty(s);
   }

   public static String truncateStringIfNecessary(final String s, final int maxLength, final boolean addDotDotDotIfTruncated) {
      if (s.length() <= maxLength) {
         return s;
      } else {
         return addDotDotDotIfTruncated && maxLength > 3 ? s.substring(0, maxLength - 3) + "..." : s.substring(0, maxLength);
      }
   }

   public static int lineCount(final String s) {
      if (s.isEmpty()) {
         return 0;
      }

      Matcher matcher = LINE_PATTERN.matcher(s);
      int count = 1;

      while (matcher.find()) {
         count++;
      }

      return count;
   }

   public static boolean endsWithNewLine(final String s) {
      return LINE_END_PATTERN.matcher(s).find();
   }

   public static String trimChatMessage(final String message) {
      return truncateStringIfNecessary(message, 256, false);
   }

   public static boolean isAllowedChatCharacter(final int ch) {
      return ch != 167 && ch >= 32 && ch != 127;
   }

   public static boolean isValidPlayerName(final String name) {
      return name.length() > 16 ? false : name.chars().filter(c -> c <= 32 || c >= 127).findAny().isEmpty();
   }

   public static String filterText(final String input) {
      return filterText(input, false);
   }

   public static String filterText(final String input, final boolean multiline) {
      StringBuilder builder = new StringBuilder();

      for (char c : input.toCharArray()) {
         if (isAllowedChatCharacter(c)) {
            builder.append(c);
         } else if (multiline && c == '\n') {
            builder.append(c);
         }
      }

      return builder.toString();
   }

   public static boolean isWhitespace(final int codepoint) {
      return Character.isWhitespace(codepoint) || Character.isSpaceChar(codepoint);
   }

   public static boolean isBlank(final @Nullable String string) {
      return string != null && !string.isEmpty() ? string.chars().allMatch(StringUtil::isWhitespace) : true;
   }
}
