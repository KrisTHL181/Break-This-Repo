package net.minecraft.commands.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;

public record StringTemplate(List<String> segments, List<String> variables) {
   public static StringTemplate fromString(String p_311822_) {
      Builder<String> builder = ImmutableList.builder();
      Builder<String> builder1 = ImmutableList.builder();
      int i = p_311822_.length();
      int j = 0;
      int k = p_311822_.indexOf(36);

      while (k != -1) {
         if (k != i - 1 && p_311822_.charAt(k + 1) == '(') {
            builder.add(p_311822_.substring(j, k));
            int l = p_311822_.indexOf(41, k + 1);
            if (l == -1) {
               throw new IllegalArgumentException("Unterminated macro variable");
            }

            String s = p_311822_.substring(k + 2, l);
            if (!isValidVariableName(s)) {
               throw new IllegalArgumentException("Invalid macro variable name '" + s + "'");
            }

            builder1.add(s);
            j = l + 1;
            k = p_311822_.indexOf(36, j);
         } else {
            k = p_311822_.indexOf(36, k + 1);
         }
      }

      if (j == 0) {
         throw new IllegalArgumentException("No variables in macro");
      }

      if (j != i) {
         builder.add(p_311822_.substring(j));
      }

      return new StringTemplate(builder.build(), builder1.build());
   }

   public static boolean isValidVariableName(String p_312174_) {
      for (int i = 0; i < p_312174_.length(); i++) {
         char c0 = p_312174_.charAt(i);
         if (!Character.isLetterOrDigit(c0) && c0 != '_') {
            return false;
         }
      }

      return true;
   }

   public String substitute(List<String> p_310551_) {
      StringBuilder stringbuilder = new StringBuilder();

      for (int i = 0; i < this.variables.size(); i++) {
         stringbuilder.append(this.segments.get(i)).append(p_310551_.get(i));
         CommandFunction.checkCommandLineLength(stringbuilder);
      }

      if (this.segments.size() > this.variables.size()) {
         stringbuilder.append(this.segments.getLast());
      }

      CommandFunction.checkCommandLineLength(stringbuilder);
      return stringbuilder.toString();
   }
}
