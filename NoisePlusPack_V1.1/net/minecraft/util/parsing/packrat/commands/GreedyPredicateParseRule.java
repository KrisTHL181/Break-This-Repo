package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jspecify.annotations.Nullable;

public abstract class GreedyPredicateParseRule implements Rule<StringReader, String> {
   private final int minSize;
   private final int maxSize;
   private final DelayedException<CommandSyntaxException> error;

   public GreedyPredicateParseRule(int p_391684_, DelayedException<CommandSyntaxException> p_394387_) {
      this(p_391684_, Integer.MAX_VALUE, p_394387_);
   }

   public GreedyPredicateParseRule(int p_394087_, int p_392470_, DelayedException<CommandSyntaxException> p_396328_) {
      this.minSize = p_394087_;
      this.maxSize = p_392470_;
      this.error = p_396328_;
   }

   public @Nullable String parse(ParseState<StringReader> p_397364_) {
      StringReader stringreader = p_397364_.input();
      String s = stringreader.getString();
      int i = stringreader.getCursor();
      int j = i;

      while (j < s.length() && this.isAccepted(s.charAt(j)) && j - i < this.maxSize) {
         j++;
      }

      int k = j - i;
      if (k < this.minSize) {
         p_397364_.errorCollector().store(p_397364_.mark(), this.error);
         return null;
      } else {
         stringreader.setCursor(j);
         return s.substring(i, j);
      }
   }

   protected abstract boolean isAccepted(char var1);
}
