package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.ParserUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;

public class SlotsArgument implements ArgumentType<SlotRange> {
   private static final Collection<String> EXAMPLES = List.of("container.*", "container.5", "weapon");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_SLOT = new DynamicCommandExceptionType(
      p_331324_ -> Component.translatableEscape("slot.unknown", p_331324_)
   );

   public static SlotsArgument slots() {
      return new SlotsArgument();
   }

   public static SlotRange getSlots(CommandContext<CommandSourceStack> p_333218_, String p_328819_) {
      return (SlotRange)p_333218_.getArgument(p_328819_, SlotRange.class);
   }

   public SlotRange parse(StringReader p_329039_) throws CommandSyntaxException {
      String s = ParserUtils.readWhile(p_329039_, p_329908_ -> p_329908_ != ' ');
      SlotRange slotrange = SlotRanges.nameToIds(s);
      if (slotrange == null) {
         throw ERROR_UNKNOWN_SLOT.createWithContext(p_329039_, s);
      } else {
         return slotrange;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_329445_, SuggestionsBuilder p_329636_) {
      return SharedSuggestionProvider.suggest(SlotRanges.allNames(), p_329636_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
