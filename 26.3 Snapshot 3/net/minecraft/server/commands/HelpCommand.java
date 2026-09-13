package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class HelpCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.help.failed"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("help").executes(s -> {
               Map<CommandNode<CommandSourceStack>, String> usage = dispatcher.getSmartUsage(dispatcher.getRoot(), (CommandSourceStack)s.getSource());

               for (String line : usage.values()) {
                  ((CommandSourceStack)s.getSource()).sendSuccess(() -> Component.literal("/" + line), false);
               }

               return usage.size();
            }))
            .then(
               Commands.argument("command", StringArgumentType.greedyString())
                  .executes(
                     s -> {
                        ParseResults<CommandSourceStack> command = dispatcher.parse(
                           StringArgumentType.getString(s, "command"), (CommandSourceStack)s.getSource()
                        );
                        if (command.getContext().getNodes().isEmpty()) {
                           throw ERROR_FAILED.create();
                        }

                        Map<CommandNode<CommandSourceStack>, String> usage = dispatcher.getSmartUsage(
                           ((ParsedCommandNode)Iterables.getLast(command.getContext().getNodes())).getNode(), (CommandSourceStack)s.getSource()
                        );

                        for (String line : usage.values()) {
                           ((CommandSourceStack)s.getSource()).sendSuccess(() -> Component.literal("/" + command.getReader().getString() + " " + line), false);
                        }

                        return usage.size();
                     }
                  )
            )
      );
   }
}
