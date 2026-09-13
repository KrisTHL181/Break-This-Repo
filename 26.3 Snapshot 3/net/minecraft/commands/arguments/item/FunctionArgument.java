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
      tag -> Component.translatableEscape("arguments.function.tag.unknown", tag)
   );
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_FUNCTION = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("arguments.function.unknown", value)
   );

   public static FunctionArgument functions() {
      return new FunctionArgument();
   }

   public FunctionArgument.Result parse(final StringReader reader) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '#') {
         reader.skip();
         final Identifier id = Identifier.read(reader);
         return new FunctionArgument.Result() {
            @Override
            public Collection<CommandFunction<CommandSourceStack>> create(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
               return FunctionArgument.getFunctionTag(c, id);
            }

            @Override
            public Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(
               final CommandContext<CommandSourceStack> context
            ) throws CommandSyntaxException {
               return Pair.of(id, Either.right(FunctionArgument.getFunctionTag(context, id)));
            }

            @Override
            public Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
               return Pair.of(id, FunctionArgument.getFunctionTag(context, id));
            }
         };
      } else {
         final Identifier id = Identifier.read(reader);
         return new FunctionArgument.Result() {
            @Override
            public Collection<CommandFunction<CommandSourceStack>> create(final CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
               return Collections.singleton(FunctionArgument.getFunction(c, id));
            }

            @Override
            public Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(
               final CommandContext<CommandSourceStack> context
            ) throws CommandSyntaxException {
               return Pair.of(id, Either.left(FunctionArgument.getFunction(context, id)));
            }

            @Override
            public Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
               return Pair.of(id, Collections.singleton(FunctionArgument.getFunction(context, id)));
            }
         };
      }
   }

   private static CommandFunction<CommandSourceStack> getFunction(final CommandContext<CommandSourceStack> c, final Identifier id) throws CommandSyntaxException {
      return ((CommandSourceStack)c.getSource()).getServer().getFunctions().get(id).orElseThrow(() -> ERROR_UNKNOWN_FUNCTION.create(id.toString()));
   }

   private static Collection<CommandFunction<CommandSourceStack>> getFunctionTag(final CommandContext<CommandSourceStack> c, final Identifier id) throws CommandSyntaxException {
      Collection<CommandFunction<CommandSourceStack>> tag = ((CommandSourceStack)c.getSource()).getServer().getFunctions().getTag(id);
      if (tag == null) {
         throw ERROR_UNKNOWN_TAG.create(id.toString());
      } else {
         return tag;
      }
   }

   public static Collection<CommandFunction<CommandSourceStack>> getFunctions(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return ((FunctionArgument.Result)context.getArgument(name, FunctionArgument.Result.class)).create(context);
   }

   public static Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> getFunctionOrTag(
      final CommandContext<CommandSourceStack> context, final String name
   ) throws CommandSyntaxException {
      return ((FunctionArgument.Result)context.getArgument(name, FunctionArgument.Result.class)).unwrap(context);
   }

   public static Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> getFunctionCollection(
      final CommandContext<CommandSourceStack> context, final String name
   ) throws CommandSyntaxException {
      return ((FunctionArgument.Result)context.getArgument(name, FunctionArgument.Result.class)).unwrapToCollection(context);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public interface Result {
      Collection<CommandFunction<CommandSourceStack>> create(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

      Pair<Identifier, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> unwrap(
         CommandContext<CommandSourceStack> context
      ) throws CommandSyntaxException;

      Pair<Identifier, Collection<CommandFunction<CommandSourceStack>>> unwrapToCollection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
   }
}
