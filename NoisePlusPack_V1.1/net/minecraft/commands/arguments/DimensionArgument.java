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
      .map(p_448480_ -> p_448480_.identifier().toString())
      .collect(Collectors.toList());
   private static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
      p_308347_ -> Component.translatableEscape("argument.dimension.invalid", p_308347_)
   );

   public Identifier parse(StringReader p_88807_) throws CommandSyntaxException {
      return Identifier.read(p_88807_);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_88817_, SuggestionsBuilder p_88818_) {
      return p_88817_.getSource() instanceof SharedSuggestionProvider
         ? SharedSuggestionProvider.suggestResource(((SharedSuggestionProvider)p_88817_.getSource()).levels().stream().map(ResourceKey::identifier), p_88818_)
         : Suggestions.empty();
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static DimensionArgument dimension() {
      return new DimensionArgument();
   }

   public static ServerLevel getDimension(CommandContext<CommandSourceStack> p_88809_, String p_88810_) throws CommandSyntaxException {
      Identifier identifier = (Identifier)p_88809_.getArgument(p_88810_, Identifier.class);
      ResourceKey<Level> resourcekey = ResourceKey.create(Registries.DIMENSION, identifier);
      ServerLevel serverlevel = ((CommandSourceStack)p_88809_.getSource()).getServer().getLevel(resourcekey);
      if (serverlevel == null) {
         throw ERROR_INVALID_VALUE.create(identifier);
      } else {
         return serverlevel;
      }
   }
}
