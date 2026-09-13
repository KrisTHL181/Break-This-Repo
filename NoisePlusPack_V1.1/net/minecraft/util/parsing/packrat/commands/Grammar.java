package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.ErrorCollector;
import net.minecraft.util.parsing.packrat.ErrorEntry;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;

public record Grammar<T>(Dictionary<StringReader> rules, NamedRule<StringReader, T> top) implements CommandArgumentParser<T> {
   public Grammar {
      rules.checkAllBound();
   }

   public Optional<T> parse(ParseState<StringReader> p_333096_) {
      return p_333096_.parseTopRule(this.top);
   }

   @Override
   public T parseForCommands(StringReader p_333110_) throws CommandSyntaxException {
      ErrorCollector.LongestOnly<StringReader> longestonly = new ErrorCollector.LongestOnly<>();
      StringReaderParserState stringreaderparserstate = new StringReaderParserState(longestonly, p_333110_);
      Optional<T> optional = this.parse(stringreaderparserstate);
      if (optional.isPresent()) {
         return optional.get();
      }

      List<ErrorEntry<StringReader>> list = longestonly.entries();
      List<Exception> list1 = list.stream().<Exception>mapMulti((p_390457_, p_390458_) -> {
         if (p_390457_.reason() instanceof DelayedException<?> delayedexception) {
            p_390458_.accept(delayedexception.create(p_333110_.getString(), p_390457_.cursor()));
         } else if (p_390457_.reason() instanceof Exception exception1) {
            p_390458_.accept(exception1);
         }
      }).toList();

      for (Exception exception : list1) {
         if (exception instanceof CommandSyntaxException commandsyntaxexception) {
            throw commandsyntaxexception;
         }
      }

      if (list1.size() == 1 && list1.get(0) instanceof RuntimeException runtimeexception) {
         throw runtimeexception;
      } else {
         throw new IllegalStateException("Failed to parse: " + list.stream().map(ErrorEntry::toString).collect(Collectors.joining(", ")));
      }
   }

   @Override
   public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder p_327864_) {
      StringReader stringreader = new StringReader(p_327864_.getInput());
      stringreader.setCursor(p_327864_.getStart());
      ErrorCollector.LongestOnly<StringReader> longestonly = new ErrorCollector.LongestOnly<>();
      StringReaderParserState stringreaderparserstate = new StringReaderParserState(longestonly, stringreader);
      this.parse(stringreaderparserstate);
      List<ErrorEntry<StringReader>> list = longestonly.entries();
      if (list.isEmpty()) {
         return p_327864_.buildFuture();
      }

      SuggestionsBuilder suggestionsbuilder = p_327864_.createOffset(longestonly.cursor());

      for (ErrorEntry<StringReader> errorentry : list) {
         if (errorentry.suggestions() instanceof ResourceSuggestion resourcesuggestion) {
            SharedSuggestionProvider.suggestResource(resourcesuggestion.possibleResources(), suggestionsbuilder);
         } else {
            SharedSuggestionProvider.suggest(errorentry.suggestions().possibleValues(stringreaderparserstate), suggestionsbuilder);
         }
      }

      return suggestionsbuilder.buildFuture();
   }
}
