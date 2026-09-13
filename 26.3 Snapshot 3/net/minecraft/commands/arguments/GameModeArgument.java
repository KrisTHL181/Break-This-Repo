package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

public class GameModeArgument implements ArgumentType<GameType> {
   private static final Collection<String> EXAMPLES = Stream.of(GameType.SURVIVAL, GameType.CREATIVE).map(GameType::getName).collect(Collectors.toList());
   private static final GameType[] VALUES = GameType.values();
   private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("argument.gamemode.invalid", value)
   );

   public GameType parse(final StringReader reader) throws CommandSyntaxException {
      String gameTypeString = reader.readUnquotedString();
      GameType gameType = GameType.byName(gameTypeString, null);
      if (gameType == null) {
         throw ERROR_INVALID.createWithContext(reader, gameTypeString);
      } else {
         return gameType;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
         ? SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(GameType::getName), builder)
         : Suggestions.empty();
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static GameModeArgument gameMode() {
      return new GameModeArgument();
   }

   public static GameType getGameMode(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return (GameType)context.getArgument(name, GameType.class);
   }
}
