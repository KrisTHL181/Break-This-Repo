package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public final class GreedyPatternParseRule implements Rule<StringReader, String> {
   private final Pattern pattern;
   private final DelayedException<CommandSyntaxException> error;

   public GreedyPatternParseRule(final Pattern pattern, final DelayedException<CommandSyntaxException> error) {
      this.pattern = pattern;
      this.error = error;
   }

   public String parse(final ParseState<StringReader> state) {
      StringReader input = state.input();
      String fullString = input.getString();
      Matcher matcher = this.pattern.matcher(fullString).region(input.getCursor(), fullString.length());
      if (!matcher.lookingAt()) {
         state.errorCollector().store(state.mark(), this.error);
         return null;
      } else {
         input.setCursor(matcher.end());
         return matcher.group(0);
      }
   }
}
