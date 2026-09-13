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
   public static void register(CommandDispatcher<CommandSourceStack> p_139064_, CommandBuildContext p_327876_) {
      p_139064_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("tellraw").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(Commands.argument("message", ComponentArgument.textComponent(p_327876_)).executes(p_390113_ -> {
                     int i = 0;

                     for (ServerPlayer serverplayer : EntityArgument.getPlayers(p_390113_, "targets")) {
                        serverplayer.sendSystemMessage(ComponentArgument.getResolvedComponent(p_390113_, "message", serverplayer), false);
                        i++;
                     }

                     return i;
                  }))
            )
      );
   }
}
