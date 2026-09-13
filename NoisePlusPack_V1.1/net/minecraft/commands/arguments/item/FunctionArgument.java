package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FunctionArgument implements ArgumentType<FunctionArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "#foo");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
      p_308403_ -> Component.translatableEscape("arguments.function.tag.unknown", p_308403_)
   );
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_FUNCTION = new DynamicCommandExceptionType(
      p_308402_ -> Component.translatableEscape("arguments.function.unknown", p_308402_)
   );

   public static FunctionArgument functions() {
      return new FunctionArgument();
   }

   public FunctionArgument.Result parse(StringReader p_120909_) throws CommandSyntaxException {
      if (p_120909_.canRead() && p_120909_.peek() == '#') {
         p_120909_.skip();
         final Identifier identifier1 = Identifier.read(p_120909_);
         return new FunctionArgument.Result() {
            @Override
            public Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> p_120943_) throws CommandSyntaxException {
               return FunctionArgument.getFunctionTag(p_120943_, identifier1);
            }

            @Override
            public Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(
               CommandContext<CommandSourceStack> p_120945_
            ) throws CommandSyntaxException {
               return Pair.of(identifier1, Either.right(FunctionArgument.getFunctionTag(p_120945_, identifier1)));
            }

            @Override
            public Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> p_310998_) throws CommandSyntaxException {
               return Pair.of(identifier1, FunctionArgument.getFunctionTag(p_310998_, identifier1));
            }
         };
      } else {
         final Identifier identifier = Identifier.read(p_120909_);
         return new FunctionArgument.Result() {
            @Override
            public Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> p_120952_) throws CommandSyntaxException {
               return Collections.singleton(FunctionArgument.getFunction(p_120952_, identifier));
            }

            @Override
            public Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(
               CommandContext<CommandSourceStack> p_120954_
            ) throws CommandSyntaxException {
               return Pair.of(identifier, Either.left(FunctionArgument.getFunction(p_120954_, identifier)));
            }

            @Override
            public Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> p_310823_) throws CommandSyntaxException {
               return Pair.of(identifier, Collections.singleton(FunctionArgument.getFunction(p_310823_, identifier)));
            }
         };
      }
   }

   static CommandFunction<CommandSourceStack> getFunction(CommandContext<CommandSourceStack> p_120929_, Identifier p_455973_) throws CommandSyntaxException {
      return ((CommandSourceStack)p_120929_.getSource())
         .getServer()
         .getFunctions()
         .get(p_455973_)
         .orElseThrow(() -> ERROR_UNKNOWN_FUNCTION.create(p_455973_.toString()));
   }

   static Collection<CommandFunction<CommandSourceStack>> getFunctionTag(CommandContext<CommandSourceStack> p_235274_, Identifier p_453196_) throws CommandSyntaxException {
      Collection<CommandFunction<CommandSourceStack>> collection = ((CommandSourceStack)p_235274_.getSource()).getServer().getFunctions().getTag(p_453196_);
      if (collection == null) {
         throw ERROR_UNKNOWN_TAG.create(p_453196_.toString());
      } else {
         return collection;
      }
   }

   public static Collection<CommandFunction<CommandSourceStack>> getFunctions(CommandContext<CommandSourceStack> p_120911_, String p_120912_) throws CommandSyntaxException {
      return ((FunctionArgument.Result)p_120911_.getArgument(p_120912_, FunctionArgument.Result.class)).create(p_120911_);
   }

   public static Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> getFunctionOrTag(
      CommandContext<CommandSourceStack> p_120921_, String p_120922_
   ) throws CommandSyntaxException {
      return ((FunctionArgument.Result)p_120921_.getArgument(p_120922_, FunctionArgument.Result.class)).unwrap(p_120921_);
   }

   public static Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> getFunctionCollection(
      CommandContext<CommandSourceStack> p_312555_, String p_311726_
   ) throws CommandSyntaxException {
      return ((FunctionArgument.Result)p_312555_.getArgument(p_311726_, FunctionArgument.Result.class)).unwrapToCollection(p_312555_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public interface Result {
      Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;

      Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(
         CommandContext<CommandSourceStack> var1
      ) throws CommandSyntaxException;

      Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
   }
}
