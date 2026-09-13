package net.minecraft.server.commands;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import org.jspecify.annotations.Nullable;

public class BanIpCommands {
   private static final SimpleCommandExceptionType ERROR_INVALID_IP = new SimpleCommandExceptionType(Component.translatable("commands.banip.invalid"));
   private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.banip.failed"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ban-ip").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", StringArgumentType.word())
                     .executes(c -> banIpOrName((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "target"), null)))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           c -> banIpOrName(
                              (CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "target"), MessageArgument.getMessage(c, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int banIpOrName(final CommandSourceStack source, final String target, final @Nullable Component reason) throws CommandSyntaxException {
      if (InetAddresses.isInetAddress(target)) {
         return banIp(source, target, reason);
      } else {
         ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(target);
         if (player != null) {
            return banIp(source, player.getIpAddress(), reason);
         } else {
            throw ERROR_INVALID_IP.create();
         }
      }
   }

   private static int banIp(final CommandSourceStack source, final String ip, final @Nullable Component reason) throws CommandSyntaxException {
      IpBanList list = source.getServer().getPlayerList().getIpBans();
      if (list.isBanned(ip)) {
         throw ERROR_ALREADY_BANNED.create();
      }

      List<ServerPlayer> players = source.getServer().getPlayerList().getPlayersWithAddress(ip);
      IpBanListEntry entry = new IpBanListEntry(ip, null, source.getTextName(), null, reason == null ? null : reason.getString());
      list.add(entry);
      source.sendSuccess(() -> Component.translatable("commands.banip.success", ip, entry.getReasonMessage()), true);
      if (!players.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.banip.info", players.size(), EntitySelector.joinNames(players)), true);
      }

      for (ServerPlayer player : players) {
         player.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned"));
      }

      return players.size();
   }
}
