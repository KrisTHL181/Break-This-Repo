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
import net.minecraft.commands.ParserUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;

public class SlotArgument implements ArgumentType<Integer> {
   private static final Collection<String> EXAMPLES = Arrays.asList("container.5", "weapon");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_SLOT = new DynamicCommandExceptionType(
      p_308386_ -> Component.translatableEscape("slot.unknown", p_308386_)
   );
   private static final DynamicCommandExceptionType ERROR_ONLY_SINGLE_SLOT_ALLOWED = new DynamicCommandExceptionType(
      p_325606_ -> Component.translatableEscape("slot.only_single_allowed", p_325606_)
   );

   public static SlotArgument slot() {
      return new SlotArgument();
   }

   public static int getSlot(CommandContext<CommandSourceStack> p_111280_, String p_111281_) {
      return (Integer)p_111280_.getArgument(p_111281_, Integer.class);
   }

   public Integer parse(StringReader p_111278_) throws CommandSyntaxException {
      String s = ParserUtils.readWhile(p_111278_, p_325605_ -> p_325605_ != ' ');
      SlotRange slotrange = SlotRanges.nameToIds(s);
      if (slotrange == null) {
         throw ERROR_UNKNOWN_SLOT.createWithContext(p_111278_, s);
      } else if (slotrange.size() != 1) {
         throw ERROR_ONLY_SINGLE_SLOT_ALLOWED.createWithContext(p_111278_, s);
      } else {
         return slotrange.slots().getInt(0);
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_111288_, SuggestionsBuilder p_111289_) {
      return SharedSuggestionProvider.suggest(SlotRanges.singleSlotNames(), p_111289_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
