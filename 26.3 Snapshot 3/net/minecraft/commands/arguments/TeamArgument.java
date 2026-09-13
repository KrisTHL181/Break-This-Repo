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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class TeamArgument implements ArgumentType<String> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "123");
   private static final DynamicCommandExceptionType ERROR_TEAM_NOT_FOUND = new DynamicCommandExceptionType(
      name -> Component.translatableEscape("team.notFound", name)
   );

   public static TeamArgument team() {
      return new TeamArgument();
   }

   public static PlayerTeam getTeam(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      String id = (String)context.getArgument(name, String.class);
      Scoreboard scoreboard = ((CommandSourceStack)context.getSource()).getServer().getScoreboard();
      PlayerTeam team = scoreboard.getPlayerTeam(id);
      if (team == null) {
         throw ERROR_TEAM_NOT_FOUND.create(id);
      } else {
         return team;
      }
   }

   public String parse(final StringReader reader) throws CommandSyntaxException {
      return reader.readUnquotedString();
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> contextBuilder, final SuggestionsBuilder builder) {
      return contextBuilder.getSource() instanceof SharedSuggestionProvider
         ? SharedSuggestionProvider.suggest(((SharedSuggestionProvider)contextBuilder.getSource()).getAllTeams(), builder)
         : Suggestions.empty();
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
