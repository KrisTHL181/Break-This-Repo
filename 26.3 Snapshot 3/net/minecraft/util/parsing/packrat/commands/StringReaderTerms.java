package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.chars.CharList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import net.minecraft.util.parsing.packrat.Term;

public interface StringReaderTerms {
   static Term<StringReader> word(final String value) {
      return new StringReaderTerms.TerminalWord(value);
   }

   static Term<StringReader> character(final char value) {
      return new StringReaderTerms.TerminalCharacters(CharList.of(value)) {
         @Override
         protected boolean isAccepted(final char v) {
            return value == v;
         }
      };
   }

   static Term<StringReader> characters(final char v1, final char v2) {
      return new StringReaderTerms.TerminalCharacters(CharList.of(v1, v2)) {
         @Override
         protected boolean isAccepted(final char v) {
            return v == v1 || v == v2;
         }
      };
   }

   static StringReader createReader(final String contents, final int cursor) {
      StringReader reader = new StringReader(contents);
      reader.setCursor(cursor);
      return reader;
   }

   abstract class TerminalCharacters implements Term<StringReader> {
      private final DelayedException<CommandSyntaxException> error;
      private final SuggestionSupplier<StringReader> suggestions;

      public TerminalCharacters(final CharList values) {
         String joinedValues = values.intStream().mapToObj(Character::toString).collect(Collectors.joining("|"));
         this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), joinedValues);
         this.suggestions = s -> values.intStream().mapToObj(Character::toString);
      }

      @Override
      public boolean parse(final ParseState<StringReader> state, final Scope scope, final Control control) {
         state.input().skipWhitespace();
         int cursor = state.mark();
         if (state.input().canRead() && this.isAccepted(state.input().read())) {
            return true;
         }

         state.errorCollector().store(cursor, this.suggestions, this.error);
         return false;
      }

      protected abstract boolean isAccepted(char value);
   }

   final class TerminalWord implements Term<StringReader> {
      private final String value;
      private final DelayedException<CommandSyntaxException> error;
      private final SuggestionSupplier<StringReader> suggestions;

      public TerminalWord(final String value) {
         this.value = value;
         this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), value);
         this.suggestions = s -> Stream.of(value);
      }

      @Override
      public boolean parse(final ParseState<StringReader> state, final Scope scope, final Control control) {
         state.input().skipWhitespace();
         int cursor = state.mark();
         String value = state.input().readUnquotedString();
         if (!value.equals(this.value)) {
            state.errorCollector().store(cursor, this.suggestions, this.error);
            return false;
         } else {
            return true;
         }
      }

      @Override
      public String toString() {
         return "terminal[" + this.value + "]";
      }
   }
}
