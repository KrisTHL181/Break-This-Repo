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

   public UnquotedStringParseRule(int p_391791_, DelayedException<CommandSyntaxException> p_397262_) {
      this.minSize = p_391791_;
      this.error = p_397262_;
   }

   public @Nullable String parse(ParseState<StringReader> p_392280_) {
      p_392280_.input().skipWhitespace();
      int i = p_392280_.mark();
      String s = p_392280_.input().readUnquotedString();
      if (s.length() < this.minSize) {
         p_392280_.errorCollector().store(i, this.error);
         return null;
      } else {
         return s;
      }
   }
}
