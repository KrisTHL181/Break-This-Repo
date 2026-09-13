package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface CommandArgumentParser<T> {
   T parseForCommands(StringReader var1) throws CommandSyntaxException;

   CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder var1);

   default <S> CommandArgumentParser<S> mapResult(final Function<T, S> p_397761_) {
      return new CommandArgumentParser<S>() {
         @Override
         public S parseForCommands(StringReader p_393564_) throws CommandSyntaxException {
            return p_397761_.apply((T)CommandArgumentParser.this.parseForCommands(p_393564_));
         }

         @Override
         public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder p_395812_) {
            return CommandArgumentParser.this.parseForSuggestions(p_395812_);
         }
      };
   }

   default <T, O> CommandArgumentParser<T> withCodec(
      final DynamicOps<O> p_396478_, final CommandArgumentParser<O> p_394585_, final Codec<T> p_394654_, final DynamicCommandExceptionType p_391576_
   ) {
      return new CommandArgumentParser<T>() {
         @Override
         public T parseForCommands(StringReader p_391748_) throws CommandSyntaxException {
            int i = p_391748_.getCursor();
            O o = p_394585_.parseForCommands(p_391748_);
            DataResult<T> dataresult = p_394654_.parse(p_396478_, o);
            return (T)dataresult.getOrThrow(p_394070_ -> {
               p_391748_.setCursor(i);
               return p_391576_.createWithContext(p_391748_, p_394070_);
            });
         }

         @Override
         public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder p_393320_) {
            return CommandArgumentParser.this.parseForSuggestions(p_393320_);
         }
      };
   }
}
