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
import net.minecraft.util.ARGB;

public class HexColorArgument implements ArgumentType<Integer> {
   private static final Collection<String> EXAMPLES = Arrays.asList("F00", "FF0000");
   public static final DynamicCommandExceptionType ERROR_INVALID_HEX = new DynamicCommandExceptionType(
      p_407117_ -> Component.translatableEscape("argument.hexcolor.invalid", p_407117_)
   );

   private HexColorArgument() {
   }

   public static HexColorArgument hexColor() {
      return new HexColorArgument();
   }

   public static Integer getHexColor(CommandContext<CommandSourceStack> p_407032_, String p_407090_) {
      return (Integer)p_407032_.getArgument(p_407090_, Integer.class);
   }

   public Integer parse(StringReader p_406115_) throws CommandSyntaxException {
      String s = p_406115_.readUnquotedString();

      return switch (s.length()) {
         case 3 -> ARGB.color(
            duplicateDigit(Integer.parseInt(s, 0, 1, 16)), duplicateDigit(Integer.parseInt(s, 1, 2, 16)), duplicateDigit(Integer.parseInt(s, 2, 3, 16))
         );
         case 6 -> ARGB.color(Integer.parseInt(s, 0, 2, 16), Integer.parseInt(s, 2, 4, 16), Integer.parseInt(s, 4, 6, 16));
         default -> throw ERROR_INVALID_HEX.createWithContext(p_406115_, s);
      };
   }

   private static int duplicateDigit(int p_427017_) {
      return p_427017_ * 17;
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_409242_, SuggestionsBuilder p_410053_) {
      return SharedSuggestionProvider.suggest(EXAMPLES, p_410053_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
