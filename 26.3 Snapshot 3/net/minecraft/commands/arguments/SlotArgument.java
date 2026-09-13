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
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;

public class SlotArgument implements ArgumentType<Integer> {
   private static final Collection<String> EXAMPLES = Arrays.asList("container.5", "weapon");
   private static final DynamicCommandExceptionType ERROR_ONLY_SINGLE_SLOT_ALLOWED = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("slot.only_single_allowed", id)
   );

   public static SlotArgument slot() {
      return new SlotArgument();
   }

   public static int getSlot(final CommandContext<CommandSourceStack> context, final String name) {
      return (Integer)context.getArgument(name, Integer.class);
   }

   public Integer parse(final StringReader reader) throws CommandSyntaxException {
      SlotRange result = SlotRanges.read(reader);
      if (result.size() != 1) {
         throw ERROR_ONLY_SINGLE_SLOT_ALLOWED.createWithContext(reader, result.getSerializedName());
      } else {
         return result.slots().getInt(0);
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> contextBuilder, final SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggest(SlotRanges.singleSlotNames(), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
