package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;

public class TeamMsgCommand {
   private static final Style SUGGEST_STYLE = Style.EMPTY
      .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.type.team.hover")))
      .withClickEvent(new ClickEvent.SuggestCommand("/teammsg "));
   private static final SimpleCommandExceptionType ERROR_NOT_ON_TEAM = new SimpleCommandExceptionType(Component.translatable("commands.teammsg.failed.noteam"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralCommandNode<CommandSourceStack> msg = dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("teammsg")
            .then(
               Commands.argument("message", MessageArgument.message())
                  .executes(
                     c -> {
                        CommandSourceStack source = (CommandSourceStack)c.getSource();
                        Entity entity = source.getEntityOrException();
                        PlayerTeam team = entity.getTeam();
                        if (team == null) {
                           throw ERROR_NOT_ON_TEAM.create();
                        }

                        List<ServerPlayer> receivers = source.getServer()
                           .getPlayerList()
                           .getPlayers()
                           .stream()
                           .filter(receiver -> receiver == entity || receiver.getTeam() == team)
                           .toList();
                        if (!receivers.isEmpty()) {
                           MessageArgument.resolveChatMessage(c, "message", message -> sendMessage(source, entity, team, receivers, message));
                        }

                        return receivers.size();
                     }
                  )
            )
      );
      dispatcher.register((LiteralArgumentBuilder)Commands.literal("tm").redirect(msg));
   }

   private static void sendMessage(
      final CommandSourceStack source, final Entity entity, final PlayerTeam team, final List<ServerPlayer> receivers, final PlayerChatMessage message
   ) {
      Component teamName = team.getFormattedDisplayName().withStyle(SUGGEST_STYLE);
      ChatType.Bound incomingChatType = ChatType.bind(ChatType.TEAM_MSG_COMMAND_INCOMING, source).withTargetName(teamName);
      ChatType.Bound outgoingChatType = ChatType.bind(ChatType.TEAM_MSG_COMMAND_OUTGOING, source).withTargetName(teamName);
      OutgoingChatMessage tracked = OutgoingChatMessage.create(message);
      boolean wasFullyFiltered = false;

      for (ServerPlayer teamPlayer : receivers) {
         ChatType.Bound chatType = teamPlayer == entity ? outgoingChatType : incomingChatType;
         boolean filtered = source.shouldFilterMessageTo(teamPlayer);
         teamPlayer.sendChatMessage(tracked, filtered, chatType);
         wasFullyFiltered |= filtered && message.isFullyFiltered();
      }

      if (wasFullyFiltered) {
         source.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
      }
   }
}
