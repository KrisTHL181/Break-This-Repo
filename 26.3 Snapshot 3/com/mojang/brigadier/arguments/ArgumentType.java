package com.mojang.brigadier.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public interface ArgumentType<T> {
   T parse(StringReader var1) throws CommandSyntaxException;

   default <S> T parse(StringReader reader, S source) throws CommandSyntaxException {
      return this.parse(reader);
   }

   default <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return Suggestions.empty();
   }

   default Collection<String> getExamples() {
      return Collections.emptyList();
   }
}
