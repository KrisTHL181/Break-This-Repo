package net.minecraft.commands.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;

public record StringTemplate(List<String> segments, List<String> variables) {
   public static StringTemplate fromString(final String input) {
      Builder<String> segments = ImmutableList.builder();
      Builder<String> variables = ImmutableList.builder();
      int length = input.length();
      int start = 0;
      int index = input.indexOf(36);

      while (index != -1) {
         if (index != length - 1 && input.charAt(index + 1) == '(') {
            segments.add(input.substring(start, index));
            int variableEnd = input.indexOf(41, index + 1);
            if (variableEnd == -1) {
               throw new IllegalArgumentException("Unterminated macro variable");
            }

            String variable = input.substring(index + 2, variableEnd);
            if (!isValidVariableName(variable)) {
               throw new IllegalArgumentException("Invalid macro variable name '" + variable + "'");
            }

            variables.add(variable);
            start = variableEnd + 1;
            index = input.indexOf(36, start);
         } else {
            index = input.indexOf(36, index + 1);
         }
      }

      if (start == 0) {
         throw new IllegalArgumentException("No variables in macro");
      }

      if (start != length) {
         segments.add(input.substring(start));
      }

      return new StringTemplate(segments.build(), variables.build());
   }

   public static boolean isValidVariableName(final String variable) {
      for (int i = 0; i < variable.length(); i++) {
         char character = variable.charAt(i);
         if (!Character.isLetterOrDigit(character) && character != '_') {
            return false;
         }
      }

      return true;
   }

   public String substitute(final List<String> arguments) {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < this.variables.size(); i++) {
         builder.append(this.segments.get(i)).append(arguments.get(i));
         CommandFunction.checkCommandLineLength(builder);
      }

      if (this.segments.size() > this.variables.size()) {
         builder.append(this.segments.getLast());
      }

      CommandFunction.checkCommandLineLength(builder);
      return builder.toString();
   }
}
