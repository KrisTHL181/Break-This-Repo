package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public interface ArgProvider<T> {
   T access(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

   ArgumentBuilder<CommandSourceStack, ?> wrap(
      ArgumentBuilder<CommandSourceStack, ?> parent, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> function
   );

   static <T> ArgProvider<T> create(
      final String key, final Supplier<ArgumentBuilder<CommandSourceStack, ?>> child, final InCommandFunction<CommandContext<CommandSourceStack>, T> access
   ) {
      return new ArgProvider<T>() {
         @Override
         public T access(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            return access.apply(context);
         }

         @Override
         public ArgumentBuilder<CommandSourceStack, ?> wrap(
            final ArgumentBuilder<CommandSourceStack, ?> parent,
            final Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> function
         ) {
            return parent.then(Commands.literal(key).then(function.apply(child.get())));
         }
      };
   }

   static <T> List<ArgProvider<T>> buildList(final String argName, final List<ArgProvider.Factory<T>> factories) {
      Builder<ArgProvider<T>> result = ImmutableList.builderWithExpectedSize(factories.size());

      for (ArgProvider.Factory<T> factory : factories) {
         result.add(factory.create(argName));
      }

      return result.build();
   }

   @FunctionalInterface
   interface Factory<T> {
      ArgProvider<T> create(String arg);
   }
}
