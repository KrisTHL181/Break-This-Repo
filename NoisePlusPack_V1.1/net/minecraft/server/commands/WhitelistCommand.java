package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.entity.player.Player;

public class WhitelistCommand {
   private static final SimpleCommandExceptionType ERROR_ALREADY_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.alreadyOn")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_DISABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.alreadyOff")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_WHITELISTED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.add.failed")
   );
   private static final SimpleCommandExceptionType ERROR_NOT_WHITELISTED = new SimpleCommandExceptionType(
      Component.translatable("commands.whitelist.remove.failed")
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_139202_) {
      p_139202_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "whitelist"
                              )
                              .requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                           .then(Commands.literal("on").executes(p_139236_ -> enableWhitelist((CommandSourceStack)p_139236_.getSource()))))
                        .then(Commands.literal("off").executes(p_139232_ -> disableWhitelist((CommandSourceStack)p_139232_.getSource()))))
                     .then(Commands.literal("list").executes(p_139228_ -> showList((CommandSourceStack)p_139228_.getSource()))))
                  .then(
                     Commands.literal("add")
                        .then(
                           Commands.argument("targets", GameProfileArgument.gameProfile())
                              .suggests(
                                 (p_421388_, p_421389_) -> {
                                    PlayerList playerlist = ((CommandSourceStack)p_421388_.getSource()).getServer().getPlayerList();
                                    return SharedSuggestionProvider.suggest(
                                       playerlist.getPlayers()
                                          .stream()
                                          .map(Player::nameAndId)
                                          .filter(p_421387_ -> !playerlist.getWhiteList().isWhiteListed(p_421387_))
                                          .map(NameAndId::name),
                                       p_421389_
                                    );
                                 }
                              )
                              .executes(
                                 p_139224_ -> addPlayers((CommandSourceStack)p_139224_.getSource(), GameProfileArgument.getGameProfiles(p_139224_, "targets"))
                              )
                        )
                  ))
               .then(
                  Commands.literal("remove")
                     .then(
                        Commands.argument("targets", GameProfileArgument.gameProfile())
                           .suggests(
                              (p_139206_, p_139207_) -> SharedSuggestionProvider.suggest(
                                 ((CommandSourceStack)p_139206_.getSource()).getServer().getPlayerList().getWhiteListNames(), p_139207_
                              )
                           )
                           .executes(
                              p_139214_ -> removePlayers((CommandSourceStack)p_139214_.getSource(), GameProfileArgument.getGameProfiles(p_139214_, "targets"))
                           )
                     )
               ))
            .then(Commands.literal("reload").executes(p_139204_ -> reload((CommandSourceStack)p_139204_.getSource())))
      );
   }

   private static int reload(CommandSourceStack p_139209_) {
      p_139209_.getServer().getPlayerList().reloadWhiteList();
      p_139209_.sendSuccess(() -> Component.translatable("commands.whitelist.reloaded"), true);
      p_139209_.getServer().kickUnlistedPlayers();
      return 1;
   }

   private static int addPlayers(CommandSourceStack p_139211_, Collection<NameAndId> p_139212_) throws CommandSyntaxException {
      UserWhiteList userwhitelist = p_139211_.getServer().getPlayerList().getWhiteList();
      int i = 0;

      for (NameAndId nameandid : p_139212_) {
         if (!userwhitelist.isWhiteListed(nameandid)) {
            UserWhiteListEntry userwhitelistentry = new UserWhiteListEntry(nameandid);
            userwhitelist.add(userwhitelistentry);
            p_139211_.sendSuccess(() -> Component.translatable("commands.whitelist.add.success", Component.literal(nameandid.name())), true);
            i++;
         }
      }

      if (i == 0) {
         throw ERROR_ALREADY_WHITELISTED.create();
      } else {
         return i;
      }
   }

   private static int removePlayers(CommandSourceStack p_139221_, Collection<NameAndId> p_139222_) throws CommandSyntaxException {
      UserWhiteList userwhitelist = p_139221_.getServer().getPlayerList().getWhiteList();
      int i = 0;

      for (NameAndId nameandid : p_139222_) {
         if (userwhitelist.isWhiteListed(nameandid)) {
            UserWhiteListEntry userwhitelistentry = new UserWhiteListEntry(nameandid);
            userwhitelist.remove(userwhitelistentry);
            p_139221_.sendSuccess(() -> Component.translatable("commands.whitelist.remove.success", Component.literal(nameandid.name())), true);
            i++;
         }
      }

      if (i == 0) {
         throw ERROR_NOT_WHITELISTED.create();
      }

      p_139221_.getServer().kickUnlistedPlayers();
      return i;
   }

   private static int enableWhitelist(CommandSourceStack p_139219_) throws CommandSyntaxException {
      if (p_139219_.getServer().isUsingWhitelist()) {
         throw ERROR_ALREADY_ENABLED.create();
      }

      p_139219_.getServer().setUsingWhitelist(true);
      p_139219_.sendSuccess(() -> Component.translatable("commands.whitelist.enabled"), true);
      p_139219_.getServer().kickUnlistedPlayers();
      return 1;
   }

   private static int disableWhitelist(CommandSourceStack p_139226_) throws CommandSyntaxException {
      if (!p_139226_.getServer().isUsingWhitelist()) {
         throw ERROR_ALREADY_DISABLED.create();
      }

      p_139226_.getServer().setUsingWhitelist(false);
      p_139226_.sendSuccess(() -> Component.translatable("commands.whitelist.disabled"), true);
      return 1;
   }

   private static int showList(CommandSourceStack p_139230_) {
      String[] astring = p_139230_.getServer().getPlayerList().getWhiteListNames();
      if (astring.length == 0) {
         p_139230_.sendSuccess(() -> Component.translatable("commands.whitelist.none"), false);
      } else {
         p_139230_.sendSuccess(() -> Component.translatable("commands.whitelist.list", astring.length, String.join(", ", astring)), false);
      }

      return astring.length;
   }
}
