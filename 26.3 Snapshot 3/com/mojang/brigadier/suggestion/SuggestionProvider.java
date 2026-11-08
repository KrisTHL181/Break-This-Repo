package com.mojang.brigadier.suggestion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface SuggestionProvider<S> {
   CompletableFuture<Suggestions> getSuggestions(CommandContext<S> var1, SuggestionsBuilder var2) throws CommandSyntaxException;
}
