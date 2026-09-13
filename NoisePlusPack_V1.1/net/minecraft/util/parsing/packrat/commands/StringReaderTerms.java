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
   static Term<StringReader> word(String p_327924_) {
      return new StringReaderTerms.TerminalWord(p_327924_);
   }

   static Term<StringReader> character(final char p_329750_) {
      return new StringReaderTerms.TerminalCharacters(CharList.of(p_329750_)) {
         @Override
         protected boolean isAccepted(char p_391277_) {
            return p_329750_ == p_391277_;
         }
      };
   }

   static Term<StringReader> characters(final char p_395208_, final char p_393692_) {
      return new StringReaderTerms.TerminalCharacters(CharList.of(p_395208_, p_393692_)) {
         @Override
         protected boolean isAccepted(char p_393492_) {
            return p_393492_ == p_395208_ || p_393492_ == p_393692_;
         }
      };
   }

   static StringReader createReader(String p_397367_, int p_395927_) {
      StringReader stringreader = new StringReader(p_397367_);
      stringreader.setCursor(p_395927_);
      return stringreader;
   }

   abstract class TerminalCharacters implements Term<StringReader> {
      private final DelayedException<CommandSyntaxException> error;
      private final SuggestionSupplier<StringReader> suggestions;

      public TerminalCharacters(CharList p_395243_) {
         String s = p_395243_.intStream().mapToObj(Character::toString).collect(Collectors.joining("|"));
         this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), s);
         this.suggestions = p_392492_ -> p_395243_.intStream().mapToObj(Character::toString);
      }

      @Override
      public boolean parse(ParseState<StringReader> p_393490_, Scope p_391874_, Control p_397093_) {
         p_393490_.input().skipWhitespace();
         int i = p_393490_.mark();
         if (p_393490_.input().canRead() && this.isAccepted(p_393490_.input().read())) {
            return true;
         }

         p_393490_.errorCollector().store(i, this.suggestions, this.error);
         return false;
      }

      protected abstract boolean isAccepted(char var1);
   }

   final class TerminalWord implements Term<StringReader> {
      private final String value;
      private final DelayedException<CommandSyntaxException> error;
      private final SuggestionSupplier<StringReader> suggestions;

      public TerminalWord(String p_329076_) {
         this.value = p_329076_;
         this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), p_329076_);
         this.suggestions = p_390460_ -> Stream.of(p_329076_);
      }

      @Override
      public boolean parse(ParseState<StringReader> p_333566_, Scope p_332362_, Control p_328812_) {
         p_333566_.input().skipWhitespace();
         int i = p_333566_.mark();
         String s = p_333566_.input().readUnquotedString();
         if (!s.equals(this.value)) {
            p_333566_.errorCollector().store(i, this.suggestions, this.error);
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
