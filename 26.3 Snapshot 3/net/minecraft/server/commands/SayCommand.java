package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.players.PlayerList;

public class SayCommand {
   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("say").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(Commands.argument("message", MessageArgument.message()).executes(c -> {
               MessageArgument.resolveChatMessage(c, "message", message -> {
                  CommandSourceStack source = (CommandSourceStack)c.getSource();
                  PlayerList playerList = source.getServer().getPlayerList();
                  playerList.broadcastChatMessage(message, source, ChatType.bind(ChatType.SAY_COMMAND, source));
               });
               return 1;
            }))
      );
   }
}
