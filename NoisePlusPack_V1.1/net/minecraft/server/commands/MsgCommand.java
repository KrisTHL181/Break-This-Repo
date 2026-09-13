package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
   public static void register(CommandDispatcher<CommandSourceStack> p_138061_) {
      LiteralCommandNode<CommandSourceStack> literalcommandnode = p_138061_.register(
         (LiteralArgumentBuilder)Commands.literal("msg")
            .then(
               Commands.argument("targets", EntityArgument.players())
                  .then(
                     Commands.argument("message", MessageArgument.message())
                        .executes(
                           p_248155_ -> {
                              Collection<ServerPlayer> collection = EntityArgument.getPlayers(p_248155_, "targets");
                              if (!collection.isEmpty()) {
                                 MessageArgument.resolveChatMessage(
                                    p_248155_, "message", p_248154_ -> sendMessage((CommandSourceStack)p_248155_.getSource(), collection, p_248154_)
                                 );
                              }

                              return collection.size();
                           }
                        )
                  )
            )
      );
      p_138061_.register((LiteralArgumentBuilder)Commands.literal("tell").redirect(literalcommandnode));
      p_138061_.register((LiteralArgumentBuilder)Commands.literal("w").redirect(literalcommandnode));
   }

   private static void sendMessage(CommandSourceStack p_250209_, Collection<ServerPlayer> p_252344_, PlayerChatMessage p_249416_) {
      ChatType.Bound chattype$bound = ChatType.bind(ChatType.MSG_COMMAND_INCOMING, p_250209_);
      OutgoingChatMessage outgoingchatmessage = OutgoingChatMessage.create(p_249416_);
      boolean flag = false;

      for (ServerPlayer serverplayer : p_252344_) {
         ChatType.Bound chattype$bound1 = ChatType.bind(ChatType.MSG_COMMAND_OUTGOING, p_250209_).withTargetName(serverplayer.getDisplayName());
         p_250209_.sendChatMessage(outgoingchatmessage, false, chattype$bound1);
         boolean flag1 = p_250209_.shouldFilterMessageTo(serverplayer);
         serverplayer.sendChatMessage(outgoingchatmessage, flag1, chattype$bound);
         flag |= flag1 && p_249416_.isFullyFiltered();
      }

      if (flag) {
         p_250209_.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
      }
   }
}
