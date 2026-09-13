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

   public static void register(CommandDispatcher<CommandSourceStack> p_139000_) {
      LiteralCommandNode<CommandSourceStack> literalcommandnode = p_139000_.register(
         (LiteralArgumentBuilder)Commands.literal("teammsg")
            .then(
               Commands.argument("message", MessageArgument.message())
                  .executes(
                     p_248184_ -> {
                        CommandSourceStack commandsourcestack = (CommandSourceStack)p_248184_.getSource();
                        Entity entity = commandsourcestack.getEntityOrException();
                        PlayerTeam playerteam = entity.getTeam();
                        if (playerteam == null) {
                           throw ERROR_NOT_ON_TEAM.create();
                        }

                        List<ServerPlayer> list = commandsourcestack.getServer()
                           .getPlayerList()
                           .getPlayers()
                           .stream()
                           .filter(p_449043_ -> p_449043_ == entity || p_449043_.getTeam() == playerteam)
                           .toList();
                        if (!list.isEmpty()) {
                           MessageArgument.resolveChatMessage(
                              p_248184_, "message", p_248180_ -> sendMessage(commandsourcestack, entity, playerteam, list, p_248180_)
                           );
                        }

                        return list.size();
                     }
                  )
            )
      );
      p_139000_.register((LiteralArgumentBuilder)Commands.literal("tm").redirect(literalcommandnode));
   }

   private static void sendMessage(
      CommandSourceStack p_248778_, Entity p_248891_, PlayerTeam p_250504_, List<ServerPlayer> p_249706_, PlayerChatMessage p_249707_
   ) {
      Component component = p_250504_.getFormattedDisplayName().withStyle(SUGGEST_STYLE);
      ChatType.Bound chattype$bound = ChatType.bind(ChatType.TEAM_MSG_COMMAND_INCOMING, p_248778_).withTargetName(component);
      ChatType.Bound chattype$bound1 = ChatType.bind(ChatType.TEAM_MSG_COMMAND_OUTGOING, p_248778_).withTargetName(component);
      OutgoingChatMessage outgoingchatmessage = OutgoingChatMessage.create(p_249707_);
      boolean flag = false;

      for (ServerPlayer serverplayer : p_249706_) {
         ChatType.Bound chattype$bound2 = serverplayer == p_248891_ ? chattype$bound1 : chattype$bound;
         boolean flag1 = p_248778_.shouldFilterMessageTo(serverplayer);
         serverplayer.sendChatMessage(outgoingchatmessage, flag1, chattype$bound2);
         flag |= flag1 && p_249707_.isFullyFiltered();
      }

      if (flag) {
         p_248778_.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
      }
   }
}
