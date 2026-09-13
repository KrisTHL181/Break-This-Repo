package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.jspecify.annotations.Nullable;

public class BanPlayerCommands {
   private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.ban.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> p_136559_) {
      p_136559_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ban").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", GameProfileArgument.gameProfile())
                     .executes(
                        p_136569_ -> banPlayers((CommandSourceStack)p_136569_.getSource(), GameProfileArgument.getGameProfiles(p_136569_, "targets"), null)
                     ))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           p_136561_ -> banPlayers(
                              (CommandSourceStack)p_136561_.getSource(),
                              GameProfileArgument.getGameProfiles(p_136561_, "targets"),
                              MessageArgument.getMessage(p_136561_, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int banPlayers(CommandSourceStack p_136565_, Collection<NameAndId> p_136566_, @Nullable Component p_136567_) throws CommandSyntaxException {
      UserBanList userbanlist = p_136565_.getServer().getPlayerList().getBans();
      int i = 0;

      for (NameAndId nameandid : p_136566_) {
         if (!userbanlist.isBanned(nameandid)) {
            UserBanListEntry userbanlistentry = new UserBanListEntry(
               nameandid, null, p_136565_.getTextName(), null, p_136567_ == null ? null : p_136567_.getString()
            );
            userbanlist.add(userbanlistentry);
            i++;
            p_136565_.sendSuccess(
               () -> Component.translatable("commands.ban.success", Component.literal(nameandid.name()), userbanlistentry.getReasonMessage()), true
            );
            ServerPlayer serverplayer = p_136565_.getServer().getPlayerList().getPlayer(nameandid.id());
            if (serverplayer != null) {
               serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
            }
         }
      }

      if (i == 0) {
         throw ERROR_ALREADY_BANNED.create();
      } else {
         return i;
      }
   }
}
