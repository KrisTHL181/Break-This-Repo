package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class DimensionArgument implements ArgumentType<Identifier> {
   private static final Collection<String> EXAMPLES = Stream.of(Level.OVERWORLD, Level.NETHER)
      .map(key -> key.identifier().toString())
      .collect(Collectors.toList());
   private static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("argument.dimension.invalid", value)
   );

   public Identifier parse(final StringReader reader) throws CommandSyntaxException {
      return Identifier.read(reader);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
         ? SharedSuggestionProvider.suggestResource(((SharedSuggestionProvider)context.getSource()).levels().stream().map(ResourceKey::identifier), builder)
         : Suggestions.empty();
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static DimensionArgument dimension() {
      return new DimensionArgument();
   }

   public static ServerLevel getDimension(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      Identifier location = (Identifier)context.getArgument(name, Identifier.class);
      ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
      ServerLevel level = ((CommandSourceStack)context.getSource()).getServer().getLevel(key);
      if (level == null) {
         throw ERROR_INVALID_VALUE.create(location);
      } else {
         return level;
      }
   }
}
