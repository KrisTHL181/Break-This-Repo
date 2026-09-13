package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jspecify.annotations.Nullable;

public abstract class NumberRunParseRule implements Rule<StringReader, String> {
   private final DelayedException<CommandSyntaxException> noValueError;
   private final DelayedException<CommandSyntaxException> underscoreNotAllowedError;

   public NumberRunParseRule(DelayedException<CommandSyntaxException> p_395317_, DelayedException<CommandSyntaxException> p_397647_) {
      this.noValueError = p_395317_;
      this.underscoreNotAllowedError = p_397647_;
   }

   public @Nullable String parse(ParseState<StringReader> p_397291_) {
      StringReader stringreader = p_397291_.input();
      stringreader.skipWhitespace();
      String s = stringreader.getString();
      int i = stringreader.getCursor();
      int j = i;

      while (j < s.length() && this.isAccepted(s.charAt(j))) {
         j++;
      }

      int k = j - i;
      if (k == 0) {
         p_397291_.errorCollector().store(p_397291_.mark(), this.noValueError);
         return null;
      } else if (s.charAt(i) != '_' && s.charAt(j - 1) != '_') {
         stringreader.setCursor(j);
         return s.substring(i, j);
      } else {
         p_397291_.errorCollector().store(p_397291_.mark(), this.underscoreNotAllowedError);
         return null;
      }
   }

   protected abstract boolean isAccepted(char var1);
}
