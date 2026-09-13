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

   public Optional<T> parse(final ParseState<StringReader> state) {
      return state.parseTopRule(this.top);
   }

   @Override
   public T parseForCommands(final StringReader reader) throws CommandSyntaxException {
      ErrorCollector.LongestOnly<StringReader> errorCollector = new ErrorCollector.LongestOnly<>();
      StringReaderParserState state = new StringReaderParserState(errorCollector, reader);
      Optional<T> result = this.parse(state);
      if (result.isPresent()) {
         return result.get();
      }

      List<ErrorEntry<StringReader>> errorEntries = errorCollector.entries();
      List<Exception> exceptions = errorEntries.stream().<Exception>mapMulti((entry, output) -> {
         if (entry.reason() instanceof DelayedException<?> delayedException) {
            output.accept(delayedException.create(reader.getString(), entry.cursor()));
         } else if (entry.reason() instanceof Exception exceptionx) {
            output.accept(exceptionx);
         }
      }).toList();

      for (Exception exception : exceptions) {
         if (exception instanceof CommandSyntaxException cse) {
            throw cse;
         }
      }

      if (exceptions.size() == 1 && exceptions.get(0) instanceof RuntimeException re) {
         throw re;
      } else {
         throw new IllegalStateException("Failed to parse: " + errorEntries.stream().map(ErrorEntry::toString).collect(Collectors.joining(", ")));
      }
   }

   @Override
   public CompletableFuture<Suggestions> parseForSuggestions(final SuggestionsBuilder suggestionsBuilder) {
      StringReader reader = new StringReader(suggestionsBuilder.getInput());
      reader.setCursor(suggestionsBuilder.getStart());
      ErrorCollector.LongestOnly<StringReader> errorCollector = new ErrorCollector.LongestOnly<>();
      StringReaderParserState state = new StringReaderParserState(errorCollector, reader);
      this.parse(state);
      List<ErrorEntry<StringReader>> errorEntries = errorCollector.entries();
      if (errorEntries.isEmpty()) {
         return suggestionsBuilder.buildFuture();
      }

      SuggestionsBuilder offsetBuilder = suggestionsBuilder.createOffset(errorCollector.cursor());

      for (ErrorEntry<StringReader> entry : errorEntries) {
         if (entry.suggestions() instanceof ResourceSuggestion resourceSuggestionTerm) {
            SharedSuggestionProvider.suggestResource(resourceSuggestionTerm.possibleResources(), offsetBuilder);
         } else {
            SharedSuggestionProvider.suggest(entry.suggestions().possibleValues(state), offsetBuilder);
         }
      }

      return offsetBuilder.buildFuture();
   }
}
