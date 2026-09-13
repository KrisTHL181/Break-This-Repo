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
import net.minecraft.world.scores.DisplaySlot;

public class ScoreboardSlotArgument implements ArgumentType<DisplaySlot> {
   private static final Collection<String> EXAMPLES = Arrays.asList("sidebar", "foo.bar");
   public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("argument.scoreboardDisplaySlot.invalid", value)
   );

   private ScoreboardSlotArgument() {
   }

   public static ScoreboardSlotArgument displaySlot() {
      return new ScoreboardSlotArgument();
   }

   public static DisplaySlot getDisplaySlot(final CommandContext<CommandSourceStack> context, final String name) {
      return (DisplaySlot)context.getArgument(name, DisplaySlot.class);
   }

   public DisplaySlot parse(final StringReader reader) throws CommandSyntaxException {
      String name = reader.readUnquotedString();
      DisplaySlot result = DisplaySlot.CODEC.byName(name);
      if (result == null) {
         throw ERROR_INVALID_VALUE.createWithContext(reader, name);
      } else {
         return result;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggest(Arrays.stream(DisplaySlot.values()).map(DisplaySlot::getSerializedName), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
