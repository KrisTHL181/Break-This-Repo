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
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public class ObjectiveArgument implements ArgumentType<String> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "*", "012");
   private static final DynamicCommandExceptionType ERROR_OBJECTIVE_NOT_FOUND = new DynamicCommandExceptionType(
      name -> Component.translatableEscape("arguments.objective.notFound", name)
   );
   private static final DynamicCommandExceptionType ERROR_OBJECTIVE_READ_ONLY = new DynamicCommandExceptionType(
      name -> Component.translatableEscape("arguments.objective.readonly", name)
   );

   public static ObjectiveArgument objective() {
      return new ObjectiveArgument();
   }

   public static Objective getObjective(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      String id = (String)context.getArgument(name, String.class);
      Scoreboard scoreboard = ((CommandSourceStack)context.getSource()).getServer().getScoreboard();
      Objective objective = scoreboard.getObjective(id);
      if (objective == null) {
         throw ERROR_OBJECTIVE_NOT_FOUND.create(id);
      } else {
         return objective;
      }
   }

   public static Objective getWritableObjective(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      Objective objective = getObjective(context, name);
      if (objective.getCriteria().isReadOnly()) {
         throw ERROR_OBJECTIVE_READ_ONLY.create(objective.getName());
      } else {
         return objective;
      }
   }

   public String parse(final StringReader reader) throws CommandSyntaxException {
      return reader.readUnquotedString();
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      S rawSource = (S)context.getSource();
      if (rawSource instanceof CommandSourceStack source) {
         return SharedSuggestionProvider.suggest(source.getServer().getScoreboard().getObjectiveNames(), builder);
      } else {
         return rawSource instanceof SharedSuggestionProvider source ? source.customSuggestion(context) : Suggestions.empty();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
