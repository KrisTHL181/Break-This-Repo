package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class MsgCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> msg = dispatcher.register(
            Commands.literal("msg")
                .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message()).executes(c -> {
                    Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
                    if (!players.isEmpty()) {
                        MessageArgument.resolveChatMessage(c, "message", message -> sendMessage(c.getSource(), players, message));
                    }

                    return players.size();
                })))
        );
        dispatcher.register(Commands.literal("tell").redirect(msg));
        dispatcher.register(Commands.literal("w").redirect(msg));
    }

    private static void sendMessage(final CommandSourceStack source, final Collection<ServerPlayer> players, final PlayerChatMessage message) {
        ChatType.Bound incomingChatType = ChatType.bind(ChatType.MSG_COMMAND_INCOMING, source);
        OutgoingChatMessage tracked = OutgoingChatMessage.create(message);
        boolean wasFullyFiltered = false;

        for (ServerPlayer player : players) {
            ChatType.Bound outgoingChatType = ChatType.bind(ChatType.MSG_COMMAND_OUTGOING, source).withTargetName(player.getDisplayName());
            source.sendChatMessage(tracked, false, outgoingChatType);
            boolean filtered = source.shouldFilterMessageTo(player);
            player.sendChatMessage(tracked, filtered, incomingChatType);
            wasFullyFiltered |= filtered && message.isFullyFiltered();
        }

        if (wasFullyFiltered) {
            source.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
        }
    }
}