package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;

public abstract class ParserBasedArgument<T> implements ArgumentType<T> {
   private final CommandArgumentParser<T> parser;

   public ParserBasedArgument(final CommandArgumentParser<T> parser) {
      this.parser = parser;
   }

   public T parse(final StringReader reader) throws CommandSyntaxException {
      return this.parser.parseForCommands(reader);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return this.parser.parseForSuggestions(builder);
   }
}
