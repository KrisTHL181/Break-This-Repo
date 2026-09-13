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

   public static void register(CommandDispatcher<CommandSourceStack> p_136528_) {
      p_136528_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ban-ip").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", StringArgumentType.word())
                     .executes(p_136538_ -> banIpOrName((CommandSourceStack)p_136538_.getSource(), StringArgumentType.getString(p_136538_, "target"), null)))
                  .then(
                     Commands.argument("reason", MessageArgument.message())
                        .executes(
                           p_136530_ -> banIpOrName(
                              (CommandSourceStack)p_136530_.getSource(),
                              StringArgumentType.getString(p_136530_, "target"),
                              MessageArgument.getMessage(p_136530_, "reason")
                           )
                        )
                  )
            )
      );
   }

   private static int banIpOrName(CommandSourceStack p_136534_, String p_136535_, @Nullable Component p_136536_) throws CommandSyntaxException {
      if (InetAddresses.isInetAddress(p_136535_)) {
         return banIp(p_136534_, p_136535_, p_136536_);
      } else {
         ServerPlayer serverplayer = p_136534_.getServer().getPlayerList().getPlayerByName(p_136535_);
         if (serverplayer != null) {
            return banIp(p_136534_, serverplayer.getIpAddress(), p_136536_);
         } else {
            throw ERROR_INVALID_IP.create();
         }
      }
   }

   private static int banIp(CommandSourceStack p_136540_, String p_136541_, @Nullable Component p_136542_) throws CommandSyntaxException {
      IpBanList ipbanlist = p_136540_.getServer().getPlayerList().getIpBans();
      if (ipbanlist.isBanned(p_136541_)) {
         throw ERROR_ALREADY_BANNED.create();
      }

      List<ServerPlayer> list = p_136540_.getServer().getPlayerList().getPlayersWithAddress(p_136541_);
      IpBanListEntry ipbanlistentry = new IpBanListEntry(p_136541_, null, p_136540_.getTextName(), null, p_136542_ == null ? null : p_136542_.getString());
      ipbanlist.add(ipbanlistentry);
      p_136540_.sendSuccess(() -> Component.translatable("commands.banip.success", p_136541_, ipbanlistentry.getReasonMessage()), true);
      if (!list.isEmpty()) {
         p_136540_.sendSuccess(() -> Component.translatable("commands.banip.info", list.size(), EntitySelector.joinNames(list)), true);
      }

      for (ServerPlayer serverplayer : list) {
         serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned"));
      }

      return list.size();
   }
}
