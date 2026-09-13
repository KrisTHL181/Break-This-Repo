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

   public ParserBasedArgument(CommandArgumentParser<T> p_392092_) {
      this.parser = p_392092_;
   }

   public T parse(StringReader p_393710_) throws CommandSyntaxException {
      return this.parser.parseForCommands(p_393710_);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_396816_, SuggestionsBuilder p_393526_) {
      return this.parser.parseForSuggestions(p_393526_);
   }
}
