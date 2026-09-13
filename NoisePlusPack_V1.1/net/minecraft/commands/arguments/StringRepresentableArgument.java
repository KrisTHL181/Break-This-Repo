package net.minecraft.commands.arguments;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public class StringRepresentableArgument<T extends Enum<T> & StringRepresentable> implements ArgumentType<T> {
   private static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
      p_308387_ -> Component.translatableEscape("argument.enum.invalid", p_308387_)
   );
   private final Codec<T> codec;
   private final Supplier<T[]> values;

   protected StringRepresentableArgument(Codec<T> p_234060_, Supplier<T[]> p_234061_) {
      this.codec = p_234060_;
      this.values = p_234061_;
   }

   public T parse(StringReader p_234063_) throws CommandSyntaxException {
      String s = p_234063_.readUnquotedString();
      return (T)this.codec.parse(JsonOps.INSTANCE, new JsonPrimitive(s)).result().orElseThrow(() -> ERROR_INVALID_VALUE.createWithContext(p_234063_, s));
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_234074_, SuggestionsBuilder p_234075_) {
      return SharedSuggestionProvider.suggest(
         Arrays.<Enum>stream((Enum[])this.values.get())
            .map(p_234069_ -> ((StringRepresentable)p_234069_).getSerializedName())
            .map(this::convertId)
            .collect(Collectors.toList()),
         p_234075_
      );
   }

   public Collection<String> getExamples() {
      return Arrays.<Enum>stream((Enum[])this.values.get())
         .map(p_234065_ -> ((StringRepresentable)p_234065_).getSerializedName())
         .map(this::convertId)
         .limit(2L)
         .collect(Collectors.toList());
   }

   protected String convertId(String p_275436_) {
      return p_275436_;
   }
}
