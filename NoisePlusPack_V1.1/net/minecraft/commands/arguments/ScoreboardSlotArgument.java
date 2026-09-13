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
      p_308385_ -> Component.translatableEscape("argument.scoreboardDisplaySlot.invalid", p_308385_)
   );

   private ScoreboardSlotArgument() {
   }

   public static ScoreboardSlotArgument displaySlot() {
      return new ScoreboardSlotArgument();
   }

   public static DisplaySlot getDisplaySlot(CommandContext<CommandSourceStack> p_109200_, String p_109201_) {
      return (DisplaySlot)p_109200_.getArgument(p_109201_, DisplaySlot.class);
   }

   public DisplaySlot parse(StringReader p_109198_) throws CommandSyntaxException {
      String s = p_109198_.readUnquotedString();
      DisplaySlot displayslot = DisplaySlot.CODEC.byName(s);
      if (displayslot == null) {
         throw ERROR_INVALID_VALUE.createWithContext(p_109198_, s);
      } else {
         return displayslot;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_109206_, SuggestionsBuilder p_109207_) {
      return SharedSuggestionProvider.suggest(Arrays.stream(DisplaySlot.values()).map(DisplaySlot::getSerializedName), p_109207_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
