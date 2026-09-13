package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jspecify.annotations.Nullable;

public class UnquotedStringParseRule implements Rule<StringReader, String> {
   private final int minSize;
   private final DelayedException<CommandSyntaxException> error;

   public UnquotedStringParseRule(final int minSize, final DelayedException<CommandSyntaxException> error) {
      this.minSize = minSize;
      this.error = error;
   }

   public @Nullable String parse(final ParseState<StringReader> state) {
      state.input().skipWhitespace();
      int cursor = state.mark();
      String value = state.input().readUnquotedString();
      if (value.length() < this.minSize) {
         state.errorCollector().store(cursor, this.error);
         return null;
      } else {
         return value;
      }
   }
}
