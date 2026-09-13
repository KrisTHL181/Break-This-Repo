package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SetPlayerIdleTimeoutCommand {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setidletimeout").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               Commands.argument("minutes", IntegerArgumentType.integer(0))
                  .executes(c -> setIdleTimeout((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger(c, "minutes")))
            )
      );
   }

   private static int setIdleTimeout(final CommandSourceStack source, final int time) {
      source.getServer().setPlayerIdleTimeout(time);
      if (time > 0) {
         source.sendSuccess(() -> Component.translatable("commands.setidletimeout.success", time), true);
      } else {
         source.sendSuccess(() -> Component.translatable("commands.setidletimeout.success.disabled"), true);
      }

      return time;
   }
}
