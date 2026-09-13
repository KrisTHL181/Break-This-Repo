package net.minecraft.server.commands;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.IpBanList;

public class PardonIpCommand {
   private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(Component.translatable("commands.pardonip.invalid"));
   private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.pardonip.failed"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("pardon-ip").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               Commands.argument("target", StringArgumentType.word())
                  .suggests(
                     (c, p) -> SharedSuggestionProvider.suggest(((CommandSourceStack)c.getSource()).getServer().getPlayerList().getIpBans().getUserList(), p)
                  )
                  .executes(c -> unban((CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "target")))
            )
      );
   }

   private static int unban(final CommandSourceStack source, final String ip) throws CommandSyntaxException {
      if (!InetAddresses.isInetAddress(ip)) {
         throw ERROR_INVALID.create();
      }

      IpBanList bans = source.getServer().getPlayerList().getIpBans();
      if (!bans.isBanned(ip)) {
         throw ERROR_NOT_BANNED.create();
      }

      bans.remove(ip);
      source.sendSuccess(() -> Component.translatable("commands.pardonip.success", ip), true);
      return 1;
   }
}
