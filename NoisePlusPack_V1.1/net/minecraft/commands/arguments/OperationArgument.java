package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.ScoreAccess;

public class OperationArgument implements ArgumentType<OperationArgument.Operation> {
   private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
   private static final SimpleCommandExceptionType ERROR_INVALID_OPERATION = new SimpleCommandExceptionType(
      Component.translatable("arguments.operation.invalid")
   );
   private static final SimpleCommandExceptionType ERROR_DIVIDE_BY_ZERO = new SimpleCommandExceptionType(Component.translatable("arguments.operation.div0"));

   public static OperationArgument operation() {
      return new OperationArgument();
   }

   public static OperationArgument.Operation getOperation(CommandContext<CommandSourceStack> p_103276_, String p_103277_) {
      return (OperationArgument.Operation)p_103276_.getArgument(p_103277_, OperationArgument.Operation.class);
   }

   public OperationArgument.Operation parse(StringReader p_103274_) throws CommandSyntaxException {
      if (!p_103274_.canRead()) {
         throw ERROR_INVALID_OPERATION.createWithContext(p_103274_);
      }

      int i = p_103274_.getCursor();

      while (p_103274_.canRead() && p_103274_.peek() != ' ') {
         p_103274_.skip();
      }

      return getOperation(p_103274_.getString().substring(i, p_103274_.getCursor()));
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_103302_, SuggestionsBuilder p_103303_) {
      return SharedSuggestionProvider.suggest(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, p_103303_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static OperationArgument.Operation getOperation(String p_103282_) throws CommandSyntaxException {
      return p_103282_.equals("><") ? (p_308356_, p_308357_) -> {
         int i = p_308356_.get();
         p_308356_.set(p_308357_.get());
         p_308357_.set(i);
      } : getSimpleOperation(p_103282_);
   }

   private static OperationArgument.SimpleOperation getSimpleOperation(String p_103287_) throws CommandSyntaxException {
      return switch (p_103287_) {
         case "=" -> (p_103298_, p_103299_) -> p_103299_;
         case "+=" -> Integer::sum;
         case "-=" -> (p_103292_, p_103293_) -> p_103292_ - p_103293_;
         case "*=" -> (p_103289_, p_103290_) -> p_103289_ * p_103290_;
         case "/=" -> (p_264713_, p_264714_) -> {
            if (p_264714_ == 0) {
               throw ERROR_DIVIDE_BY_ZERO.create();
            } else {
               return Mth.floorDiv(p_264713_, p_264714_);
            }
         };
         case "%=" -> (p_103271_, p_103272_) -> {
            if (p_103272_ == 0) {
               throw ERROR_DIVIDE_BY_ZERO.create();
            } else {
               return Mth.positiveModulo(p_103271_, p_103272_);
            }
         };
         case "<" -> Math::min;
         case ">" -> Math::max;
         default -> throw ERROR_INVALID_OPERATION.create();
      };
   }

   @FunctionalInterface
   public interface Operation {
      void apply(ScoreAccess var1, ScoreAccess var2) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface SimpleOperation extends OperationArgument.Operation {
      int apply(int var1, int var2) throws CommandSyntaxException;

      @Override
      default void apply(ScoreAccess p_311079_, ScoreAccess p_311087_) throws CommandSyntaxException {
         p_311079_.set(this.apply(p_311079_.get(), p_311087_.get()));
      }
   }
}
