package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.players.PlayerList;

public class EmoteCommands {
   public static void register(CommandDispatcher<CommandSourceStack> p_136986_) {
      p_136986_.register((LiteralArgumentBuilder)Commands.literal("me").then(Commands.argument("action", MessageArgument.message()).executes(p_248130_ -> {
         MessageArgument.resolveChatMessage(p_248130_, "action", p_248129_ -> {
            CommandSourceStack commandsourcestack = (CommandSourceStack)p_248130_.getSource();
            PlayerList playerlist = commandsourcestack.getServer().getPlayerList();
            playerlist.broadcastChatMessage(p_248129_, commandsourcestack, ChatType.bind(ChatType.EMOTE_COMMAND, commandsourcestack));
         });
         return 1;
      })));
   }
}
