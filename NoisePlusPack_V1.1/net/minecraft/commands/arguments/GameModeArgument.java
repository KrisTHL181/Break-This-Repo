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
      p_308349_ -> Component.translatableEscape("argument.gamemode.invalid", p_308349_)
   );

   public GameType parse(StringReader p_260111_) throws CommandSyntaxException {
      String s = p_260111_.readUnquotedString();
      GameType gametype = GameType.byName(s, null);
      if (gametype == null) {
         throw ERROR_INVALID.createWithContext(p_260111_, s);
      } else {
         return gametype;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_259767_, SuggestionsBuilder p_259515_) {
      return p_259767_.getSource() instanceof SharedSuggestionProvider
         ? SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(GameType::getName), p_259515_)
         : Suggestions.empty();
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static GameModeArgument gameMode() {
      return new GameModeArgument();
   }

   public static GameType getGameMode(CommandContext<CommandSourceStack> p_259927_, String p_260246_) throws CommandSyntaxException {
      return (GameType)p_259927_.getArgument(p_260246_, GameType.class);
   }
}
