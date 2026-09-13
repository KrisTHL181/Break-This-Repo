package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class TellRawCommand {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("tellraw").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(Commands.argument("message", ComponentArgument.textComponent(context)).executes(c -> {
                     int result = 0;

                     for (ServerPlayer player : EntityArgument.getPlayers(c, "targets")) {
                        player.sendSystemMessage(ComponentArgument.getResolvedComponent(c, "message", player));
                        result++;
                     }

                     return result;
                  }))
            )
      );
   }
}
